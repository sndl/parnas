package sndl.parnas

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

object TestContainersFactory {
    // See: https://github.com/testcontainers/testcontainers-java/issues/318
    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

    private const val LOCALSTACK_IMAGE = "localstack/localstack:3.8"

    fun getLocalstack(): LocalStackContainer = LocalStackContainer(DockerImageName.parse(LOCALSTACK_IMAGE))
        .withServices(LocalStackContainer.Service.SSM)
        .waitingFor(Wait.forHttp("/_localstack/health").forPort(4566).forStatusCode(200))
        .also { it.start() }
}
