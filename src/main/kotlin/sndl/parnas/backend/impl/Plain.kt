package sndl.parnas.backend.impl

import nu.studer.java.util.OrderedProperties
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.*
import java.io.File

class Plain(name: String, private val path: String) : Backend(name) {
    constructor(name: String, config: Map<String, String>) :
            this(name = name, path = getConfigParameter("path", config))

    private val file = File(path)

    private lateinit var data: OrderedProperties

    override val isInitialized: Boolean = file.exists()

    init {
        if (file.exists()) {
            data = OrderedProperties().apply { load(file.reader()) }
        }
    }

    override fun initialize() {
        if (isInitialized) {
            throw CannotInitializeBackend("Backend is already initialized")
        }

        file.createNewFile()
        data = OrderedProperties().apply { load(file.reader()) }
    }

    override fun list() = data.entrySet().map { ConfigOption(it.key.toString(), it.value.toString()) }.toLinkedSet()

    override fun get(key: String): ConfigOption? = data.getProperty(key)?.let { ConfigOption(key, it) }

    override fun set(key: String, value: String): ConfigOption {
        data.setProperty(key, value)
        data.store(file.outputStream(), null)

        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        data.removeProperty(key)
        data.store(file.outputStream(), null)
    }
}
