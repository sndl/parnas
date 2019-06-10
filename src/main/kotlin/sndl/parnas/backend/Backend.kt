package sndl.parnas.backend

import sndl.parnas.utils.toLinkedSet


/** Provides methods to work with backend storages that contain configuration parameters */
abstract class Backend(val name: String) {
    abstract val isInitialized: Boolean
    var permitDestroy = false

    abstract fun list(): LinkedHashSet<ConfigOption>

    abstract operator fun get(key: String): ConfigOption?

    abstract operator fun set(key: String, value: String): ConfigOption

    // TODO@sndl: add namespace (lookup redis key)?
    abstract fun delete(key: String)

    /** A method to initialize a backend, i.e. create a storage file */
    abstract fun initialize()

    /**
     * Removes all entries in this backend
     *
     * @throws IllegalStateException -- is thrown when trying to destroy non-destroyable backend
     */
    open fun destroy() {
        require(permitDestroy) { "Trying to destroy non-destroyable backend!" }

        list().forEach {
            delete(it.key)
        }
    }

    /**
     * @return set of config options that contain prefix string in its key
     * @param prefix an optional prefix to filter keys by
     */
    open fun listByKeyPrefix(prefix: String): LinkedHashSet<ConfigOption> {
        return list().filter { it.key.startsWith(prefix) }.toLinkedSet()
    }

    /**
     * @return set of config options that are not present in other backend in comparison to this backend
     * @param other backend to compare with
     * @param prefix an optional prefix to filter keys by
     */
    private fun notIn(other: Backend, prefix: String = ""): LinkedHashSet<ConfigOption> {
        return listByKeyPrefix(prefix).filter { it !in other.listByKeyPrefix(prefix) }.toLinkedSet()
    }

    /**
     * Method to get separated symmetric difference between this and other backend
     *
     * @param other backend to compare with
     * @param prefix an optional prefix to filter keys by
     * @return a pair of sets where:
     *        first - set of parameters not in this backend
     *        second - set of parameters not in other backend
     */
    fun diff(other: Backend, prefix: String = ""): Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>> {
        val notInThis = other.notIn(this, prefix)
        val notInOther = this.notIn(other, prefix)

        return Pair(notInThis, notInOther)
    }

    /**
     * Updates all parameters, that are not present or different in this backend, with parameters from other backend
     * @param other backend to compare with
     * @return set of updated parameters
     */
    @Suppress("unused")
    fun updateFrom(other: Backend): LinkedHashSet<ConfigOption> {
        val notInThis = other.notIn(this)

        notInThis.forEach {
            this[it.key] = it.value
        }

        return notInThis
    }
}
