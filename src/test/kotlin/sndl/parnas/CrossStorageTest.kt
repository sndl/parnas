package sndl.parnas

import org.junit.ClassRule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import sndl.parnas.TestUtils.tmpFilePath
import sndl.parnas.storage.Storage
import sndl.parnas.storage.ConfigOption
import sndl.parnas.storage.impl.Plain
import sndl.parnas.storage.impl.SSM
import sndl.parnas.storage.impl.Toml
import sndl.parnas.storage.impl.keepass.KeePass
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.util.*
import java.util.UUID.randomUUID

class CrossStorageTest {
    enum class Storages {
        PLAIN {
            override val get
                get() = Plain("plain-test", tmpFilePath("parnas-${name.lowercase()}/${randomUUID()}.properties")).also {
                    it.initialize()
                    it["COMMON_ENTRY"] = "common-entry"
                }
        },
        KEEPASS {
            override val get
                get() = KeePass("keepass-test",
                    tmpFilePath("parnas-${name.lowercase(Locale.getDefault())}/${randomUUID()}.kdbx"), "test1234").also {
                    it.initialize()
                    it["COMMON_ENTRY"] = "common-entry"
                }
        },
        TOML {
            override val get
                get() = Toml("toml-test", tmpFilePath("parnas-${name.lowercase(Locale.getDefault())}/${randomUUID()}.toml")).also {
                    it.initialize()
                    it["COMMON_ENTRY"] = "common-entry"
                }
        },
        SSM {
            override val get: SSM
                get() {
                    // Fix prefix for WindowsPath
                    val paramPath = tmpFilePath("parnas-${name.lowercase(Locale.getDefault())}/${randomUUID()}/")
                        .replace('\\', '/')
                        .substringAfter(':')
                    return SSM("ssm-test", ssmClient, paramPath, "1111").also {
                        it["COMMON_ENTRY"] = "common-entry"
                    }
                }
        };

        abstract val get: Storage
    }

    companion object {
        @ClassRule
        private val localstack = TestContainersFactory.getLocalstack()
        private val ssmClient = TestUtils.buildClient(localstack)

        @JvmStatic
        @AfterAll
        fun cleanupTestStorage() {
            Storages.entries.forEach {
                FileUtils.deleteDirectory(File(tmpFilePath("parnas-${it.name.lowercase(Locale.getDefault())}")))
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

        Storages.entries.forEach {
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

        Storages.entries.forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertEquals(expectedResult, storage.diff(otherStorage), "Failed to compare ${storage.name} and ${otherStorage.name}")
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

        Storages.entries.forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertEquals(storage.diff(otherStorage), expectedResult, "Failed to compare ${storage.name} and ${otherStorage.name}")
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

        Storages.entries.forEach {
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }

            Assertions.assertEquals(storage.diff(otherStorage), expectedResult, "Failed to compare ${storage.name} and ${otherStorage.name}")
        }
    }

    @Test
    fun updateFromPlain_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.entries.forEach {
            val storage = Storages.PLAIN.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }

    @Test
    fun updateFromToml_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.entries.forEach {
            val storage = Storages.TOML.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }

    @Test
    fun updateFromKeepass_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.entries.forEach {

            val storage = Storages.KEEPASS.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }

    @Test
    fun updateFromSSM_twoStoragesWithUniqueEntries_firstStorageHasAllItsValuesAndValuesFromTheSecondStorage() {
        Storages.entries.forEach {
            val storage = Storages.SSM.get.also { it["UNIQUE_ENTRY_1"] = "unique-entry-1" }
            val otherStorage = it.get.also { it["UNIQUE_ENTRY_2"] = "unique-entry-2" }
            val expectedResult = storage.list() + otherStorage.list()

            storage.updateFrom(otherStorage)

            Assertions.assertTrue(storage.list() == expectedResult)
        }
    }
}
