package sndl.parnas

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import sndl.parnas.cli.installSkill
import java.io.File
import java.nio.file.Files

class InstallSkillTest {
    private val tmpHome = Files.createTempDirectory("parnas-install-skill-test").toFile()

    @AfterEach
    fun cleanup() {
        tmpHome.deleteRecursively()
    }

    @Test
    fun `installSkill writes skill file to the expected path`() {
        System.setProperty("user.home", tmpHome.absolutePath)

        installSkill()

        val dest = tmpHome.resolve(".claude/skills/parnas/SKILL.md")
        assertTrue(dest.exists(), "SKILL.md should exist at ${dest.absolutePath}")
    }

    @Test
    fun `installSkill content matches bundled resource`() {
        System.setProperty("user.home", tmpHome.absolutePath)

        installSkill()

        val expected = object {}::class.java.getResource("/skill.md")!!.readText()
        val actual = tmpHome.resolve(".claude/skills/parnas/SKILL.md").readText()
        assertEquals(expected, actual)
    }

    @Test
    fun `installSkill creates parent directories`() {
        System.setProperty("user.home", tmpHome.absolutePath)

        installSkill()

        assertTrue(tmpHome.resolve(".claude/skills/parnas").isDirectory)
    }
}
