package sndl.parnas.storage

import sndl.parnas.utils.toLinkedSet


/** Provides methods to work with storage that contain configuration parameters */
abstract class Storage(val name: String) {
    abstract val isInitialized: Boolean
    var permitDestroy = false

    abstract fun list(): LinkedHashSet<ConfigOption>

    abstract operator fun get(key: String): ConfigOption?

    abstract operator fun set(key: String, value: String): ConfigOption

    // TODO@sndl: add namespace (lookup redis key)?
    abstract fun delete(key: String)

    /** A method to initialize a storage, i.e. create a storage file */
    abstract fun initialize()

    /**
     * Removes all entries in this storage
     *
     * @throws IllegalStateException -- is thrown when trying to destroy non-destroyable storage
     */
    open fun destroy() {
        require(permitDestroy) { "Trying to destroy a protected storage!" }

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
     * @return set of config options that are not present in other storage in comparison to this storage
     * @param other storage to compare with
     * @param prefix an optional prefix to filter keys by
     */
    fun notIn(other: Storage, prefix: String = ""): LinkedHashSet<ConfigOption> {
        val otherList = other.listByKeyPrefix(prefix)

        return listByKeyPrefix(prefix).filter { it !in otherList }.toLinkedSet()
    }

    /**
     * Method to get separated symmetric difference between this and other storage
     *
     * @param other storage to compare with
     * @param prefix an optional prefix to filter keys by
     * @return a pair of sets where:
     *        first - set of parameters not in this storage
     *        second - set of parameters not in other storage
     */
    fun diff(other: Storage, prefix: String = ""): Pair<LinkedHashSet<ConfigOption>, LinkedHashSet<ConfigOption>> {
        val notInThis = other.notIn(this, prefix)
        val notInOther = this.notIn(other, prefix)

        return Pair(notInThis, notInOther)
    }

    /**
     * Updates all parameters, that are not present or different in this storage, with parameters from other storage
     * @param other storage to compare with
     * @return set of updated parameters
     */
    @Suppress("unused")
    fun updateFrom(other: Storage, prefix: String = ""): LinkedHashSet<ConfigOption> {
        val notInThis = other.notIn(this, prefix)

        notInThis.forEach {
            this[it.key] = it.value
        }

        return notInThis
    }
}
