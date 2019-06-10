package sndl.parnas.backend.impl

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.*
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.*

class SSM(name: String, private val ssmClient: AWSSimpleSystemsManagement,
          private var prefix: String, private val keyId: String) : Backend(name) {

    constructor(name: String, region: String?, profileName: String?,
                prefix: String, keyId: String) :
            this(
                    name = name,
                    ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
                            .withRegion(region)
                            .withCredentials(ProfileCredentialsProvider(profileName))
                            .build(),
                    prefix = prefix,
                    keyId = keyId
            )

    constructor(name: String, config: Map<String, String>) :
            this(
                    name = name,
                    region = getConfigParameter("region", config),
                    profileName = getConfigParameter("profile", config),
                    prefix = getConfigParameter("prefix", config),
                    keyId = getConfigParameter("kms-key-id", config, true)
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
                .withWithDecryption(true)

        do {
            val result = ssmClient.getParametersByPath(request)
            val nextToken = result.nextToken

            result.parameters.forEach {
                add(ConfigOption(it.name.removePrefix(prefix), it.value))
            }

            request.nextToken = nextToken
        } while (nextToken != null)
    }

    override fun get(key: String): ConfigOption? {
        val fullKey = prefix + key
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
        val fullKey = prefix + key
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
        val fullKey = prefix + key
        val request = DeleteParameterRequest()
                .withName(fullKey)

        try {
            ssmClient.deleteParameter(request)
        } catch (e: ParameterNotFoundException) {
            System.err.println("ERROR: Parameter Not Found - \"$key\"")
        }
    }
}
