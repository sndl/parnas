package sndl.parnas.storage.impl.keepass

import de.slackspace.openkeepass.exception.KeePassDatabaseUnreadableException
import org.slf4j.LoggerFactory
import sndl.parnas.storage.Storage
import sndl.parnas.storage.ConfigOption
import sndl.parnas.utils.*
import java.io.File

class KeePass(name: String, path: String, password: String) : Storage(name) {
    constructor(name: String, config: Map<String, String>) : this(
            name = name,
            path = getConfigParameter("path", config),
            password = getConfigParameter("password", config, true)
    )

    companion object {
        private val logger = LoggerFactory.getLogger(KeepassClient::class.java)
    }

    private val file = File(path)
    private val data = try {
        KeepassClient(file, password)
    } catch (e: KeePassDatabaseUnreadableException) {
        logger.debug(e.message, e)
        throw WrongSecret("An incorrect password for KeePass storage ($name)")
    }

    override val isInitialized: Boolean
        get() = file.exists()

    override fun initialize() {
        data.createDb()
    }

    override fun list() = data.list().orEmpty().map { ConfigOption(it.title, it.password) }.toLinkedSet()

    override fun get(key: String) = data.findByTitle(key)?.let { ConfigOption(key, it.password) }

    override fun set(key: String, value: String): ConfigOption {
        data.setEntry(key, value)
        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        data.deleteEntry(key)
    }
}
