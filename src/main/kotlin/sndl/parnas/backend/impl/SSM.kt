package sndl.parnas.backend.impl

import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.*
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.system.measureTimeMillis

class SSM(name: String, ssmClient: AWSSimpleSystemsManagement,
          var prefix: String, private val keyId: String, private val separatorToReplace: String? = null) : Backend(name) {

    constructor(name: String, region: String?, profileName: String?,
                prefix: String, keyId: String, separatorToReplace: String? = null) :
            this(
                    name = name,
                    ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
                            .withRegion(region)
                            .withCredentials(AWSCredentialsProviderChain(
                                    EnvironmentVariableCredentialsProvider(),
                                    ProfileCredentialsProvider(profileName)
                            ))
                            .build(),
                    prefix = prefix,
                    keyId = keyId,
                    separatorToReplace = separatorToReplace
            )

    constructor(name: String, config: Map<String, String>) :
            this(
                    name = name,
                    region = getConfigParameter("region", config),
                    profileName = getConfigParameter("profile", config),
                    prefix = getConfigParameter("prefix", config),
                    keyId = getConfigParameter("kms-key-id", config, true),
                    separatorToReplace = config["separator-to-replace"]
            )

    /**
     * Invocation handler and proxy class for SSM client in order to induce politeness delay when there are more than 40 rps to SSM API
     * See: https://forums.aws.amazon.com/thread.jspa?threadID=275184
     */
    private class SSMHandler(private val target: Any) : InvocationHandler {
        private var counter = 0
        private var executionTime = 0L

        override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
            lateinit var result: Any

            if (counter >= 40 && executionTime <= 1000) {
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
            AWSSimpleSystemsManagement::class.java.classLoader,
            arrayOf(AWSSimpleSystemsManagement::class.java),
            SSMHandler(ssmClient)
    ) as AWSSimpleSystemsManagement

    override val isInitialized: Boolean = true

    init {
        if (!prefix.endsWith('/')) prefix += '/'
    }

    /**
     * Does nothing in context of SSM, because it cannot be initialized
     */
    override fun initialize() {
        throw CannotInitializeBackend("There is no need to initialize backend of SSM type")
    }

    override fun list() = buildSet<ConfigOption> {
        val request = GetParametersByPathRequest()
                .withPath(prefix)
                .withRecursive(true)
                .withWithDecryption(true)

        do {
            val result = ssmClientProxy.getParametersByPath(request)
            val nextToken = result.nextToken

            result.parameters.forEach {
                add(ConfigOption(it.name.removePrefix(prefix).convertFromSSMFormat(), it.value))
            }

            request.nextToken = nextToken
        } while (nextToken != null)
    }

    override fun get(key: String): ConfigOption? {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = GetParameterRequest()
                .withName(fullKey)
                .withWithDecryption(true)

        return try {
            ConfigOption(key, ssmClientProxy.getParameter(request).parameter.value)
        } catch (e: ParameterNotFoundException) {
            null
        }
    }

    override fun set(key: String, value: String): ConfigOption {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = PutParameterRequest()
                .withName(fullKey)
                .withValue(value)
                .withType("SecureString")
                .withKeyId(keyId)
                .withOverwrite(true)

        ssmClientProxy.putParameter(request)

        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = DeleteParameterRequest()
                .withName(fullKey)

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
