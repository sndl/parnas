package sndl.parnas

import org.junit.jupiter.api.*
import org.testcontainers.shaded.org.apache.commons.io.FileUtils
import sndl.parnas.storage.impl.keepass.KeePass
import sndl.parnas.utils.WrongSecret
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID.randomUUID

class KeePassErrorHandlingTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun createTestDirectory() {
            File("/tmp/parnas-keepass-errors").mkdir()
        }

        @JvmStatic
        @AfterAll
        fun cleanupTestStorage() {
            FileUtils.deleteDirectory(File("/tmp/parnas-keepass-errors/"))
        }
    }

    @Test
    fun testNonExistentDatabase() {
        val dbPath = TestUtils.tmpFilePath("parnas-keepass-errors/${randomUUID()}.kdbx")
        
        // Create a KeePass instance without initializing
        val storage = KeePass("keepass-test", dbPath, "password123")
        
        // Verify the database is not initialized
        Assertions.assertFalse(storage.isInitialized)
        
        // Trying to list entries should throw an exception
        Assertions.assertThrows(Exception::class.java) {
            storage.list()
        }
    }

    @Test
    fun testCorruptedDatabase() {
        val dbPath = TestUtils.tmpFilePath("parnas-keepass-errors/corrupted.kdbx")
        
        // Create a corrupted database file (just write some random bytes)
        File(dbPath).writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        
        // Trying to open it should throw an exception
        Assertions.assertThrows(Exception::class.java) {
            KeePass("keepass-test", dbPath, "password123")
        }
    }

    @Test
    fun testInvalidPassword() {
        val dbPath = TestUtils.tmpFilePath("parnas-keepass-errors/valid.kdbx")
        
        // Create a database with a specific password
        val storage = KeePass("keepass-test", dbPath, "correctPassword")
        storage.initialize()
        storage["TEST_KEY"] = "test_value"
        
        // Try to open it with the wrong password
        Assertions.assertThrows(WrongSecret::class.java) {
            KeePass("keepass-test", dbPath, "wrongPassword")
        }
    }

    @Test
    fun testPermissionDenied() {
        // Skip test on Windows which doesn't support POSIX permissions
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        Assumptions.assumeFalse(isWindows)
        
        val dbPath = TestUtils.tmpFilePath("parnas-keepass-errors/readonly.kdbx")
        
        // Create the database
        val storage = KeePass("keepass-test", dbPath, "password123")
        storage.initialize()
        
        try {
            // Make the file read-only
            Files.setPosixFilePermissions(Paths.get(dbPath), 
                PosixFilePermissions.fromString("r--r--r--"))
            
            // Try to write to it - should throw an exception
            Assertions.assertThrows(Exception::class.java) {
                storage["TEST_KEY"] = "test_value"
            }
        } finally {
            // Restore permissions to be able to clean up
            try {
                Files.setPosixFilePermissions(Paths.get(dbPath), 
                    PosixFilePermissions.fromString("rw-rw-rw-"))
            } catch (_: Exception) {
                // Ignore failures here, as cleanup will attempt to delete anyway
            }
        }
    }
    
    @Test
    fun testNonExistentParentDirectory() {
        val nonExistentDir = "/tmp/parnas-keepass-errors/non-existent-dir"
        val dbPath = "$nonExistentDir/${randomUUID()}.kdbx"
        
        // Create KeePass storage pointing to a file in a non-existent directory
        val storage = KeePass("keepass-test", dbPath, "password123")
        
        // The initialize method should handle creating parent directories
        storage.initialize()
        
        // Verify the database was created
        Assertions.assertTrue(storage.isInitialized)
        
        // Test basic functionality
        storage["TEST_KEY"] = "test_value"
        Assertions.assertEquals("test_value", storage["TEST_KEY"]?.value)
        
        // Clean up
        FileUtils.deleteDirectory(File(nonExistentDir))
    }
}