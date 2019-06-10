package sndl.parnas.backend.impl

import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.*
import java.io.File
import java.util.*

class Plain(name: String, private val path: String) : Backend(name) {
    constructor(name: String, config: Map<String, String>) :
            this(name = name, path = getConfigParameter("path", config))

    private val file = File(path)

    private lateinit var data: Properties

    override val isInitialized: Boolean = file.exists()

    init {
        if (file.exists()) {
            data = Properties().apply { load(file.reader()) }
        }
    }

    override fun initialize() {
        if (isInitialized) {
            throw CannotInitializeBackend("Backend is already initialized")
        }

        file.createNewFile()
        data = Properties().apply { load(file.reader()) }
    }

    override fun list() = data.map { ConfigOption(it.key.toString(), it.value.toString()) }.toLinkedSet()

    override fun get(key: String): ConfigOption? = data.getProperty(key)?.let { ConfigOption(key, it) }

    override fun set(key: String, value: String): ConfigOption {
        data.setProperty(key, value)
        data.store(file.outputStream(), null)

        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        data.remove(key)
        data.store(file.outputStream(), null)
    }
}
