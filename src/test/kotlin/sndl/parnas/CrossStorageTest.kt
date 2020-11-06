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
import sndl.parnas.storage.Storage
import sndl.parnas.storage.ConfigOption
import sndl.parnas.storage.impl.Plain
import sndl.parnas.storage.impl.SSM
import sndl.parnas.storage.impl.Toml
import sndl.parnas.storage.impl.keepass.KeePass
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.util.UUID.randomUUID

class CrossStorageTest {
    enum class Storages {
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

        abstract val get: Storage
    }

    companion object {
        private const val awsRegion = "eu-west-1"

        @ClassRule
        private val localstack = TestContainersFactory.getLocalstack()
        private val ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
                .withEndpointConfiguration(AwsClientBuilder
                        .EndpointConfiguration(
                                "http://${localstack.containerIpAddress}:${localstack.firstMappedPort}",
                                awsRegion))
                .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("dummy", "dummy")))
                .build()


        @JvmStatic
        @BeforeAll
        fun createTestDirectory() {
            Storages.values().forEach {
                File("/tmp/parnas-${it.name.toLowerCase()}").mkdir()
            }
        }

        @JvmStatic
        @AfterAll
        fun cleanupTestStorage() {
            Storages.values().forEach {
                FileUtils.deleteDirectory(File("/tmp/parnas-${it.name.toLowerCase()}"))
            }
        }
    }

    @Test
    fun diffPlain_twoStoragesWithUniqueEntries_correctDiffBetweenStorages() {
        val storage = Storages.PLAIN.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Storages.values().forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(storage.diff(otherStorage) == expectedResult)
        }
    }

    @Test
    fun diffToml_twoStoragesWithUniqueEntries_correctDiffBetweenStorages() {
        val storage = Storages.TOML.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Storages.values().forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(storage.diff(otherStorage) == expectedResult)
        }
    }

    @Test
    fun diffKeepass_twoStoragesWithUniqueEntries_correctDiffBetweenStorages() {
        val storage = Storages.KEEPASS.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Storages.values().forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(storage.diff(otherStorage) == expectedResult)
        }
    }

    @Test
    fun diffSSM_twoStoragesWithUniqueEntries_correctDiffBetweenStorages() {
        val storage = Storages.SSM.get.also {
            it["UNIQUE_ENTRY_1"] = "unique-entry-1"
        }
        val expectedResult = Pair(
                listOf(ConfigOption("UNIQUE_ENTRY_2", "unique-entry-2")).toLinkedSet(),
                listOf(ConfigOption("UNIQUE_ENTRY_1", "unique-entry-1")).toLinkedSet())

        Storages.values().forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertTrue(storage.diff(otherStorage) == expectedResult)
        }
    }

    @Test
    fun updateFromPlain_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.values().forEach {
            val storage = Storages.PLAIN.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }

    @Test
    fun updateFromToml_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.values().forEach {
            val storage = Storages.TOML.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }

    @Test
    fun updateFromKeepass_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.values().forEach {

            val storage = Storages.KEEPASS.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }

    @Test
    fun updateFromSSM_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.values().forEach {
            val storage = Storages.SSM.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }
}
