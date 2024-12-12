package sndl.parnas.storage.impl.keepass

import org.linguafranca.pwdb.kdbx.KdbxCreds
import org.linguafranca.pwdb.kdbx.dom.DomDatabaseWrapper
import org.linguafranca.pwdb.kdbx.dom.DomEntryWrapper
import org.linguafranca.pwdb.kdbx.dom.DomGroupWrapper
import java.io.File

class KeepassClient(private val databaseFile: File, private val masterPassword: String) {
    companion object {
        private const val ROOT_GROUP_NAME = "Root"
    }

    private lateinit var database: DomDatabaseWrapper

    init {
        val credentials = KdbxCreds(masterPassword.toByteArray())
        if (databaseFile.exists()) database = DomDatabaseWrapper.load(credentials, databaseFile.inputStream())
    }

    data class KeepassEntry(val title: String, val password: String)

    private fun saveDB() {
        database.save(KdbxCreds(masterPassword.toByteArray()), databaseFile.outputStream())
    }

    private fun findEntryByTitle(title: String): DomEntryWrapper? {
        return database.findEntries { it.title == title }.firstOrNull()
    }

    fun createDb(): DomDatabaseWrapper {
        databaseFile.createNewFile()
        database = DomDatabaseWrapper().also {
            it.newGroup(ROOT_GROUP_NAME)
        }
        saveDB()
        return database
    }

    fun findByTitle(title: String): KeepassEntry? {
        return findEntryByTitle(title)?.let { KeepassEntry(it.title, it.password) }
    }

    fun list(): List<KeepassEntry> {
        return database.rootGroup.entries.map { KeepassEntry(it.title, it.password) }
    }

    fun deleteEntry(title: String): Unit = updateDb {
        findEntryByTitle(title)?.let { removeEntry(it) }
    }

    fun setEntry(title: String, value: String) {
        deleteEntry(title)

        updateDb {
            val newEntry = database.newEntry(title).apply {
                password = value
            }
            addEntry(newEntry)
        }
    }

    private fun updateDb(body: DomGroupWrapper.() -> Unit) {
        database.rootGroup.body()
        saveDB()
    }
}
