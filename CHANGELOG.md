# Changelog

## 0.1.5
* Fix wrapper issue with update prompt when it could not connect to GitHub
* Require --force flag when using `update-from` command
* CLI Refactoring

## 0.1.4

* SSM Backend now supports automatic separator replacement with `separator-to-replace` option.
I.e. if key you set separator to `.`, and upload key `section.key1` then it will be automatically uploaded to SSM as `prefix/section/key1`.
This will keeping compatibility between different config backends (i.e. Toml) and will allow using SSM features like search by-path

## 0.1.3

* `list` command on SSM backend now returns all parameters with recursive call

## 0.1.2

* Add fallback to password from a file for Keepass backend. Filename can now be specified in config as `password-from-file`

## 0.1.1

* Change "cli" to "parnas" in usage help
* Add fallback to password from a file for Keepass backend
