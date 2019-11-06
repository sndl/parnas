package sndl.parnas.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import sndl.parnas.storage.Storage
import sndl.parnas.storage.ConfigOption
import sndl.parnas.config.Config
import sndl.parnas.output.Output
import sndl.parnas.output.PrettyOutput
import sndl.parnas.output.SilentOutput
import sndl.parnas.utils.*
import java.io.File
import kotlin.system.exitProcess

// TODO: to refactor - completely separate logic from output and CLI
class Cli : CliktCommand(name = "parnas", help = "This is a command line tool that helps to manage configuration parameters in different storages") {
    private val storageIdentifier: String by argument("BACKEND|TAG")
    private val configFile: File by option("-c", "--config",
            help = "Path to config file").file().default(File("parnas.conf"))
    private val outputMethod: String by option("-o", "--output",
            help = "Select preferred output method").choice("pretty", "silent").default("pretty")
    private val byTag by option("-t", "--by-tag").flag(default = false)
    private val debug by option("--debug").flag(default = false)

    data class ConfigObjects(val storage: LinkedHashSet<Storage>, val output: Output, val config: Config)

    override fun run() {
        if (debug) {
            Configurator.setRootLevel(Level.DEBUG)
        }

        val config = Config(configFile)
        val storages = try {
            if (byTag) {
                config.getStoragesByTag(storageIdentifier)
            } else {
                linkedSetOf(config.getStorage(storageIdentifier))
            }
        } catch (e: ConfigurationException) {
            exitProcessWithMessage(1, "Configuration error: ${e.message}")
        }

        val output = when (outputMethod) {
            "pretty" -> PrettyOutput()
            "silent" -> SilentOutput()
            else -> throw ConfigurationException("\"$outputMethod\" output is not supported")
        }

        context.obj = ConfigObjects(storages, output, config)
    }
}

abstract class Command(name: String, help: String = "") : CliktCommand(name = name, help = help) {
    private val configObjects by requireObject<Cli.ConfigObjects>()
    val storages: LinkedHashSet<Storage> by lazy { configObjects.storage }
    val output: Output by lazy { configObjects.output }
    val config: Config by lazy { configObjects.config }

    fun prompt(): Boolean {
        echo("""Do you want to apply changes above?
            |Only 'yes' or 'y' will be accepted
        """.trimMargin())

        val userResponse = System.console().readLine("Enter value: ")

        return userResponse == "yes" || userResponse == "y"
    }
}

class GetParam : Command("get", "Get value by key") {
    private val key: String by argument()

    override fun run() {
        storages.forEach {
            output.printGet(key, it[key]?.value, it)
        }
    }
}

class SetParam : Command("set", "Set specific value for a specific key, use --value option if you have value with '=' sign") {
    private val key: String by argument()
    private val valueArg: String? by argument("VALUE").optional()
    /**
     * This is done as a workaround for a problem with uploading values that contain '=' sign,
     * when this is the case you have to use --value option, otherwise clikt will try to parse string before '=' as an
     * option name. Example: parnas storageName set NewParam --value="DC=org,DC=acme"
     */
    private val valueOpt: String? by option("--value")
    private val force: Boolean by option("-f", "--force",
            help = "Overwrites an existing parameter if this flag is applied").flag(default = false)

    override fun run() {
        val value = when {
            valueArg != null && valueOpt == null -> valueArg
            valueOpt != null && valueArg == null -> valueOpt
            else -> {
                System.err.println("ERROR: Either VALUE argument or --value option must be provided\n")
                System.err.println(getFormattedHelp())
                exitProcess(1)
            }
        }

        storages.forEach {
            require(it.isInitialized) {
                exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
            }

            val oldValue = it[key]?.value

            output.printSet(ConfigOption(key, value!!), oldValue, it)

            if (oldValue != null) {
                require(force || (output.interactive && prompt())) { exitProcessWithMessage(1, "WARNING: Changes were not applied") }
            }

            it[key] = value
        }
    }
}

class RmParam : Command("rm", "Remove parameter by key") {
    private val key: String by argument()
    private val force: Boolean by option("-f", "--force",
            help = "Overwrites an existing parameter if this flag is applied").flag(default = false)

