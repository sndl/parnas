package sndl.parnas

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy

object TestContainersFactory {
    // See: https://github.com/testcontainers/testcontainers-java/issues/318
    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

    private const val localstackVersion = "0.12.1"

    fun getLocalstack(services: String = "ssm"): KGenericContainer = KGenericContainer("localstack/localstack:$localstackVersion")
            .withExposedPorts(4566)
            .waitingFor(HttpWaitStrategy()
                    .forPath("/health")
                    .forStatusCode(200)
                    .forResponsePredicate { it != "{}" })
            .withEnv("SERVICES", services).also { it.start() }
}
