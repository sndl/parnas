package sndl.parnas.config

import org.ini4j.Ini
import sndl.parnas.backend.Backend
import sndl.parnas.backend.impl.*
import sndl.parnas.backend.impl.keepass.KeePass
import sndl.parnas.utils.ConfigurationException
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.io.FileReader

class Config(configFile: File) {
    private val ini = Ini(FileReader(configFile))

    companion object {
        private val backends = HashMap<String, Backend>()
    }

    fun getBackend(name: String) = backends.getOrPut(name) { initBackend(name) }

    fun getBackendsByTag(tag: String) = ini.filter {
        tag in it.value["tags"]?.split(",")?.map { it.trim() } ?: emptyList()
    }.map { getBackend(it.key) }.toLinkedSet()

    private fun initBackend(name: String): Backend {
        val backendConfig = ini[name]?.apply { add("name", name) }
                ?: throw ConfigurationException("configuration for the backend named \"$name\" is absent")

        return when (backendConfig["type"]) {
            "plain" -> Plain(name, backendConfig)
            "ssm" -> SSM(name, backendConfig)
            "keepass" -> KeePass(name, backendConfig)
            "toml" -> Toml(name, backendConfig)
            else -> throw ConfigurationException("specified backend type is not supported: ${backendConfig["type"]}")
        }
    }
}
