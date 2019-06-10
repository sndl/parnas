package sndl.parnas

import com.github.ajalt.clikt.core.subcommands
import sndl.parnas.cli.*

fun main(args: Array<String>) = Cli()
        .subcommands(
                GetParam(), SetParam(), RmParam(),
                ListParam(), DiffParam(), UpdateParamFrom(),
                DestroyParam(), InitializeBackend()
        )
        .main(args)
