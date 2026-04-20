# Parnas — Claude Code Guide

## Project Overview

**Parnas** (PARameter Naming And Storing) is a Kotlin CLI tool for managing configuration parameters across multiple storage backends. It uses [Clikt](https://ajalt.github.io/clikt/) for CLI parsing.

## Tech Stack

- **Language**: Kotlin 2.0, JVM 17
- **Build**: Gradle with `build.gradle.kts`
- **Packaging**: Shadow JAR via `tanvd.kosogor`
- **Linting**: Detekt (`buildScripts/detekt/detekt.yml`)
- **Testing**: JUnit 5 + Testcontainers (LocalStack for SSM tests)

## Project Structure

```
src/main/kotlin/sndl/parnas/
├── Main.kt                  # Entry point, logging setup
├── cli/Cli.kt               # All CLI commands (Clikt)
├── config/                  # INI config parsing (parnas.conf)
├── output/                  # Output implementations (PrettyOutput, SilentOutput)
├── storage/
│   ├── Storage.kt           # Abstract base class
│   ├── ConfigOption.kt      # Key-value data class
│   └── impl/
│       ├── Plain.kt         # .properties files
│       ├── Toml.kt          # TOML files
│       ├── SSM.kt           # AWS Parameter Store
│       └── keepass/         # KeePass (.kdbx) via KeePassJava2-jackson
└── utils/
```

## Common Commands

```bash
# Build fat JAR
./gradlew shadowJar

# Run tests
./gradlew test

# Run linter
./gradlew detekt

# Full build (compiles + tests + detekt)
./gradlew build
```

## Key Conventions

- Storage backends extend `abstract class Storage` and implement `list()`, `get()`, `set()`, `delete()`, `initialize()`
- CLI commands extend `abstract class Command` (which extends `CliktCommand`)
- Config is read from `parnas.conf` (INI format) in the current directory by default
- `ConfigOption` is the core data class: `data class ConfigOption(val key: String, val value: String)`
- `permitDestroy` flag on `Storage` must be set to `true` before calling `destroy()`

## Adding a New Storage Backend

1. Create a new class in `src/main/kotlin/sndl/parnas/storage/impl/` extending `Storage`
2. Register the new type in `src/main/kotlin/sndl/parnas/config/Config.kt`
3. Add tests in `src/test/kotlin/sndl/parnas/`

## Testing Notes

- SSM tests require Docker (Testcontainers + LocalStack)
- Test utilities are in `TestUtils.kt` and `TestContainersFactory.kt`
- Run SSM tests with a running Docker daemon
