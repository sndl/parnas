package sndl.parnas.storage.impl.keepass

import de.slackspace.openkeepass.KeePassDatabase
import de.slackspace.openkeepass.domain.*
import de.slackspace.openkeepass.domain.zipper.GroupZipper
import java.io.File
import java.io.FileOutputStream


class KeepassClient(private val databaseFile: File, private val masterPassword: String) {
    companion object {
        private const val rootGroupName = "Root"
    }

    private lateinit var database: KeePassFile

    init {
        if (databaseFile.exists()) database = KeePassDatabase.getInstance(databaseFile).openDatabase(masterPassword)
    }

    private val rootGroup
        get() = database.getGroupByName(rootGroupName)

    data class KeepassEntry(val password: String)

    fun createDb(): KeePassFile {
        database = KeePassFileBuilder(databaseFile.toString())
                .addTopGroups(GroupBuilder()
                        .addGroup(GroupBuilder().name(rootGroupName).build())
                        .build()
                ).build()
        KeePassDatabase.write(database, masterPassword, FileOutputStream(databaseFile))

        return database
    }

    fun findByTitle(title: String): KeepassEntry? {
        return database.getEntryByTitle(title)?.let { KeepassEntry(it.password) }
    }

    fun list(): List<Entry>? {
        return rootGroup.entries
    }

    fun deleteEntry(title: String) = updateDb { removeEntry(database.getEntryByTitle(title)) }

    fun setEntry(title: String, value: String) {
        deleteEntry(title)

        return updateDb {
            addEntry(EntryBuilder().title(title).password(value).build())
        }
    }

    private fun <T> updateDb(body: GroupBuilder.() -> T): T {
        var result: T
        val group = GroupBuilder(rootGroup).apply { result = body() }.build()
        val updatedDatabase = GroupZipper(database).replace(group).close()

        databaseFile.delete()
        KeePassDatabase.write(updatedDatabase, masterPassword, FileOutputStream(databaseFile))
        database = KeePassDatabase.getInstance(databaseFile).openDatabase(masterPassword)

        return result
    }
}
