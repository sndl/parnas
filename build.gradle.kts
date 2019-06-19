import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import tanvd.kosogor.proxy.shadowJar

group = "sndl.parnas"
version = "0.1.4"
description = "PARameter Naming And Storing"

plugins {
    id("tanvd.kosogor") version "1.0.5"
    kotlin("jvm") version "1.3.31" apply true
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC14"
    id("com.github.breadmoirai.github-release") version "2.2.9"
}

repositories {
    jcenter()
}

dependencies {
    compile("de.slackspace", "openkeepass", "0.8.2")
    compile("com.amazonaws", "aws-java-sdk-ssm", "1.11.555")
    compile("com.electronwill.night-config", "toml", "3.5.0")
    compile("com.electronwill.night-config", "core", "3.5.0")

    compile("com.github.ajalt", "clikt", "1.6.0")
    compile("com.github.ajalt", "mordant", "1.2.0")

    compile("org.ini4j", "ini4j", "0.5.4")

    testCompile("org.junit.jupiter", "junit-jupiter-api", "5.4.1")
    testRuntime("org.junit.jupiter", "junit-jupiter-engine", "5.4.1")
    testCompile("org.testcontainers", "testcontainers", "1.10.6")
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        languageVersion = "1.3"
        apiVersion = "1.3"
    }
}

detekt {
    parallel = true
    failFast = false
    config = files(File(project.rootProject.projectDir, "buildScripts/detekt/detekt.yml"))
    reports {
        xml {
            enabled = false
        }
        html {
            enabled = false
        }
    }
}

val shadowJar = shadowJar {
    jar {
        mainClass = "sndl.parnas.MainKt"
    }
}.apply {
    task.archiveClassifier.set("")
}

githubRelease {
    token(System.getenv("GITHUB_TOKEN"))
    owner("sndl")
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
