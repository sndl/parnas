package sndl.parnas.backend.impl

import com.electronwill.nightconfig.core.AbstractConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.core.file.FileConfig
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.*
import java.io.File

class Toml(name: String, private val path: String) : Backend(name) {
    constructor(name: String, config: Map<String, String>) :
            this(name = name, path = getConfigParameter("path", config))

    private val file = File(path)
    private lateinit var config: FileConfig
    override val isInitialized: Boolean
        get() = File(path).exists()


    init {
        if (file.exists()) {
            config = FileConfig.of(file).apply { load() }
        }
    }

    override fun list() = nestedList(HashSet(config.entrySet()))

    private fun nestedList(entrySet: Set<Config.Entry>, prefix: String? = null): LinkedHashSet<ConfigOption> = buildSet {
        entrySet.forEach { entry ->
            val key = prefix?.let { "$prefix.${entry.key}" } ?: entry.key
            val value = entry.getValue<Any>()

            if (value is AbstractConfig) {
                // TOML file is not expected to be deeply nested, therefore recursion should do just fine
                addAll(nestedList(HashSet(value.entrySet()), key))
            } else {
                add(ConfigOption(key, value.toString()))
            }
        }
    }

    override fun get(key: String) = config.get<Any>(key)?.let {
        ConfigOption(key, it.toString())
    }

    override fun set(key: String, value: String): ConfigOption {
        config.set<Any>(key, value)
        config.save()

        return ConfigOption(key, value)
    }

    override fun delete(key: String) {
        config.remove<Any>(key)
        config.save()
    }

    override fun initialize() {
        if (isInitialized) {
            throw CannotInitializeBackend("Backend is already initialized")
        }

        file.createNewFile()
        config = FileConfig.of(file).also { it.load() }
    }
}
