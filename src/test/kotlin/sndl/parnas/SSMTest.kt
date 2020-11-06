package sndl.parnas

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import org.junit.ClassRule
import org.junit.jupiter.api.*
import sndl.parnas.storage.ConfigOption
import sndl.parnas.storage.impl.SSM
import sndl.parnas.utils.toLinkedSet
import java.lang.IllegalArgumentException
import java.time.Duration
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SSMTest {
    companion object {
        private const val containerPort = 4583
        private const val awsRegion = "eu-west-1"
    }

    @ClassRule
    private val localstack = KGenericContainer("localstack/localstack:latest")
            .withExposedPorts(containerPort)
            .withStartupTimeout(Duration.ofSeconds(180L))
            .withEnv("SERVICES", "ssm").also { it.start() }

    private val ssmClient = AWSSimpleSystemsManagementClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder
                    .EndpointConfiguration(
                            "http://${localstack.containerIpAddress}:${localstack.getMappedPort(containerPort)}",
                            awsRegion))
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("dummy", "dummy")))
            .build()
    private val storage
        get() = SSM("ssm-test", ssmClient, "/${randomUUID()}/", "1111").also {
            it["FIRST_ENTRY"] = "first-entry"
            it["SECOND_ENTRY"] = "second-entry"
            it["ENTRY_PREFIX/THIRD_ENTRY"] = "third-entry"
        }

    @Test
    fun list_storageIsNotEmpty_gotNotEmptyList() {
        Assertions.assertTrue(storage.list().size > 0)
    }

    @Test
    fun get_entryExists_gotEntry() {
        Assertions.assertEquals(ConfigOption("FIRST_ENTRY", "first-entry"), storage["FIRST_ENTRY"])
    }

    @Test
    fun list_storageIsNotEmpty_gotExpectedListOfValues() {
        val list = storage.list()
        val expectedList = setOf(
                ConfigOption("FIRST_ENTRY", "first-entry"),
                ConfigOption("SECOND_ENTRY", "second-entry"),
                ConfigOption("ENTRY_PREFIX/THIRD_ENTRY", "third-entry")
        )

        Assertions.assertEquals(expectedList, list)
    }

    @Test
    fun get_entryDoesNotExist_gotNull() {
        Assertions.assertNull(storage["THIRD_ENTRY"])
    }

    @Test
    fun set_entryDoesNotExist_entryExists() {
        val testStorage = storage

        testStorage["THIRD_ENTRY"] = "third-entry"

        val expectedEntry = ConfigOption("THIRD_ENTRY", "third-entry")
        val entry = testStorage["THIRD_ENTRY"]

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun set_entryDoesNotExist_exactlyOneEntryIsCreated() {
        val testStorage = storage
        val beforeSize = testStorage.list().size

        testStorage["THIRD_ENTRY"] = "third-entry"

        val afterSize = testStorage.list().size

        Assertions.assertTrue(afterSize - beforeSize == 1)
    }

    @Test
    fun set_entryExists_entryUpdated() {
        val testStorage = storage

        testStorage["FIRST_ENTRY"] = "updated-first-entry"

        val expectedEntry = ConfigOption("FIRST_ENTRY", "updated-first-entry")
        val entry = testStorage["FIRST_ENTRY"]

        Assertions.assertEquals(expectedEntry, entry)
    }

    @Test
    fun delete_entryExists_entryDoesNotExist() {
        val testStorage = storage
        testStorage.delete("FIRST_ENTRY")
        Assertions.assertNull(testStorage["FIRST_ENTRY"])
    }

    @Test
    fun delete_entryExists_exactlyOneEntryIsRemoved() {
        val testStorage = storage
        val sizeBefore = testStorage.list().size

        testStorage.delete("FIRST_ENTRY")

        val sizeAfter = testStorage.list().size

        Assertions.assertTrue(sizeBefore - sizeAfter == 1)
    }

    @Test
    fun destroyNonDestroyableStorage_storageExistsAndHasRecords_storageExistsAndHasRecords() {
        val testStorage = storage

        assertThrows<IllegalArgumentException> {
            testStorage.destroy()
        }
    }

    @Test
    fun destroy_storageHasRecords_storageIsEmpty() {
        val testStorage = storage

        testStorage.permitDestroy = true
        testStorage.destroy()

        Assertions.assertTrue(testStorage.list().size == 0)
    }

    @Test
    fun updateFrom_firstStorageHasRecords_firstStoragesHasAllItsRecordsAndRecordsFromSecondStorage() {
        val storage1 = storage
        val storage2 = storage.also {
            it["additional_record1"] = "val1"
            it["additional_record2"] = "val2"
            it["additional_record3"] = "val3"
        }

        val storage1BeforeUpdateList = storage1.list()

        storage1.updateFrom(storage2)
        storage1.diff(storage2)

        val expectedResult = storage1BeforeUpdateList + storage2.list()

        Assertions.assertTrue(storage1.list() == expectedResult)
    }

    @Test
    fun diff_bothStoragesHaveEntries_listOfDifferentEntriesReturned() {
        val storage1 = storage.also {
            it["commonRecord"] = "commonRecord"
            it["uniqueRecord1"] = "uniqueRecord1"
        }
        val storage2 = storage.also {
            it["commonRecord"] = "commonRecord"
            it["uniqueRecord2"] = "uniqueRecord2"
        }

        val expectedResult = Pair(
                listOf(ConfigOption("uniqueRecord2", "uniqueRecord2")).toLinkedSet(),
                listOf(ConfigOption("uniqueRecord1", "uniqueRecord1")).toLinkedSet())

        Assertions.assertTrue(storage1.diff(storage2) == expectedResult)
    }
}
