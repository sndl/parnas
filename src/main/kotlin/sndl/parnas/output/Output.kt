package sndl.parnas.output

import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.quoted
import sndl.parnas.utils.toStringOrEmpty

sealed class Output {
    abstract val interactive: Boolean

    abstract fun printGet(key: String, value: String?, backend: Backend)

    abstract fun printSet(configOption: ConfigOption, oldValue: String?, backend: Backend)

    abstract fun printRm(key: String, value: String?, backend: Backend)

    abstract fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, backend: Backend)

    abstract fun printDestroy(backend: Backend, configOptions: LinkedHashSet<ConfigOption>? = null)

    abstract fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>,
                           prefix: String?, backend: Backend, otherBackend: Backend)

    abstract fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>,
                                 backend: Backend, otherBackend: Backend)
}

class PrettyOutput : Output() {
    override val interactive = true

    private val style = TermColors()

    private fun decorateKey(input: String) = style.reset(input)

    private fun decorateValue(input: String?) = input?.quoted()

    private fun decorateBackend(input: String) = style.yellow(input)

    override fun printGet(key: String, value: String?, backend: Backend) {
        echo("${decorateBackend(backend.name)}/${decorateKey(key)} = " +
                decorateValue(value))
    }

    override fun printSet(configOption: ConfigOption, oldValue: String?, backend: Backend) {
        echo("${decorateBackend(backend.name)}/${decorateKey(configOption.key)}: " +
                "${decorateValue(oldValue)} ${style.yellow("=>")} ${decorateValue(configOption.value)}")
    }

    override fun printRm(key: String, value: String?, backend: Backend) {
        echo("${decorateBackend(backend.name)}/${decorateKey(key)}: " +
                "${decorateValue(value)} ${style.red("=>")} null")
    }

    override fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, backend: Backend) {

        echo("${decorateBackend(backend.name)}/${decorateKey("${prefix.toStringOrEmpty()}*")}:")
        printConfigOptionSet(configOptions)
    }

    override fun printDestroy(backend: Backend, configOptions: LinkedHashSet<ConfigOption>?) {
        val longestKey = configOptions?.map { it.key }?.maxBy { it.length }?.length ?: 0

        configOptions?.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.red("-")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        } ?: echo("${decorateBackend(backend.name)}/${decorateKey("*")}: ${style.red("DESTROYED")}")
    }

    override fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>,
                           prefix: String?, backend: Backend, otherBackend: Backend) {
        val backendParams = diff.second.toMutableSet()
        val otherBackendParams = diff.first.toMutableSet()
        val combined = backendParams + otherBackendParams

        val intersection = combined
                .groupBy { it.key }
                .filter { it.value.size == 2 }
                .mapValues {
                    it.value.map { configOption -> configOption.value }
                }

        val longestKey = (intersection.keys + backendParams.map { it.key } + otherBackendParams.map { it.key })
                .maxBy { it.length }?.length

        echo("${decorateBackend(backend.name)}~${decorateBackend(otherBackend.name)}/" +
                "${decorateKey("${prefix.toStringOrEmpty()}*")}:")

        if (longestKey == null) return


        intersection.forEach {
            val spacing = longestKey - it.key.length + 1

            backendParams.removeIf { configOption -> it.key == configOption.key }
            otherBackendParams.removeIf { configOption -> it.key == configOption.key }

            echo("${style.yellow("~")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value[0]) + " <> " + decorateValue(it.value[1]))
        }

        backendParams.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.red("-")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        }

        otherBackendParams.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.green("+")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        }
    }

    override fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>,
                                 backend: Backend, otherBackend: Backend) {
        echo("${decorateBackend(backend.name)}<=${decorateBackend(otherBackend.name)}:")

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
}

/**
 * Output for automation purposes, when this output is used no parameters(secrets) are displayed, therefore CI tool won't store it in logs
 */
class SilentOutput : Output() {
    override val interactive = false

    private val warnMessage = "WARNING: Silent output is used, no parameters will be displayed"

    override fun printSet(configOption: ConfigOption, oldValue: String?, backend: Backend) {
        echo(warnMessage)
        echo("Parameter set")
    }

    override fun printRm(key: String, value: String?, backend: Backend) {
        echo(warnMessage)
        echo("Parameter removed")
    }

    override fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, backend: Backend) {
        echo(warnMessage)
        echo("There are ${configOptions.size} parameters")
    }

    override fun printDestroy(backend: Backend, configOptions: LinkedHashSet<ConfigOption>?) {
        echo(warnMessage)
        echo("Backend was destroyed")
    }

    override fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>, prefix: String?, backend: Backend, otherBackend: Backend) {
        echo(warnMessage)
        echo("Diff does nothing with \"silent\" output mode")
    }

    override fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>, backend: Backend, otherBackend: Backend) {
        echo(warnMessage)
        echo("Updated ${updatedParams.size} parameters.")
    }

    override fun printGet(key: String, value: String?, backend: Backend) {
        echo(warnMessage)
        echo(if (value == null) {
            "The parameter doesn't exist"
        } else {
            "The parameter exists"
        })
    }
}
