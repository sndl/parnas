[![CircleCI](https://img.shields.io/circleci/build/github/sndl/parnas.svg)](https://circleci.com/gh/sndl/parnas)
[![TAG](https://img.shields.io/github/tag/sndl/parnas.svg)](https://github.com/sndl/parnas/tags)

# Parnas

Parnas is a tool to manage configuration parameters stored in different backends.
The tool can be extended with additional backends and outputs.

![GIPHY](http://www.giphy.com/gifs/WryP8X3pfFkMR3gacz)

## Installation

The easiest way to get and start using Parnas is to download wrapper script, that will check and get fresh versions for you.
Prerequisites for this script are: `java curl jq grep`

Commands to install the wrapper: 
```
curl -s 'https://raw.githubusercontent.com/sndl/parnas/master/install.sh' -o ./install.sh
chmod +x install.sh && ./install.sh && rm ./install.sh
```
Restart your terminal, and you should be able to use `parnas` from the command line

## Configuration

Configuration is done in INI formatted file, by default configuration is looked up in current working
directory and named `parnas.conf`. Config file path can be changed by passing `--config` option during execution

Common configuration format:
```$ini
[backend-name]
tags = tag1, tag2, tag3
type = backend-type
```

Example: 
```$ini
[local1]
    type = plain
    path = local1.properties

[local2]
    type = plain
    path = local2.properties

[local3]
    type = toml
    path = local3.toml

[keepass1]
    tags = non-prod
    type = keepass
    path = keepass1.kdbx
    password = somepassword # If not set app will try to read password from PARNAS_KEEPASS1_PASSWORD
                            # If env variable is not set, it will try to read file name from "password-from-file" and then read file contents
                            # If "password-from-file" is not set, it will try to read contents from file named .parnas_keepass1_password
                            # If file does not exist, it will prompt for password

[ssm1]
    tags = prod
    type = ssm
    region = eu-west-1
    profile = sandbox
    prefix = /sndl/parnas/test
    kms-key-id = 111a1aa1-a11a-1a1a-1aa1-1111111111a1
```

## Usage

Usage examples are based on example config above.

1. Help: `parnas --help`
1. List all parameters: `parnas local1 list`
1. List all parameters by a prefix: `parnas local1 list --prefix param`
1. Set a parameter: `parnas local1 set newParamName newParamValue`
1. Get a parameter: `parnas local1 get newParamName`
1. Remove parameter: `parnas local1 rm newParamName`
1. Diff between parameters in two backends: `parnas local1 diff ssm1`
1. Remove ALL parameters in a backend, must pass `--permit-destroy` flag for command to succeed:
`parnas local1 destroy --permit-destroy`

Actions can be done on multiple backends by tag: `parnas --by-tag non-prod set newParamName2 newParamValue2`

## Backends
At the moment supported backends are:
* Plain (properties format)
* AWS SSM
* KeePass (kdbx)
* Toml

## Outputs
At the moment supported output formats are:
* Pretty

## Known Issues & Limitations
* Only flat configs are supported for now, i.e. it is not possible to create SSM by-path configs, TOML sections work though

## Roadmap
* ~~Config validation~~ (Implemented)
* ~~Sync parameters from one backend to another~~ (Implemented)
* ~~Secure config file or think about providing credentials a different way~~ (Implemented)
* ~~Support tags and add ability to set parameters to multiple backends simultaneously~~ (Implemented)
* ~~Use GraalVM to build executable~~ (Not Implemented: due to problems with reflection)
* Add Consul backend
* Add JSON output
* Add daemon mode and thin client in order to reuse SSM or any other remote storage connections

--------
[Changelog](CHANGELOG.md)
