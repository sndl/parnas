package sndl.parnas

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import sndl.parnas.config.Config
import sndl.parnas.storage.impl.Plain
import sndl.parnas.storage.impl.Toml
import sndl.parnas.utils.ConfigurationException
import java.io.File
import java.util.UUID

class ConfigTest {

    @TempDir
    lateinit var tempDir: File

    // Config has a static storage cache keyed by name — use unique names per test
    // to avoid cross-test contamination.
    private fun uid() = UUID.randomUUID().toString().take(8)

    private fun conf(content: String): Config {
        val file = File(tempDir, "${uid()}.conf").also { it.writeText(content.trimIndent()) }
        return Config(file)
    }

    // --- getStorage ---

    @Test
    fun getStorage_plainType_returnsPlainStorage() {
        val name = uid()
        val config = conf("""
            [$name]
            type = plain
            path = ${tempDir.resolve("$name.properties")}
        """)
        assertInstanceOf(Plain::class.java, config.getStorage(name))
    }

    @Test
    fun getStorage_tomlType_returnsTomlStorage() {
        val name = uid()
        val config = conf("""
            [$name]
            type = toml
            path = ${tempDir.resolve("$name.toml")}
        """)
        assertInstanceOf(Toml::class.java, config.getStorage(name))
    }

    @Test
    fun getStorage_unknownStorageName_throwsConfigurationException() {
        val name = uid()
        val config = conf("""
            [$name]
            type = plain
            path = ${tempDir.resolve("$name.properties")}
        """)
        assertThrows<ConfigurationException> { config.getStorage("nonexistent-${uid()}") }
    }

    @Test
    fun getStorage_unknownType_throwsConfigurationException() {
        val name = uid()
        val config = conf("""
            [$name]
            type = foobar
            path = ${tempDir.resolve("$name.properties")}
        """)
        assertThrows<ConfigurationException> { config.getStorage(name) }
    }

    // --- getStoragesByTag ---

    @Test
    fun getStoragesByTag_all_returnsAllStorages() {
        val s1 = uid(); val s2 = uid()
        val config = conf("""
            [$s1]
            type = plain
            path = ${tempDir.resolve("$s1.properties")}

            [$s2]
            type = plain
            path = ${tempDir.resolve("$s2.properties")}
        """)
        assertEquals(2, config.getStoragesByTag("all").size)
    }

    @Test
    fun getStoragesByTag_matchingTag_returnsOnlyTaggedStorages() {
        val s1 = uid(); val s2 = uid()
        val config = conf("""
            [$s1]
            tags = prod
            type = plain
            path = ${tempDir.resolve("$s1.properties")}

            [$s2]
            tags = dev
            type = plain
            path = ${tempDir.resolve("$s2.properties")}
        """)
        val result = config.getStoragesByTag("prod")
        assertEquals(1, result.size)
        assertEquals(s1, result.first().name)
    }

    @Test
    fun getStoragesByTag_noMatchingTag_returnsEmpty() {
        val name = uid()
        val config = conf("""
            [$name]
            tags = prod
            type = plain
            path = ${tempDir.resolve("$name.properties")}
        """)
        assertTrue(config.getStoragesByTag("staging").isEmpty())
    }

    @Test
    fun getStoragesByTag_multipleTags_matchesAnyTag() {
        val name = uid()
        val config = conf("""
            [$name]
            tags = prod, staging
            type = plain
            path = ${tempDir.resolve("$name.properties")}
        """)
        assertEquals(1, config.getStoragesByTag("staging").size)
        assertEquals(1, config.getStoragesByTag("prod").size)
        assertTrue(config.getStoragesByTag("dev").isEmpty())
    }

    @Test
    fun getStoragesByTag_multipleStoragesWithSameTag_returnsAll() {
        val s1 = uid(); val s2 = uid()
        val config = conf("""
            [$s1]
            tags = prod
            type = plain
            path = ${tempDir.resolve("$s1.properties")}

            [$s2]
            tags = prod
            type = plain
            path = ${tempDir.resolve("$s2.properties")}
        """)
        assertEquals(2, config.getStoragesByTag("prod").size)
    }

    // --- INI format edge cases ---

    @Test
    fun parse_commentsAreIgnored_storageStillResolved() {
        val name = uid()
        val config = conf("""
            ; this is a comment
            # this is also a comment
            [$name]
            ; comment before key
            type = plain
            path = ${tempDir.resolve("$name.properties")}
        """)
        assertInstanceOf(Plain::class.java, config.getStorage(name))
    }

    @Test
    fun parse_indentedKeys_storageStillResolved() {
        val name = uid()
        val config = conf("""
            [$name]
                type = plain
                path = ${tempDir.resolve("$name.properties")}
        """)
        assertInstanceOf(Plain::class.java, config.getStorage(name))
    }

    @Test
    fun parse_valueContainsEquals_fullValuePreserved() {
        val name = uid()
        val config = conf("""
            [$name]
            type = plain
            path = ${tempDir.resolve("$name.properties")}
            extra = a=b
        """)
        assertInstanceOf(Plain::class.java, config.getStorage(name))
    }

    @Test
    fun parse_blankLinesBetweenSections_allSectionsResolved() {
        val s1 = uid(); val s2 = uid()
        val config = conf("""
            [$s1]
            type = plain
            path = ${tempDir.resolve("$s1.properties")}


            [$s2]
            type = toml
            path = ${tempDir.resolve("$s2.toml")}
        """)
        assertInstanceOf(Plain::class.java, config.getStorage(s1))
        assertInstanceOf(Toml::class.java, config.getStorage(s2))
    }
}
