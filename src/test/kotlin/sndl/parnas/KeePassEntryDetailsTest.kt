package sndl.parnas

import org.junit.jupiter.api.*
import sndl.parnas.storage.impl.keepass.KeePass
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import java.io.File
import java.util.UUID.randomUUID

class KeePassEntryDetailsTest {
    companion object {
        private val storage
            get() = KeePass("keepass-test", TestUtils.tmpFilePath("parnas-keepass-details/${randomUUID()}.kdbx"), "test1234").also {
                it.initialize()
            }

        @JvmStatic
        @BeforeAll
        fun createTestDirectory() {
            File("/tmp/parnas-keepass-details").mkdir()
        }

        @JvmStatic
        @AfterAll
        fun cleanupTestStorage() {
            FileUtils.deleteDirectory(File("/tmp/parnas-keepass-details/"))
        }
    }

    @Test
    fun testEntryWithSpecialCharacters() {
        val testStorage = storage
        
        // Test keys with special characters
        val specialChars = listOf(
            "KEY_WITH_SPACES",
            "KEY-WITH-DASH",
            "KEY_WITH_@_SYMBOL",
            "KEY_WITH_#_HASH",
            "KEY_WITH_\$_DOLLAR",
            "KEY.WITH.DOTS",
            "KEY/WITH/SLASHES",
            "KEY\\WITH\\BACKSLASHES",
            "KEY_WITH_UNICODE_Üñïçødé"
        )
        
        for (key in specialChars) {
            testStorage[key] = "value_for_$key"
            Assertions.assertEquals("value_for_$key", testStorage[key]?.value)
        }
        
        // Verify all entries are in the list
        val entries = testStorage.list()
        Assertions.assertEquals(specialChars.size, entries.size)
        
        for (key in specialChars) {
            Assertions.assertTrue(entries.any { it.key == key })
        }
    }
    
    @Test
    fun testEntryWithLongValues() {
        val testStorage = storage
        
        // Create a very long value (10KB)
        val longValue = "A".repeat(10_000)
        
        // Store and retrieve it
        testStorage["LONG_VALUE_KEY"] = longValue
        val retrievedValue = testStorage["LONG_VALUE_KEY"]?.value
        
        Assertions.assertEquals(longValue, retrievedValue)
        Assertions.assertEquals(10_000, retrievedValue?.length)
    }
    
    @Test
    fun testEntryWithEmptyValue() {
        val testStorage = storage
        
        // Store and retrieve an empty value
        testStorage["EMPTY_VALUE_KEY"] = ""
        val retrievedValue = testStorage["EMPTY_VALUE_KEY"]?.value
        
        Assertions.assertEquals("", retrievedValue)
    }
    
    @Test
    fun testDuplicateEntries() {
        val testStorage = storage
        
        // Create an entry
        testStorage["DUPLICATE_KEY"] = "original_value"
        
        // Create the same entry again with a different value
        testStorage["DUPLICATE_KEY"] = "new_value"
        
        // Verify that the value was updated (not duplicated)
        val entries = testStorage.list()
        val matchingEntries = entries.filter { it.key == "DUPLICATE_KEY" }
        
        Assertions.assertEquals(1, matchingEntries.size)
        Assertions.assertEquals("new_value", matchingEntries.first().value)
    }
    
    @Test
    fun testCaseSensitivity() {
        val testStorage = storage
        
        // Create entries with different case
        testStorage["lowercase_key"] = "lowercase_value"
        testStorage["UPPERCASE_KEY"] = "uppercase_value"
        testStorage["MixedCase_Key"] = "mixed_value"
        
        // Verify exact case matching
        Assertions.assertEquals("lowercase_value", testStorage["lowercase_key"]?.value)
        Assertions.assertEquals("uppercase_value", testStorage["UPPERCASE_KEY"]?.value)
        Assertions.assertEquals("mixed_value", testStorage["MixedCase_Key"]?.value)
        
        // Verify non-matching cases return null
        Assertions.assertNull(testStorage["LOWERCASE_KEY"])
        Assertions.assertNull(testStorage["uppercase_key"])
        Assertions.assertNull(testStorage["mixedcase_key"])
    }
}