package sndl.parnas

import org.testcontainers.containers.GenericContainer

// See: https://github.com/testcontainers/testcontainers-java/issues/318
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
