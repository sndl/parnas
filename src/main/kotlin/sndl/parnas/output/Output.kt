package sndl.parnas.output

import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import sndl.parnas.storage.Storage
import sndl.parnas.storage.ConfigOption
import sndl.parnas.utils.quoted
import sndl.parnas.utils.toStringOrEmpty

sealed class Output {
    abstract val interactive: Boolean

    abstract fun printGet(key: String, value: String?, storage: Storage)

    abstract fun printSet(configOption: ConfigOption, oldValue: String?, storage: Storage)

    abstract fun printRm(key: String, value: String?, storage: Storage)

    abstract fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, storage: Storage)

    abstract fun printDestroy(storage: Storage, configOptions: LinkedHashSet<ConfigOption>? = null)

    abstract fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>,
                           prefix: String?, storage: Storage, otherStorage: Storage)

    abstract fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>,
                                 storage: Storage, otherStorage: Storage)

    abstract fun printInfo(storage: Storage)
}

class PrettyOutput : Output() {
    override val interactive = true

    private val style = TermColors()

    private fun decorateKey(input: String) = style.reset(input)

    private fun decorateValue(input: String?) = input?.quoted()

    private fun decorateStorage(input: String) = style.yellow(input)

    override fun printGet(key: String, value: String?, storage: Storage) {
        echo("${decorateStorage(storage.name)}/${decorateKey(key)} = " +
                decorateValue(value))
    }

    override fun printSet(configOption: ConfigOption, oldValue: String?, storage: Storage) {
        echo("${decorateStorage(storage.name)}/${decorateKey(configOption.key)}: " +
                "${decorateValue(oldValue)} ${style.yellow("=>")} ${decorateValue(configOption.value)}")
    }

    override fun printRm(key: String, value: String?, storage: Storage) {
        echo("${decorateStorage(storage.name)}/${decorateKey(key)}: " +
                "${decorateValue(value)} ${style.red("=>")} null")
    }

    override fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, storage: Storage) {

        echo("${decorateStorage(storage.name)}/${decorateKey("${prefix.toStringOrEmpty()}*")}:")
        printConfigOptionSet(configOptions)
    }

    override fun printDestroy(storage: Storage, configOptions: LinkedHashSet<ConfigOption>?) {
        val longestKey = configOptions?.map { it.key }?.maxBy { it.length }?.length ?: 0

        echo("${decorateStorage(storage.name)}/${decorateKey("*")}:")
        configOptions?.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.red("-")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        } ?: echo("${decorateStorage(storage.name)}/${decorateKey("*")}: ${style.red("DESTROYED")}")
    }

    override fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>,
                           prefix: String?, storage: Storage, otherStorage: Storage) {
        val storageParams = diff.second.toMutableSet()
        val otherStorageParams = diff.first.toMutableSet()
        val combined = storageParams + otherStorageParams

        val intersection = combined
                .groupBy { it.key }
                .filter { it.value.size == 2 }
                .mapValues {
                    it.value.map { configOption -> configOption.value }
                }

        val longestKey = (intersection.keys + storageParams.map { it.key } + otherStorageParams.map { it.key })
                .maxBy { it.length }?.length

        echo("${decorateStorage(storage.name)}~${decorateStorage(otherStorage.name)}/" +
                "${decorateKey("${prefix.toStringOrEmpty()}*")}:")

        if (longestKey == null) return


        intersection.forEach {
            val spacing = longestKey - it.key.length + 1

            storageParams.removeIf { configOption -> it.key == configOption.key }
            otherStorageParams.removeIf { configOption -> it.key == configOption.key }

            echo("${style.yellow("~")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value[0]) + " <> " + decorateValue(it.value[1]))
        }

        storageParams.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.red("-")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        }

        otherStorageParams.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.green("+")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        }
    }

    override fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>,
                                 storage: Storage, otherStorage: Storage) {
        echo("${decorateStorage(storage.name)}<=${decorateStorage(otherStorage.name)}:")

        updatedParams.forEach { updatedConfigOption ->
            oldParams.find { it.key == updatedConfigOption.key }?.let { oldConfigOption ->
                echo(style.yellow("~") + " ${decorateKey(updatedConfigOption.key)}: " +
                        "${decorateValue(oldConfigOption.value)} ${style.yellow("=>")} " +
                        decorateValue(updatedConfigOption.value))
            } ?: echo(style.green("+") + " ${decorateKey(updatedConfigOption.key)} = " +
                    decorateValue(updatedConfigOption.value))
        }
    }

    private fun printConfigOptionSet(configSet: LinkedHashSet<ConfigOption>) {
        val longestKey = configSet.maxBy { it.key.length }

        configSet.sortedBy { it.key }.forEach {
            val spacing = longestKey!!.key.length - it.key.length + 1
            echo("  ${decorateKey(it.key)}${" ".repeat(spacing)}= ${decorateValue(it.value)}")
        }
    }

    override fun printInfo(storage: Storage) {
        echo("${decorateStorage(storage.javaClass.simpleName.toLowerCase())}/${decorateStorage(storage.name)}")
    }
}

/**
 * Output for automation purposes, when this output is used no parameters(secrets) are displayed, therefore CI tool won't store it in logs
 */
class SilentOutput : Output() {
    override val interactive = false

    private val warnMessage = "WARNING: Silent output is used, no parameters will be displayed"

    override fun printSet(configOption: ConfigOption, oldValue: String?, storage: Storage) {
        echo(warnMessage)
        echo("Parameter set")
    }

    override fun printRm(key: String, value: String?, storage: Storage) {
        echo(warnMessage)
        echo("Parameter removed")
    }

    override fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, storage: Storage) {
        echo(warnMessage)
        echo("Displaying ${configOptions.size} parameters")
    }

    override fun printDestroy(storage: Storage, configOptions: LinkedHashSet<ConfigOption>?) {
        echo(warnMessage)
        echo("Storage was destroyed")
    }

    override fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>, prefix: String?, storage: Storage, otherStorage: Storage) {
        echo(warnMessage)
        echo("Diff does nothing with \"silent\" output mode")
    }

    override fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>, storage: Storage, otherStorage: Storage) {
        echo(warnMessage)
        echo("Updated ${updatedParams.size} parameters.")
    }

    override fun printGet(key: String, value: String?, storage: Storage) {
        echo(warnMessage)
        echo(if (value == null) {
            "Parameter doesn’t exist"
        } else {
            "Parameter exists"
        })
    }

    override fun printInfo(storage: Storage) {
        echo("${storage.javaClass.simpleName.toLowerCase()} ${(storage.name)}")
    }
}
