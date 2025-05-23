package sndl.parnas

import org.junit.jupiter.api.*
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import sndl.parnas.storage.impl.keepass.KeePass
import sndl.parnas.utils.WrongSecret
import java.io.File

class KeePassPasswordFileTest {
    companion object {
        private const val TEST_PASSWORD = "testPassword123"
        private lateinit var passwordFile: File
        private lateinit var kdbxFile: String

        @JvmStatic
        @BeforeAll
        fun setup() {
            // Create a directory for testing
            File(TestUtils.tmpFilePath("parnas-keepass-pwd-test")).mkdirs()
            
            // Create a password file
            passwordFile = File(TestUtils.tmpFilePath("parnas-keepass-pwd-test/password.txt"))
            passwordFile.writeText(TEST_PASSWORD)
            
            kdbxFile = TestUtils.tmpFilePath("parnas-keepass-pwd-test/test.kdbx")
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            FileUtils.deleteDirectory(File(TestUtils.tmpFilePath("parnas-keepass-pwd-test")))
        }
    }

    @Test
    fun testLoadingPasswordFromFile() {
        // Create a configuration map that includes password-from-file
        val configMap = mapOf(
            "name" to "keepass-test",
            "type" to "keepass",
            "path" to kdbxFile,
            "password-from-file" to passwordFile.absolutePath
        )

        // Initialize KeePass with the config map
        val storage = KeePass("keepass-test", configMap)
        
        // Initialize the database
        storage.initialize()
        
        // Add a test entry
        storage["TEST_KEY"] = "test_value"
        
        // Verify we can read it back
        Assertions.assertEquals("test_value", storage["TEST_KEY"]?.value)
    }

    @Test
    fun testWrongPasswordFileThrowsException() {
        // Create a password file with wrong content
        val wrongPasswordFile = File(TestUtils.tmpFilePath("parnas-keepass-pwd-test/wrong-password.txt"))
        wrongPasswordFile.writeText("wrongPassword")
        
        // Create a KeePass database with the correct password
        val storage = KeePass("keepass-test", kdbxFile, TEST_PASSWORD)
        storage.initialize()
        storage["TEST_KEY"] = "test_value"
        
        // Try to open it with a config using the wrong password file
        val configMap = mapOf(
            "name" to "keepass-test",
            "type" to "keepass",
            "path" to kdbxFile,
            "password-from-file" to wrongPasswordFile.absolutePath
        )
        
        // This should throw a WrongSecret exception
        Assertions.assertThrows(WrongSecret::class.java) {
            KeePass("keepass-test", configMap)
        }
        
        // Clean up
        wrongPasswordFile.delete()
    }

    @Test
    fun testMissingPasswordFile() {
        val nonExistentFile = TestUtils.tmpFilePath("parnas-keepass-pwd-test/non-existent.txt")
        
        // Create a configuration map with a non-existent password file
        val configMap = mapOf(
            "name" to "keepass-test",
            "type" to "keepass",
            "path" to kdbxFile,
            "password-from-file" to nonExistentFile
        )
        
        // This should throw an exception since the password file doesn't exist
        // and there's no fallback password
        Assertions.assertThrows(Exception::class.java) {
            KeePass("keepass-test", configMap)
        }
    }

    @Test
    fun testEmptyPasswordFile() {
        // Create an empty password file
        val emptyPasswordFile = File(TestUtils.tmpFilePath("parnas-keepass-pwd-test/empty-password.txt"))
        emptyPasswordFile.writeText("")
        
        // Create a configuration map with the empty password file
        val configMap = mapOf(
            "name" to "keepass-test",
            "type" to "keepass",
            "path" to kdbxFile,
            "password-from-file" to emptyPasswordFile.absolutePath
        )
        
        // This should throw an exception since the password file is empty
        Assertions.assertThrows(Exception::class.java) {
            KeePass("keepass-test", configMap)
        }
        
        // Clean up
        emptyPasswordFile.delete()
    }
}