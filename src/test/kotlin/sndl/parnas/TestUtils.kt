package sndl.parnas

import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmClient
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

object TestUtils {
    fun buildClient(localstack: LocalStackContainer): SsmClient = SsmClient.builder()
        .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SSM))
        .region(Region.EU_WEST_1)
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
        .build()

    private val tmpPath = Path(System.getProperty("java.io.tmpdir"))
    fun tmpFilePath(name: String): String {
        val tmpFile = tmpPath.resolve(Path(name))
        tmpFile.parent?.createDirectories()
        return tmpFile.absolutePathString()
    }
}