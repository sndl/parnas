package sndl.parnas.config

import org.ini4j.Ini
import sndl.parnas.storage.Storage
import sndl.parnas.storage.impl.*
import sndl.parnas.storage.impl.keepass.KeePass
import sndl.parnas.utils.ConfigurationException
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.io.FileReader

class Config(configFile: File) {
    private val ini = Ini(FileReader(configFile))

    companion object {
        private val storages = HashMap<String, Storage>()
    }

    fun getStorage(name: String) = storages.getOrPut(name) { initStorage(name) }

    fun getStoragesByTag(tag: String) = ini.filter {
        tag in it.value["tags"]?.split(",")?.map { it.trim() } ?: emptyList()
    }.map { getStorage(it.key) }.toLinkedSet()

    private fun initStorage(name: String): Storage {
        val storageConfig = ini[name]?.apply { add("name", name) }
                ?: throw ConfigurationException("configuration for storage \"$name\" is missing")

        return when (storageConfig["type"]) {
            "plain" -> Plain(name, storageConfig)
            "ssm" -> SSM(name, storageConfig)
            "keepass" -> KeePass(name, storageConfig)
            "toml" -> Toml(name, storageConfig)
            else -> throw ConfigurationException("specified storage type is not supported: ${storageConfig["type"]}")
        }
    }
}
