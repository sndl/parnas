package sndl.parnas

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory
import sndl.parnas.cli.*

fun main(args: Array<String>) {
    fun prepareLogging() {
        val builder = ConfigurationBuilderFactory.newConfigurationBuilder()
        val layout = builder.newLayout("PatternLayout").apply {
            addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable")
        }
        val console = builder.newAppender("stdout", "Console").also {
            it.add(layout)
            builder.add(it)
        }
        val rootLogger = builder.newRootLogger(Level.INFO).also {
            it.add(builder.newAppenderRef(console.name))
            builder.add(it)
        }

        Configurator.initialize(builder.build())
    }

    prepareLogging()

    return Cli().versionOption({}.javaClass.getResource("/version.txt").readText())
            .subcommands(
                    GetParam(), SetParam(), RmParam(),
                    ListParam(), DiffParam(), UpdateParamFrom(),
                    DestroyParam(), InitializeBackend()
            )
            .main(args)
}
