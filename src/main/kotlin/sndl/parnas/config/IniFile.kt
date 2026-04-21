package sndl.parnas.config

import sndl.parnas.utils.ConfigurationException
import java.io.File

class IniSection : LinkedHashMap<String, String>() {
    fun add(key: String, value: String) {
        put(key, value)
    }
}

class IniFile : LinkedHashMap<String, IniSection>() {
    companion object {
        fun parse(file: File): IniFile {
            val result = IniFile()
            var currentSection: IniSection? = null

            file.forEachLine(Charsets.UTF_8) { rawLine ->
                val line = rawLine.trim()
                when {
                    line.isEmpty() || line.startsWith(";") || line.startsWith("#") -> {}
                    line.startsWith("[") -> {
                        val closeIdx = line.indexOf(']')
                        if (closeIdx < 0) throw ConfigurationException("malformed section header: $line")
                        val name = line.substring(1, closeIdx).trim()
                        currentSection = IniSection().also { result[name] = it }
                    }
                    line.contains('=') -> {
                        if (currentSection == null)
                            throw ConfigurationException("key-value pair found outside of a section: $line")
                        val idx = line.indexOf('=')
                        val key = line.substring(0, idx).trim()
                        val value = line.substring(idx + 1).trim()
                        currentSection?.put(key, value)
                    }
                }
            }

            return result
        }
    }
}
