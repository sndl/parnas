package sndl.parnas

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import org.junit.ClassRule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.backend.impl.Plain
import sndl.parnas.backend.impl.SSM
import sndl.parnas.backend.impl.Toml
import sndl.parnas.backend.impl.keepass.KeePass
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.util.UUID.randomUUID

class CrossBackendTest {
    enum class Backends {
        PLAIN {
            override val get
                get() = Plain("plain-test", "/tmp/parnas-${this.name.toLowerCase()}/${randomUUID()}.properties").also {
                    it.initialize()
                    it["COMMON_ENTRY"] = "common-entry"
                }
        },
        KEEPASS {
            override val get
                get() = KeePass("keepass-test", "/tmp/parnas-${this.name.toLowerCase()}/${randomUUID()}.kdbx", "test1234").also {
                    it.initialize()
                    it["COMMON_ENTRY"] = "common-entry"
                }
        },
        TOML {
            override val get
                get() = Toml("toml-test", "/tmp/parnas-${this.name.toLowerCase()}/${randomUUID()}.toml").also {
                    it.initialize()
                    it["COMMON_ENTRY"] = "common-entry"
                }
        },
        SSM {
            override val get
                get() = SSM("ssm-test", ssmClient, "/tmp/parnas-${this.name.toLowerCase()}/${randomUUID()}/", "1111").also {
                    it["COMMON_ENTRY"] = "common-entry"
                }
        };

        abstract val get: Backend
    }

    companion object {
        private const val containerPort = 4583
        private const val awsRegion = "eu-west-1"

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


        @JvmStatic
        @BeforeAll
        fun createTestDirectory() {
            Backends.values().forEach {
                File("/tmp/parnas-${it.name.toLowerCase()}").mkdir()
            }
        }

        @JvmStatic
        @AfterAll
        fun cleanupTestBackend() {
            Backends.values().forEach {
                FileUtils.deleteDirectory(File("/tmp/parnas-${it.name.toLowerCase()}"))
            }
        }
    }

    @Test
    fun diffPlain_twoBackendsWithUniqueEntries_correctDiffBetweenBackends() {
        val backend = Backends.PLAIN.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Backends.values().forEach {
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(backend.diff(otherBackend) == expectedResult)
        }
    }

    @Test
    fun diffToml_twoBackendsWithUniqueEntries_correctDiffBetweenBackends() {
        val backend = Backends.TOML.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Backends.values().forEach {
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(backend.diff(otherBackend) == expectedResult)
        }
    }

    @Test
    fun diffKeepass_twoBackendsWithUniqueEntries_correctDiffBetweenBackends() {
        val backend = Backends.KEEPASS.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Backends.values().forEach {
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(backend.diff(otherBackend) == expectedResult)
        }
    }

    @Test
    fun diffSSM_twoBackendsWithUniqueEntries_correctDiffBetweenBackends() {
        val backend = Backends.SSM.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Backends.values().forEach {
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(backend.diff(otherBackend) == expectedResult)
        }
    }

    @Test
    fun updateFromPlain_twoBackendsWithUniqueEntries_firstBackendHasAllItsValuesAndValuesFromTheSecondBackend() {
        Backends.values().forEach {
            val backend = Backends.PLAIN.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = backend.list() + otherBackend.list()

            backend.updateFrom(otherBackend)

            Assertions.assertTrue(backend.list() == expectedResult)
        }
    }

    @Test
    fun updateFromToml_twoBackendsWithUniqueEntries_firstBackendHasAllItsValuesAndValuesFromTheSecondBackend() {
        Backends.values().forEach {
            val backend = Backends.TOML.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = backend.list() + otherBackend.list()

            backend.updateFrom(otherBackend)

            Assertions.assertTrue(backend.list() == expectedResult)
        }
    }

    @Test
    fun updateFromKeepass_twoBackendsWithUniqueEntries_firstBackendHasAllItsValuesAndValuesFromTheSecondBackend() {
        Backends.values().forEach {

            val backend = Backends.KEEPASS.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = backend.list() + otherBackend.list()

            backend.updateFrom(otherBackend)

            Assertions.assertTrue(backend.list() == expectedResult)
        }
    }

    @Test
    fun updateFromSSM_twoBackendsWithUniqueEntries_firstBackendHasAllItsValuesAndValuesFromTheSecondBackend() {
        Backends.values().forEach {
            val backend = Backends.SSM.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherBackend = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = backend.list() + otherBackend.list()

            backend.updateFrom(otherBackend)

            Assertions.assertTrue(backend.list() == expectedResult)
        }
    }
}
