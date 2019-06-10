package sndl.parnas.utils

import kotlin.system.exitProcess

fun Any?.toStringOrEmpty() = this?.toString() ?: ""

fun String.quoted() = "\"$this\""

fun <T> buildSet(body: LinkedHashSet<T>.() -> Unit): LinkedHashSet<T> {
    val set = LinkedHashSet<T>()
    set.body()
    return set
}

/**
 * Method used to retrieve parameter values from config file and provides optional fallback to:
 * *  Environment parameter, which name is constructed this way: PARNAS_${BACKEND_NAME}_${PARAMETER_NAME}
 * *  Console prompt
 */
fun getConfigParameter(parameterName: String, backendConfig: Map<String, String>, fallback: Boolean = false): String {
    return if (fallback) {
        backendConfig[parameterName]
                ?: System.getenv("PARNAS_${backendConfig.getValue("name").toUpperCase()}_${parameterName.toUpperCase()}")
                ?: System.console()
                        .readPassword("Please enter value for backend \"${backendConfig["name"]}\" parameter \"$parameterName\": ")
                        .joinToString("")
                ?: throw ParameterRequiredException(parameterName, backendConfig.getValue("type"))
    } else {
        backendConfig[parameterName]
                ?: throw ParameterRequiredException(parameterName, backendConfig.getValue("type"))
    }
}

fun exitProcessWithMessage(status: Int, message: String): Nothing {
    System.err.println(message)
    exitProcess(status)
}

fun <T> Collection<T>.toLinkedSet() = LinkedHashSet(this)
