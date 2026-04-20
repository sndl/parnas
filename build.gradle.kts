import com.github.breadmoirai.githubreleaseplugin.GithubReleaseTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import tanvd.kosogor.proxy.shadowJar

group = "sndl.parnas"
version = "0.2.10-SNAPSHOT"
description = "PARameter Naming And Storing"

plugins {
    id("tanvd.kosogor") version "1.0.23"
    kotlin("jvm") version "2.3.20" apply true
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("com.github.breadmoirai.github-release") version "2.3.7"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.linguafranca.pwdb", "KeePassJava2-jackson", "2.2.4")
    implementation("software.amazon.awssdk", "ssm", "2.42.36")
    implementation("com.electronwill.night-config", "toml", "3.8.4")
    implementation("com.electronwill.night-config", "core", "3.8.4")

    implementation("com.github.ajalt.clikt", "clikt", "3.5.4")
    implementation("com.github.ajalt", "mordant", "1.2.1")

    implementation("org.ini4j", "ini4j", "0.5.4")
    implementation("org.apache.logging.log4j", "log4j-core", "2.25.4")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.25.4")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.14.3")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.14.3")
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher", "1.14.3")
    testImplementation("org.testcontainers", "testcontainers", "1.21.4")
    testImplementation("org.testcontainers", "localstack", "1.21.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("25"))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.AMAZON)
    }
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
        // https://jakewharton.com/kotlins-jdk-release-compatibility-flag/
        // https://youtrack.jetbrains.com/issue/KT-49746/Support-Xjdk-release-in-gradle-toolchain#focus=Comments-27-8935065.0-0
        freeCompilerArgs.addAll("-Xjdk-release=25")
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

    // Force testcontainers to use IPv4 loopback — on macOS, `localhost` may resolve to ::1 (IPv6)
    // but Docker Desktop only binds mapped ports on 127.0.0.1 (IPv4), causing NoRouteToHostException.
    environment("TESTCONTAINERS_HOST_OVERRIDE", "127.0.0.1")

    testLogging {
        events("passed", "skipped", "failed")
    }
}
