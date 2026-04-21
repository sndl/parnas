package sndl.parnas.config

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

            file.forEachLine { rawLine ->
                val line = rawLine.trim()
                when {
                    line.isEmpty() || line.startsWith(";") || line.startsWith("#") -> {}
                    line.startsWith("[") && line.endsWith("]") -> {
                        val name = line.substring(1, line.length - 1).trim()
                        currentSection = IniSection().also { result[name] = it }
                    }
                    line.contains('=') -> {
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
