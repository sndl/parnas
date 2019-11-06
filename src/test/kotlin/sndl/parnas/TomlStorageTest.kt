package sndl.parnas

import org.junit.jupiter.api.*
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import sndl.parnas.storage.ConfigOption
import sndl.parnas.storage.impl.Toml
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.lang.IllegalArgumentException
import java.util.UUID.randomUUID

class TomlStorageTest {
    companion object {
        private val storage
            get() = Toml("toml-test", "/tmp/parnas-toml/${randomUUID()}.toml").also {
                it.initialize()
                it["FIRST_ENTRY"] = "first-entry"
                it["SECOND_ENTRY"] = "second-entry"
                it["SECTION1.FIRST_ENTRY"] = "section1-first-entry"
                it["SECTION1.SUBSECTION1.FIRST_ENTRY"] = "section1-subsection1-first-entry"
                it["SECTION1.SUBSECTION1.SUBSECTION2.FIRST_ENTRY"] = "section1-subsection1-subsection2-first-entry"
            }

        @JvmStatic
        @BeforeAll
        fun createTestDirectory() {
            File("/tmp/parnas-toml").mkdir()
        }

        @JvmStatic
        @AfterAll
        fun cleanupTestStorage() {
            FileUtils.deleteDirectory(File("/tmp/parnas-toml/"))
        }
    }

    @Test
    fun list_storageIsNotEmpty_gotNotEmptyList() {
        Assertions.assertTrue(storage.list().size > 0)
    }

    @Test
    fun list_storageIsNotEmpty_gotExpectedListOfValues() {
        val list = storage.list()
        val expectedList = setOf(
                ConfigOption("FIRST_ENTRY", "first-entry"),
                ConfigOption("SECOND_ENTRY", "second-entry"),
                ConfigOption("SECTION1.FIRST_ENTRY", "section1-first-entry"),
                ConfigOption("SECTION1.SUBSECTION1.FIRST_ENTRY", "section1-subsection1-first-entry"),
                ConfigOption("SECTION1.SUBSECTION1.SUBSECTION2.FIRST_ENTRY", "section1-subsection1-subsection2-first-entry")
        )

        Assertions.assertEquals(expectedList, list)
    }

    @Test
    fun get_entryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("FIRST_ENTRY", "first-entry"), storage["FIRST_ENTRY"])
    }

    @Test
    fun getEntryFromSection_entryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("SECTION1.FIRST_ENTRY", "section1-first-entry"),
                storage["SECTION1.FIRST_ENTRY"])
    }

    @Test
    fun getEntryFromSubsection_entryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("SECTION1.SUBSECTION1.FIRST_ENTRY", "section1-subsection1-first-entry"),
                storage["SECTION1.SUBSECTION1.FIRST_ENTRY"])
    }

    @Test
    fun get_entryDoesNotExist_gotNull() {
        Assertions.assertNull(storage["THIRD_ENTRY"])
    }

    @Test
    fun get_sectionEntryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("SECTION1.FIRST_ENTRY", "section1-first-entry"),
                storage["SECTION1.FIRST_ENTRY"])
    }

    @Test
    fun get_sectionEntryDoesNotExist_gotNull() {
        Assertions.assertNull(storage["SECTION1.SECOND_ENTRY"])
    }

    @Test
    fun set_entryDoesNotExist_entryExists() {
        val testStorage = storage

        testStorage["THIRD_ENTRY"] = "third-entry"

        val expectedEntry = ConfigOption("THIRD_ENTRY", "third-entry")
        val entry = testStorage["THIRD_ENTRY"]

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun set_entryDoesNotExist_exactlyOneEntryIsCreated() {
        val testStorage = storage
        val beforeSize = testStorage.list().size

        testStorage["THIRD_ENTRY"] = "third-entry"

        val afterSize = testStorage.list().size

        Assertions.assertTrue(afterSize - beforeSize == 1)
    }

    @Test
    fun set_entryExists_entryUpdated() {
        val testStorage = storage

        testStorage["FIRST_ENTRY"] = "updated-first-entry"

        val expectedEntry = ConfigOption("FIRST_ENTRY", "updated-first-entry")
        val entry = testStorage["FIRST_ENTRY"]

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun delete_entryExists_entryDoesNotExist() {
        val testStorage = storage
        testStorage.delete("FIRST_ENTRY")
        Assertions.assertNull(testStorage["FIRST_ENTRY"])
    }

    @Test
    fun delete_entryExists_exactlyOneEntryIsRemoved() {
        val testStorage = storage
        val sizeBefore = testStorage.list().size

        testStorage.delete("FIRST_ENTRY")

        val sizeAfter = testStorage.list().size

        Assertions.assertTrue(sizeBefore - sizeAfter == 1)
    }

    @Test
    fun destroyNonDestroyableStorage_storageExistsAndHasRecords_storageExistsAndHasRecords() {
        val testStorage = storage

        assertThrows<IllegalArgumentException> {
            testStorage.destroy()
        }
    }

    @Test
    fun destroy_storageHasRecords_storageIsEmpty() {
        val testStorage = storage

        testStorage.permitDestroy = true
        testStorage.destroy()

        Assertions.assertTrue(testStorage.list().size == 0)
    }

    @Test
    fun updateFrom_firstStorageHasRecords_firstStoragesHasAllItsRecordsAndRecordsFromSecondStorage() {
        val storage1 = storage
        val storage2 = storage.also {
            it["additional_record1"] = "val1"
            it["additional_record2"] = "val2"
            it["additional_record3"] = "val3"
        }

        val storage1BeforeUpdateList = storage1.list()

        storage1.updateFrom(storage2)
        storage1.diff(storage2)

        val expectedResult = storage1BeforeUpdateList + storage2.list()

        Assertions.assertTrue(storage1.list() == expectedResult)
    }

    @Test
    fun diff_bothStoragesHaveEntries_listOfDifferentEntriesReturned() {
        val storage1 = storage.also {
            it["commonRecord"] = "commonRecord"
            it["uniqueRecord1"] = "uniqueRecord1"
        }
        val storage2 = storage.also {
            it["commonRecord"] = "commonRecord"
            it["uniqueRecord2"] = "uniqueRecord2"
        }

        val expectedResult = Pair(
                listOf(ConfigOption("uniqueRecord2", "uniqueRecord2")).toLinkedSet(),
                listOf(ConfigOption("uniqueRecord1", "uniqueRecord1")).toLinkedSet())

        Assertions.assertTrue(storage1.diff(storage2) == expectedResult)
    }
}
