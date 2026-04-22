package sndl.parnas.cli

import java.io.File

fun installSkill() {
    val skillContent = object {}::class.java.getResource("/skill.md")?.readText()
        ?: error("Bundled skill resource not found — ensure the JAR was built correctly")

    val dest = File(System.getProperty("user.home")).resolve(".claude/skills/parnas/SKILL.md")
    dest.parentFile.mkdirs()
    dest.writeText(skillContent)

    println("Skill installed at ${dest.absolutePath}")
}