    override fun run() {
        // TODO@sndl: return ConfigOption from delete method
        storages.forEach {
            require(it.isInitialized) {
                exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
            }

            val oldValue = it[key]?.value
            output.printRm(key, oldValue, it)

            if (oldValue != null) {
                require(force || (output.interactive && prompt())) { exitProcessWithMessage(1, "WARNING: Changes were not applied") }
            }

            it.delete(key)
        }
    }
}

class ListParam : Command("list", "List all parameters") {
    private val prefix by option("-p", "--prefix")

    override fun run() {
        if (prefix == null) {
            storages.forEach {
                require(it.isInitialized) {
                    exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
                }
                output.printList(it.list(), prefix, it)
            }
        } else {
            storages.forEach {
                if (!it.isInitialized) exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
                output.printList(it.listByKeyPrefix(prefix.toStringOrEmpty()), prefix, it)
            }
        }
    }
}

class DiffParam : Command(
        name = "diff",
        help = "Print difference between two storages"
) {
    private val otherStorageName: String by argument("<other-storage>")
    private val prefix by option("-p", "--prefix", help = "If set only keys by this prefix are shown")

    override fun run() {
        val otherStorage = config.getStorage(otherStorageName)
        storages.forEach {
            require(it.isInitialized) {
                exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
            }

            output.printDiff(it.diff(otherStorage, prefix.toStringOrEmpty()), prefix, it, otherStorage)
        }
    }
}

class DestroyParam : Command(
        name = "destroy",
        help = "Remove ALL parameters. IMPORTANT! This action cannot be reverted."
) {
    /**
     * For more details see documentation for boolean flags: https://ajalt.github.io/clikt/options/#boolean-flag-options
     */
    private val permitDestroy by option("--permit-destroy",
            help = "Permits/Prevents complete removal of parameters.")
            .flag("--prevent-destroy", default = false)
    private val force: Boolean by option("-f", "--force",
            help = "Overwrites all existing parameters if this flag is applied").flag(default = false)

    //TODO: rewrite printDestroy()
    override fun run() {
        storages.forEach {
            require(it.isInitialized) {
                exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
            }

            it.permitDestroy = permitDestroy

            val paramsList = it.list()
            output.printDestroy(it, paramsList)

            if(paramsList.isNotEmpty()) {
                require(force || (output.interactive && prompt())) { exitProcessWithMessage(1, "WARNING: Changes were not applied") }

                try {
                    it.destroy()
                } catch (e: IllegalArgumentException) {
                    exitProcessWithMessage(1, e.message ?: "Something went wrong")
                }
            } else {
                echo("Nothing to destroy.")
            }
        }
    }
}

class UpdateParamFrom : Command(
        name = "update-from",
        help = """Updates all parameters, that are not present or different in this storage,
        |with parameters from another storage.""".trimMargin()) {

    private val fromStorage: String by argument("<from-storage>")
    private val prefix by option("-p", "--prefix")
    private val force: Boolean by option("-f", "--force",
            help = "Overwrites all existing parameters if this flag is applied").flag(default = false)

    override fun run() {
        val otherStorage = config.getStorage(fromStorage)

        storages.forEach {
            require(it.isInitialized) {
                exitProcessWithMessage(1, "ERROR: storage \"${it.name}\" is not initialized")
            }

            val oldParams = it.list()
            val paramsToUpdate = otherStorage.notIn(it, prefix.toStringOrEmpty())
            output.printUpdateFrom(oldParams, paramsToUpdate, it, otherStorage)

            if (paramsToUpdate.isNotEmpty()) {
                require(force || (output.interactive && prompt())) { exitProcessWithMessage(1, "WARNING: Changes were not applied") }
                it.updateFrom(otherStorage, prefix.toStringOrEmpty())
            } else {
                echo("Nothing to update.")
            }
        }
    }
}

class InitializeStorage : Command(
        name = "init",
        help = "Initialize the storage, i.e. create database file"
) {
    override fun run() {
        storages.forEach {
            try {
                it.initialize()
            } catch (e: CannotInitializeStorage) {
                exitProcessWithMessage(1, "ERROR: ${e.message} (${it.name})")
            }
        }
    }
}

