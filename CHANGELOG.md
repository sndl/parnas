# Changelog

## 0.2.1
* Fix issue with NPE when receiving response to prompts on WSL (Windows Subsystem for Linux)
* "y" is accepted as well as "yes" as a response to prompts
* Renamed backend to storage, which makes more sense and match updated documentation
* Documentation was updated along with help messages

## 0.2.0
* Implement prompts for potentially destructive commands (set, rm, update-from, destroy)
* Cover destroy, diff and updateFrom methods with unit tests
* Implement cross storage tests
* Add log4j logging support
* Improve destroy command output

## 0.1.10
* Add prefix support to `update-from` command
* CLI version is now generated from version in `build.gradle.kts`

## 0.1.9
* Add silent type output, useful for automation, no parameters(secrets) will be displayed therefore it is safe to use with CI tools
* Make profile and region setings for SSM optional when using aws environment config

## 0.1.8
* Limit amount of requests to SSM, so it won't throttle the application
* Support AWS Credentials provider chain, now it is possible to pass AWS credentials via environment variables

## 0.1.7
* Fix performance issue with diff and update-from methods

## 0.1.6
* Fix diff display [#8](https://github.com/sndl/parnas/issues/8)

## 0.1.5
* Fix wrapper issue with update prompt when it could not connect to GitHub
* Require --force flag when using `update-from` command
* CLI Refactoring

## 0.1.4

* SSM Backend now supports automatic separator replacement with `separator-to-replace` option.
I.e. if key you set separator to `.`, and upload key `section.key1` then it will be automatically uploaded to SSM as `prefix/section/key1`.
This will keeping compatibility between different config backends (i.e. Toml) and will allow using SSM features like search by-path

## 0.1.3

* `list` command on SSM storage now returns all parameters with recursive call

## 0.1.2

* Add fallback to password from a file for Keepass storage. Filename can now be specified in config as `password-from-file`

## 0.1.1

* Change "cli" to "parnas" in usage help
* Add fallback to password from a file for Keepass storage
