package sndl.parnas.utils

import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

fun Any?.toStringOrEmpty() = this?.toString() ?: ""

fun String.quoted() = "\"$this\""

fun <T> buildSet(body: LinkedHashSet<T>.() -> Unit): LinkedHashSet<T> {
    val set = LinkedHashSet<T>()
    set.body()
    return set
}

/**
 * Method used to retrieve parameter values from config file and provides an optional fallback to:
 * *  Environment parameter, which name is constructed this way: PARNAS_${STORAGE_NAME}_${PARAMETER_NAME}
 * *  Contents from a file, which name should be like: parnas_${storage_name}_{parameter_name}
 * *  Console prompt
 */
fun getConfigParameter(parameterName: String, storageConfig: Map<String, String>, fallback: Boolean = false): String {
    return if (fallback) {
        storageConfig[parameterName]
                ?: System.getenv("PARNAS_${storageConfig.getValue("name").toUpperCase()}_${parameterName.toUpperCase()}")
                ?: storageConfig["$parameterName-from-file"]?.let { getFileContentOrNull(it) }
                ?: getFileContentOrNull(".parnas_${storageConfig.getValue("name")}_$parameterName")
                ?: System.console()
                        .readPassword("Please enter value for storage \"${storageConfig["name"]}\" parameter \"$parameterName\": ")
                        .joinToString("")
                ?: throw ParameterRequiredException(parameterName, storageConfig.getValue("type"))
    } else {
        storageConfig[parameterName]
                ?: throw ParameterRequiredException(parameterName, storageConfig.getValue("type"))
    }
}

fun exitProcessWithMessage(status: Int, message: String): Nothing {
    System.err.println(message)
    exitProcess(status)
}

fun getFileContentOrNull(path: String): String? = try { File(path).readText().trim() } catch (e: FileNotFoundException) { null }

fun <T> Collection<T>.toLinkedSet() = LinkedHashSet(this)
