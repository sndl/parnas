# Parnas

[![CircleCI](https://circleci.com/gh/sndl/parnas.svg?style=svg)](https://circleci.com/gh/sndl/parnas)

Parameter Manager is a tool to manage configuration parameters stored in different backends.
The tool can be extended with additional backends and outputs.

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
                            # If env variable is not set, it will prompt for password

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

1. Help: `java -jar parnas.jar --help`
1. List all parameters: `java -jar parnas.jar local1 list`
1. List all parameters by prefix: `java -jar parnas.jar local1 list --prefix param`
1. Set a parameter: `java -jar parnas.jar local1 set newParamName newParamValue`
1. Get a parameter: `java -jar parnas.jar local1 get newParamName`
1. Remove parameter: `java -jar parnas.jar local1 rm newParamName`
1. Diff between parameters in two backends: `java -jar parnas.jar local1 diff ssm1`
1. Remove ALL parameters in a backend, must pass `--permit-destroy` flag for command to succeed:
`java -jar parnas.jar local1 destroy --permit-destroy`

Actions can be done on multiple backends by tag: `java -jar parnas.jar --by-tag non-prod set newParamName2 newParamValue2`

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
* ~~Config validation~~
* ~~Sync parameters from one backend to another~~
* ~~Secure config file or think about providing credentials in a different way~~
* ~~Support tags and add ability to set parameters to multiple backends simultaneously~~
* ~~Use GraalVM to build executable~~
* Add Consul backend
* Add JSON output
* Add daemon mode and thin client in order to reuse SSM or any other remote storage connections
