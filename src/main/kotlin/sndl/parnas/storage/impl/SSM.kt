package sndl.parnas.storage.impl

import sndl.parnas.storage.Storage
import sndl.parnas.storage.ConfigOption
import sndl.parnas.utils.*
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException
import software.amazon.awssdk.services.ssm.model.PutParameterRequest
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.system.measureTimeMillis

class SSM(name: String, ssmClient: SsmClient,
          var prefix: String, private val kmsKeyId: String?, private val separatorToReplace: String? = null) : Storage(name) {

    constructor(name: String, region: String?, profileName: String?,
                prefix: String, keyId: String?, separatorToReplace: String? = null) :
            this(
                    name = name,
                    ssmClient = SsmClient.builder()
                            .region(region?.let { Region.of(it)} ?: DefaultAwsRegionProviderChain().region)
                            .credentialsProvider(
                                AwsCredentialsProviderChain.of(
                                    EnvironmentVariableCredentialsProvider.create(),
                                    profileName?.let { ProfileCredentialsProvider.create(it) },
                                    InstanceProfileCredentialsProvider.create()
                                )
                            ).build(),
                    prefix = prefix,
                    kmsKeyId = keyId,
                    separatorToReplace = separatorToReplace
            )

    // TODO: implement parameter nullability properly
    constructor(name: String, config: Map<String, String>) :
            this(
                    name = name,
                    region = try { getConfigParameter("region", config) } catch (e: ParameterRequiredException) { null },
                    profileName = try { getConfigParameter("profile", config) } catch (e: ParameterRequiredException) { null },
                    prefix = getConfigParameter("prefix", config),
                    keyId = try { getConfigParameter("kms-key-id", config, true) } catch (e: ParameterRequiredException) { null },
                    separatorToReplace = config["separator-to-replace"]
            )

    /**
     * Invocation handler and proxy class for SSM client in order to induce politeness delay when there are more than 40 rps to SSM API
     * See: https://forums.aws.amazon.com/thread.jspa?threadID=275184
     */
    private class SSMHandler(private val target: Any) : InvocationHandler {
        private var counter = 0
        private var executionTime = 0L
        private val callsPerSecondThreshold = 40

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
            lateinit var result: Any

            if (counter >= callsPerSecondThreshold && executionTime <= 1000) {
                counter = 0
                executionTime = 0
                Thread.sleep(1000)
            } else if (executionTime >= 1000) {
                counter = 0
                executionTime = 0
            }

            try {
                executionTime += measureTimeMillis {
                    result = method.invoke(target, *args)
                    counter++
                }
            } catch (e: InvocationTargetException) {
                throw(e.cause!!)
            }

            return result
        }
    }

    private val ssmClientProxy = Proxy.newProxyInstance(
        SsmClient::class.java.classLoader,
        arrayOf(SsmClient::class.java),
        SSMHandler(ssmClient)
    ) as SsmClient

    override val isInitialized: Boolean = true

    init {
        if (!prefix.endsWith('/')) prefix += '/'
    }

    /**
     * Does nothing in context of SSM, because it cannot be initialized
     */
    override fun initialize() {
        throw CannotInitializeStorage("SSM storage does not require initialization")
    }

    override fun list() = buildSet<ConfigOption> {
        var request = GetParametersByPathRequest.builder()
                .path(prefix)
                .recursive(true)
                .withDecryption(true)
                .build()

        do {
            val result = ssmClientProxy.getParametersByPath(request)
            val nextToken = result.nextToken()

            result.parameters().forEach {
                add(ConfigOption(it.name().removePrefix(prefix).convertFromSSMFormat(), it.value()))
            }

            request = request.copy { it.nextToken(nextToken) }
        } while (nextToken != null)
    }

    override fun get(key: String): ConfigOption? {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = GetParameterRequest.builder()
                .name(fullKey)
                .withDecryption(true)
                .build()

        return try {
            ConfigOption(key, ssmClientProxy.getParameter(request).parameter().value())
        } catch (e: ParameterNotFoundException) {
            null
        }
    }

    override fun set(key: String, value: String): ConfigOption {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = PutParameterRequest.builder()
                .name(fullKey)
                .value(value)
                .overwrite(true)
                .apply {
                    // TODO@sndl: support StringList type
                    kmsKeyId?.let {
                        type("SecureString")
                        keyId(it)
                    } ?: type("String")
                }.build()

        ssmClientProxy.putParameter(request)

        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = DeleteParameterRequest.builder()
                .name(fullKey)
                .build()

        try {
            ssmClientProxy.deleteParameter(request)
        } catch (e: ParameterNotFoundException) {
            System.err.println("ERROR: Parameter Not Found - \"$key\"")
        }
    }

    private fun String.convertToSSMFormat() = separatorToReplace?.let {
        this.replace(separatorToReplace, "/")
    } ?: this

    private fun String.convertFromSSMFormat() = separatorToReplace?.let {
        this.replace("/", separatorToReplace)
    } ?: this
}
