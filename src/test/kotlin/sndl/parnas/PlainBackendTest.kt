package sndl.parnas

import org.junit.jupiter.api.*
import sndl.parnas.backend.ConfigOption
import sndl.parnas.backend.impl.Plain
import java.io.File

class PlainBackendTest {
    private val backend
        get() = Plain("plain-test", "/tmp/parnas-plain.properties").also {
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
        File("/tmp/parnas-plain.properties").delete()
    }
}
