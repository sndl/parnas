---
name: managing-parnas
description: Manages configuration parameters using the Parnas CLI across storage backends (plain properties files, TOML, AWS SSM Parameter Store, KeePass). Use when the user asks to get, set, list, diff, sync, or destroy configuration parameters with Parnas, or when working with parnas.conf files.
---

## Invocation

```
parnas [OPTIONS] <STORAGE|TAG> COMMAND [ARGS...]
parnas install-skill
```

## Global Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--config` | `-c` | Path to config file | `parnas.conf` |
| `--output` | `-o` | `pretty` or `silent` | `pretty` |
| `--by-tag` | `-t` | Select all storages matching a tag | false |
| `--debug` | | Enable debug logging | false |
| `--prompt` | | Prompt for missing config values interactively | false |

## Config File Format (`parnas.conf`)

INI format. Each section is a storage name. `type` is required.

```ini
[local]
type = plain
path = local.properties

[secrets]
type = keepass
path = secrets.kdbx
password-from-file = ~/.parnas_secrets_password

[prod]
tags = production, aws
type = ssm
region = eu-west-1
profile = prod
prefix = /myapp/prod
kms-key-id = 111a1aa1-a11a-1a1a-1aa1-1111111111a1
separator-to-replace = .
```

## Commands

### `get KEY`
Retrieve a parameter value.
```
parnas local get db.host
```

### `set KEY [VALUE]`
Set a parameter. Prompts for confirmation if overwriting. Use `--value` for values containing `=`.
```
parnas local set db.host localhost
parnas local set db.url --value "jdbc:postgresql://host:5432/db"
parnas local set -f db.host localhost        # skip confirmation
```

### `rm KEY [KEY...]`
Remove one or more parameters. Prompts for confirmation.
```
parnas local rm db.host
parnas local rm db.host db.port db.name
parnas local rm -f db.host                   # skip confirmation
```

### `list`
List all parameters, optionally filtered by prefix.
```
parnas local list
parnas local list --prefix db
```

### `diff OTHER_STORAGE`
Show parameters that differ between two storages.
```
parnas local diff prod
parnas local diff prod --prefix db
```

### `update-from FROM_STORAGE`
Copy parameters from another storage that are missing or different. Prompts for confirmation.
```
parnas prod update-from local
parnas prod update-from local --prefix db
parnas prod update-from local -f             # skip confirmation
```

### `init`
Initialize storage (creates the file for `plain`, `toml`, `keepass`). SSM does not need initialization.
```
parnas local init
```

### `info`
Print information about a storage.
```
parnas local info
```

### `destroy`
**IRREVERSIBLE.** Deletes ALL parameters in the storage. Requires `--permit-destroy` flag.
```
parnas local destroy --permit-destroy
parnas local destroy --permit-destroy -f     # skip confirmation
```

## Storage Backends

### `plain` — Java `.properties` file
| Key | Required | Description |
|-----|----------|-------------|
| `path` | YES | Path to `.properties` file |

### `toml` — TOML file
| Key | Required | Description |
|-----|----------|-------------|
| `path` | YES | Path to `.toml` file |

Nested TOML keys are flattened with dot notation (e.g., `section.key`).

### `ssm` — AWS Systems Manager Parameter Store
| Key | Required | Description |
|-----|----------|-------------|
| `prefix` | YES | Parameter path prefix (e.g., `/myapp/prod`) |
| `region` | NO | AWS region (auto-detected from environment) |
| `profile` | NO | AWS named profile (uses credential chain if omitted) |
| `kms-key-id` | NO | KMS key ID — stores as `SecureString` if set, otherwise `String` |
| `separator-to-replace` | NO | Character in keys to replace with `/` (e.g., `.`) |

AWS credentials are resolved via SDK chain: env vars → named profile → instance profile.

### `keepass` — KeePass `.kdbx` database
| Key | Required | Description |
|-----|----------|-------------|
| `path` | YES | Path to `.kdbx` file |
| `password` | NO* | Master password |
| `password-from-file` | NO* | Path to file containing the password |

At least one of `password` or `password-from-file` must be provided, or use `--prompt`.

## Parameter Resolution (for optional config values)

When a value is not in the config file, Parnas checks in order:
1. Config file value
2. Environment variable: `PARNAS_<STORAGE>_<PARAMETER>` (uppercase)
3. File path from `<parameter>-from-file` config key
4. Hidden file: `.parnas_<storage>_<parameter>` in the current directory
5. Interactive prompt (only if `--prompt` flag is used)

Example for KeePass storage named `secrets`, password:
- Env var: `PARNAS_SECRETS_PASSWORD`
- Hidden file: `.parnas_secrets_password`

## Guardrails

**Destructive commands** (`set` overwrite, `rm`, `update-from`, `destroy`) prompt for confirmation by default. Use `-f` / `--force` to skip — only do this in automation with `--output silent`.

**`destroy` requires two safeguards:**
1. `--permit-destroy` flag must be explicitly passed
2. Confirmation prompt (unless `-f` is also passed)

Never run `destroy --permit-destroy -f` unless you are certain. It cannot be reverted.

**Tag-based operations** (`-t` / `--by-tag`) apply the command to ALL storages with that tag simultaneously. Double-check which storages match before using with destructive commands.

## Common Workflows

**Sync local config to AWS SSM:**
```
parnas prod update-from local --prefix db
```

**Compare environments:**
```
parnas staging diff prod
```

**Bootstrap a new storage:**
```
parnas local init
parnas local update-from prod -f
```

**Install this skill globally:**
```
parnas install-skill
```
