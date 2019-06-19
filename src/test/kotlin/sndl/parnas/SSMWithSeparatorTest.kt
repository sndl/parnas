package sndl.parnas

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import org.junit.ClassRule
import org.junit.jupiter.api.*
import sndl.parnas.backend.ConfigOption
import sndl.parnas.backend.impl.SSM
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SSMWithSeparatorTest {
    companion object {
        private const val containerPort = 4583
        private const val awsRegion = "eu-west-1"
    }

    @ClassRule
    private val localstack = KGenericContainer("localstack/localstack:latest")
            .withExposedPorts(containerPort)
            .withEnv("SERVICES", "ssm").also { it.start() }

    private val ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder
                    .EndpointConfiguration(
                            "http://${localstack.containerIpAddress}:${localstack.getMappedPort(containerPort)}",
                            awsRegion))
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("dummy", "dummy")))
            .build()
    private val backend
        get() = SSM("ssm-test", ssmClient, "/${randomUUID()}/", "1111", ".").also {
            it["FIRST_ENTRY"] = "first-entry"
            it["SECOND_ENTRY"] = "second-entry"
            it["ENTRY_PREFIX.THIRD_ENTRY"] = "third-entry"
            it["ENTRY_PREFIX.ENTRY_2ND_PREFIX.FOURTH_ENTRY"] = "fourth-entry"
        }

    @Test
    fun list_backendIsNotEmpty_gotNotEmptyList() {
        Assertions.assertTrue(backend.list().size > 0)
    }

    @Test
    fun get_entryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("ENTRY_PREFIX.THIRD_ENTRY", "third-entry"), backend["ENTRY_PREFIX.THIRD_ENTRY"])
    }

    @Test
    fun list_backendIsNotEmpty_gotExpectedListOfValues() {
        val list = backend.list()
        val expectedList = setOf(
                ConfigOption("FIRST_ENTRY", "first-entry"),
                ConfigOption("SECOND_ENTRY", "second-entry"),
                ConfigOption("ENTRY_PREFIX.THIRD_ENTRY", "third-entry"),
                ConfigOption("ENTRY_PREFIX.ENTRY_2ND_PREFIX.FOURTH_ENTRY", "fourth-entry")
        )

        println(list)

        Assertions.assertEquals(expectedList, list)
    }

    @Test
    fun get_entryDoesNotExist_gotNull() {
        Assertions.assertNull(backend["ENTRY_PREFIX.FOURTH_ENTRY"])
    }

    @Test
    fun set_entryDoesNotExist_entryExists() {
        val testBackend = backend

        testBackend["THIRD_ENTRY"] = "third-entry"

        val expectedEntry = ConfigOption("THIRD_ENTRY", "third-entry")
        val entry = testBackend["THIRD_ENTRY"]

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun set_entryDoesNotExist_entryExistsInCorrectFormInSSM() {
        val testBackend = backend

        testBackend["ENTRY_PREFIX.FIFTH_ENTRY"] = "fifth-entry"

        val expectedEntry = ConfigOption("ENTRY_PREFIX.FIFTH_ENTRY", "fifth-entry")
        val entry = ConfigOption("ENTRY_PREFIX.FIFTH_ENTRY",
                ssmClient.getParameter(GetParameterRequest()
                        .withName("${testBackend.prefix}ENTRY_PREFIX/FIFTH_ENTRY")
                        .withWithDecryption(true)
                ).parameter.value)

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun set_entryDoesNotExist_exactlyOneEntryIsCreated() {
        val testBackend = backend
        val beforeSize = testBackend.list().size

        testBackend["THIRD_ENTRY"] = "third-entry"

        val afterSize = testBackend.list().size

        Assertions.assertTrue(afterSize - beforeSize == 1)
    }

    @Test
    fun set_entryExists_entryUpdated() {
        val testBackend = backend

        testBackend["FIRST_ENTRY"] = "updated-first-entry"

        val expectedEntry = ConfigOption("FIRST_ENTRY", "updated-first-entry")
        val entry = testBackend["FIRST_ENTRY"]

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun delete_entryExists_entryDoesNotExist() {
        val testBackend = backend
        testBackend.delete("ENTRY_PREFIX.THIRD_ENTRY")
        Assertions.assertNull(testBackend["ENTRY_PREFIX.THIRD_ENTRY"])
    }

    @Test
    fun delete_entryExists_exactlyOneEntryIsRemoved() {
        val testBackend = backend
        val sizeBefore = testBackend.list().size

        testBackend.delete("ENTRY_PREFIX.THIRD_ENTRY")

        val sizeAfter = testBackend.list().size

        Assertions.assertTrue(sizeBefore - sizeAfter == 1)
    }
}
