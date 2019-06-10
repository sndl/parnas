package sndl.parnas.output

import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import sndl.parnas.backend.Backend
import sndl.parnas.backend.ConfigOption
import sndl.parnas.utils.quoted
import sndl.parnas.utils.toStringOrEmpty

sealed class Output {
    abstract fun printGet(key: String, value: String?, backend: Backend)

    abstract fun printSet(configOption: ConfigOption, oldValue: String?, backend: Backend)

    abstract fun printRm(key: String, value: String?, backend: Backend)

    abstract fun printList(configOptions: LinkedHashSet<ConfigOption>, prefix: String?, backend: Backend)

    abstract fun printDestroy(backend: Backend)

    abstract fun printDiff(diff: Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>>,
                           prefix: String?, backend: Backend, otherBackend: Backend)

    abstract fun printUpdateFrom(oldParams: LinkedHashSet<ConfigOption>, updatedParams: LinkedHashSet<ConfigOption>,
                                 backend: Backend, otherBackend: Backend)
}

class PrettyOutput : Output() {
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

    override fun printDestroy(backend: Backend) {
        echo("${decorateBackend(backend.name)}/${decorateKey("*")}: ${style.red("DESTROYED")}")
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

            echo("${style.green("+")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
                    decorateValue(it.value))
        }

        otherBackendParams.forEach {
            val spacing = longestKey - it.key.length + 1

            echo("${style.red("-")} ${decorateKey(it.key)}${" ".repeat(spacing)}= " +
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
