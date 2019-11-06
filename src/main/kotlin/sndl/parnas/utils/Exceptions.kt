package sndl.parnas.utils

open class ConfigurationException(message: String) : Exception(message)

class ParameterRequiredException(name: String, storageType: String)
    : ConfigurationException("\"$name\" is required for storage type \"$storageType\"")

class CannotInitializeStorage(message: String) : ConfigurationException(message)

class StorageIsNotInitialized(message: String) : ConfigurationException(message)

class WrongSecret(message: String) : ConfigurationException(message)
