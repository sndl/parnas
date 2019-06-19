package sndl.parnas.backend.impl

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.*
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.*

class SSM(name: String, private val ssmClient: AWSSimpleSystemsManagement,
          var prefix: String, private val keyId: String, private val separatorToReplace: String? = null) : Backend(name) {

    constructor(name: String, region: String?, profileName: String?,
                prefix: String, keyId: String, separatorToReplace: String? = null) :
            this(
                    name = name,
                    ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
                            .withRegion(region)
                            .withCredentials(ProfileCredentialsProvider(profileName))
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
            val result = ssmClient.getParametersByPath(request)
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
            ConfigOption(key, ssmClient.getParameter(request).parameter.value)
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

        ssmClient.putParameter(request)

        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        val fullKey = prefix + key.convertToSSMFormat()
        val request = DeleteParameterRequest()
                .withName(fullKey)

        try {
            ssmClient.deleteParameter(request)
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
