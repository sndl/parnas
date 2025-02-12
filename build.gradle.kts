import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import tanvd.kosogor.proxy.shadowJar

group = "sndl.parnas"
version = "0.2.8"
description = "PARameter Naming And Storing"

plugins {
    id("tanvd.kosogor") version "1.0.22"
    kotlin("jvm") version "2.0.21" apply true
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
    id("com.github.breadmoirai.github-release") version "2.3.7"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.linguafranca.pwdb", "KeePassJava2-jackson", "2.2.3")
    implementation("software.amazon.awssdk", "ssm", "2.28.7")
    implementation("com.electronwill.night-config", "toml", "3.6.5")
    implementation("com.electronwill.night-config", "core", "3.6.5")

    implementation("com.github.ajalt.clikt", "clikt", "3.5.0")
    implementation("com.github.ajalt", "mordant", "1.2.1")

    implementation("org.ini4j", "ini4j", "0.5.4")
    implementation("org.apache.logging.log4j", "log4j-core", "2.17.1")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.17.1")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
    testImplementation("org.testcontainers", "testcontainers", "1.20.3")
    testImplementation("org.testcontainers", "localstack", "1.20.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("17"))
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        // https://jakewharton.com/kotlins-jdk-release-compatibility-flag/
        // https://youtrack.jetbrains.com/issue/KT-49746/Support-Xjdk-release-in-gradle-toolchain#focus=Comments-27-8935065.0-0
        freeCompilerArgs.addAll("-Xjdk-release=17")
    }
}

detekt {
    parallel = true
    config.setFrom(files(File(project.rootProject.projectDir, "buildScripts/detekt/detekt.yml")))
    reports {
        xml {
            required.set(false)
        }
        html {
            required.set(false)
        }
    }
}

val shadowJar = shadowJar {
    jar {
        mainClass = "sndl.parnas.MainKt"
    }
}.apply {
    task.archiveClassifier.set("")

    task.from(File("src/main/resources/version.txt").apply {
        if (!exists()) {
            parentFile.mkdirs()
            createNewFile()
        }

        writeText(project.version.toString())
    })
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    owner("sndl")
    targetCommitish("master")
    releaseAssets(shadowJar.task.archiveFile.get())
}

tasks.withType(ShadowJar::class) {
    dependsOn("build")
}

tasks.withType(GithubReleaseTask::class) {
    dependsOn("shadowJar")
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}
