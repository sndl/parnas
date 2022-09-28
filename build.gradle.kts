import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import tanvd.kosogor.proxy.shadowJar

group = "sndl.parnas"
version = "0.2.4"
description = "PARameter Naming And Storing"

plugins {
    id("tanvd.kosogor") version "1.0.14"
    kotlin("jvm") version "1.6.21" apply true
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
    id("com.github.breadmoirai.github-release") version "2.3.7"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("de.slackspace", "openkeepass", "0.8.2")
    implementation("com.amazonaws", "aws-java-sdk-ssm", "1.12.239")
    implementation("com.electronwill.night-config", "toml", "3.6.5")
    implementation("com.electronwill.night-config", "core", "3.6.5")

    // TODO: update clikt to a major release 3.5.0
    implementation("com.github.ajalt.clikt", "clikt", "3.5.0")
    implementation("com.github.ajalt", "mordant", "1.2.1")

    implementation("org.ini4j", "ini4j", "0.5.4")
    implementation("org.apache.logging.log4j", "log4j-core", "2.17.1")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.17.1")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.2")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.8.2")
    testImplementation("org.testcontainers", "testcontainers", "1.17.2")
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.6"
        apiVersion = "1.6"
    }
}

detekt {
    parallel = true
    failFast = false
    config = files(File(project.rootProject.projectDir, "buildScripts/detekt/detekt.yml"))
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
