package sndl.parnas

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import sndl.parnas.cli.*

fun main(args: Array<String>) = Cli().versionOption("0.1.2")
        .subcommands(
                GetParam(), SetParam(), RmParam(),
                ListParam(), DiffParam(), UpdateParamFrom(),
                DestroyParam(), InitializeBackend()
        )
        .main(args)
