package sndl.parnas

import org.junit.jupiter.api.*
import sndl.parnas.backend.ConfigOption
import sndl.parnas.backend.impl.keepass.KeePass
import sndl.parnas.utils.toLinkedSet
import java.io.File
import java.lang.IllegalArgumentException

class KeePassBackendTest {
    private val backend
        get() = KeePass("keepass-test", "/tmp/parnas-keepass.kdbx", "test1234").also {
            it.initialize()
            it["FIRST_ENTRY"] = "first-entry"
            it["SECOND_ENTRY"] = "second-entry"
        }

    @Test
    fun list_backendIsNotEmpty_gotNotEmptyList() {
        Assertions.assertTrue(backend.list().size > 0)
    }

    @Test
    fun list_backendIsNotEmpty_gotExpectedListOfValues() {
        val list = backend.list()
        val expectedList = setOf(
                ConfigOption("FIRST_ENTRY", "first-entry"),
                ConfigOption("SECOND_ENTRY", "second-entry")
        )

        Assertions.assertEquals(expectedList, list)
    }

    @Test
    fun get_entryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("FIRST_ENTRY", "first-entry"), backend["FIRST_ENTRY"])
    }

    @Test
    fun get_entryDoesNotExist_gotNull() {
        Assertions.assertNull(backend["THIRD_ENTRY"])
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
        testBackend.delete("FIRST_ENTRY")
        Assertions.assertNull(testBackend["FIRST_ENTRY"])
    }

    @Test
    fun delete_entryExists_exactlyOneEntryIsRemoved() {
        val testBackend = backend
        val sizeBefore = testBackend.list().size

        testBackend.delete("FIRST_ENTRY")

        val sizeAfter = testBackend.list().size

        Assertions.assertTrue(sizeBefore - sizeAfter == 1)
    }

    @AfterEach
    fun cleanupTestBackend() {
        File("/tmp/parnas-keepass.kdbx").delete()
    }

    @Test
    fun destroyNonDestroyableBackend_backendExistsAndHasRecords_backendExistsAndHasRecords() {
        val testBackend = backend

        assertThrows<IllegalArgumentException> {
            testBackend.destroy()
        }
    }

    @Test
    fun destroy_backendHasRecords_backendIsEmpty() {
        val testBackend = backend

        testBackend.permitDestroy = true
        testBackend.destroy()

        Assertions.assertTrue(testBackend.list().size == 0)
    }

    @Test
    fun updateFrom_firstBackendHasRecords_firstBackendsHasAllItsRecordsAndRecordsFromSecondBackend() {
        val backend1 = backend
        val backend2 = backend.also {
            it["additional_record1"] = "val1"
            it["additional_record2"] = "val2"
            it["additional_record3"] = "val3"
        }

        val backend1BeforeUpdateList = backend1.list()

        backend1.updateFrom(backend2)
        backend1.diff(backend2)

        val expectedResult = backend1BeforeUpdateList + backend2.list()

        Assertions.assertTrue(backend1.list() == expectedResult)
    }

    @Test
    fun diff_bothBackendsHaveEntries_listOfDifferentEntriesReturned() {
        val backend1 = backend.also {
            it["commonRecord"] = "commonRecord"
            it["uniqueRecord1"] = "uniqueRecord1"
        }
        val backend2 = backend.also {
            it["commonRecord"] = "commonRecord"
            it["uniqueRecord2"] = "uniqueRecord2"
        }

        val expectedResult = Pair(
                listOf(ConfigOption("uniqueRecord2", "uniqueRecord2")).toLinkedSet(),
                listOf(ConfigOption("uniqueRecord1", "uniqueRecord1")).toLinkedSet())

        Assertions.assertTrue(backend1.diff(backend2) == expectedResult)
    }
}
