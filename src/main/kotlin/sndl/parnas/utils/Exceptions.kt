package sndl.parnas.utils

open class ConfigurationException(message: String) : Exception(message)

class ParameterRequiredException(name: String, backendType: String)
    : ConfigurationException("\"$name\" is required for backend type \"$backendType\"")

class CannotInitializeBackend(message: String) : ConfigurationException(message)

class BackendIsNotInitialized(message: String) : ConfigurationException(message)

class WrongSecret(message: String) : ConfigurationException(message)
