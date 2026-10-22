package com.thelast.app

import android.app.Application
import androidx.room.Room
import com.thelast.app.data.database.TheLastDatabase
import com.thelast.app.data.repository.EventRepository
import com.thelast.app.data.sync.CloudCredential
import com.thelast.app.data.sync.CloudSyncEngine
import com.thelast.app.data.sync.CloudTransport
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CloudSyncTest {
    private lateinit var db: TheLastDatabase
    private lateinit var repo: EventRepository
    private lateinit var server: FakeCloud
    private lateinit var sync: CloudSyncEngine
    private val credential = CloudCredential("https://example.test", "token", "", true)
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TheLastDatabase::class.java).allowMainThreadQueries().build()
        repo = EventRepository(db, db.eventTypeDao(), db.eventRecordDao())
        server = FakeCloud(); sync = CloudSyncEngine(db, server)
    }
    @After fun close() { db.close() }
    private suspend fun event() = repo.createEventType("使用手机 B", "history", 0xff4e6751, null, 0)

    @Test fun offlineAndWebRecordsMergeWithoutDuplicates() = runBlocking {
        val id = event(); repo.quickRecord(id); sync.sync(credential)
        val parent = db.syncDao().get("type:$id")!!.cloudId
        server.webRecord(parent, "来自网页")
        sync.sync(credential); sync.sync(credential)
        assertEquals(2, db.eventRecordDao().getAllRecords().size)
        assertEquals(1, db.eventTypeDao().getAllEventTypes().size)
        assertTrue(db.syncDao().all().none { it.dirty || it.conflict != null })
        // A downloaded row must reuse its identity when subsequently edited locally.
        val downloaded = db.eventRecordDao().getAllRecords().first { it.note == "来自网页" }
        repo.updateRecord(downloaded.id, downloaded.occurredAt, "网页记录在手机修改")
        sync.sync(credential)
        assertEquals(2, server.records.size)
        assertTrue(server.records.values.any { it.getJSONObject("data").optString("note") == "网页记录在手机修改" })
    }

    @Test fun lostResponseAndEditInFlightRetryFrozenRequest() = runBlocking {
        val id = event(); sync.sync(credential)
        val record = repo.quickRecord(id)
        server.dropNextResponse = true
        assertTrue(runCatching { sync.sync(credential) }.isFailure)
        val frozen = db.syncDao().get("record:${record.recordId}")!!.frozenOperation
        repo.updateRecord(record.recordId, record.occurredAt, "断网后修改")
        assertEquals(frozen, db.syncDao().get("record:${record.recordId}")!!.frozenOperation)
        sync.sync(credential)
        assertEquals(1, server.records.size)
        assertEquals("断网后修改", server.records.values.single().getJSONObject("data").getString("note"))
        assertTrue(db.syncDao().all().none { it.dirty || it.conflict != null })
    }

    @Test fun remoteConflictPreservesLocalDraftUntilExplicitResolution() = runBlocking {
        val id = event(); val record = repo.quickRecord(id); sync.sync(credential)
        val cloudId = db.syncDao().get("record:${record.recordId}")!!.cloudId
        server.records[cloudId]!!.apply { put("version", 2); getJSONObject("data").put("note", "云端修改") }
        repo.updateRecord(record.recordId, record.occurredAt, "本机修改")
        sync.sync(credential)
        assertEquals("本机修改", repo.getRecord(record.recordId)!!.note)
        assertNotNull(db.syncDao().get("record:${record.recordId}")!!.conflict)
        sync.discardConflicts(); sync.sync(credential)
        assertEquals("云端修改", repo.getRecord(record.recordId)!!.note)
    }

    @Test fun remoteParentDeleteDoesNotSilentlyDestroyOfflineDraft() = runBlocking {
        val id = event(); sync.sync(credential)
        val draft = repo.quickRecord(id)
        server.types.values.single().apply { put("deleted", true); put("version", 2) }
        sync.sync(credential)
        assertNotNull(repo.getRecord(draft.recordId))
        assertNotNull(db.syncDao().get("record:${draft.recordId}")!!.conflict)
        sync.discardConflicts(); sync.sync(credential)
        assertNull(repo.getRecord(draft.recordId))
        assertNull(repo.getEventType(id))
    }

    @Test fun localDeletionIsSyncedAndNeverResurrected() = runBlocking {
        val id = event(); val record = repo.quickRecord(id); sync.sync(credential)
        repo.deleteRecord(record.recordId); sync.sync(credential); sync.sync(credential)
        assertTrue(server.records.values.single().getBoolean("deleted"))
        assertTrue(db.eventRecordDao().getAllRecords().isEmpty())
        repo.deleteEventType(id); sync.sync(credential)
        assertTrue(server.types.values.single().getBoolean("deleted"))
    }

    @Test fun frozenUploadDoesNotClearANewerLocalEdit() = runBlocking {
        val id = event(); sync.sync(credential)
        server.beforeResponse = { repo.updateEventType(id, "上传途中改名", "history", 0xff4e6751, null) }
        repo.updateEventType(id, "第一次改名", "history", 0xff4e6751, null)
        sync.sync(credential)
        assertEquals("上传途中改名", server.types.values.single().getJSONObject("data").getString("name"))
        assertTrue(db.syncDao().all().none { it.dirty })
    }
}

private class FakeCloud : CloudTransport {
    val types = linkedMapOf<String, JSONObject>()
    val records = linkedMapOf<String, JSONObject>()
    val receipts = mutableMapOf<String, JSONObject>()
    var dropNextResponse = false
    var beforeResponse: (suspend () -> Unit)? = null
    override suspend fun post(server: String, path: String, body: JSONObject, token: String?): JSONObject {
        val results = JSONArray(); val ops = body.getJSONArray("operations")
        for (i in 0 until ops.length()) {
            val op = ops.getJSONObject(i); val opId = op.getString("opId")
            val receipt = receipts[opId]
            if (receipt != null) { results.put(receipt); continue }
            val rows = if (op.getString("entity") == "type") types else records
            val old = rows[op.getString("id")]
            val status = when {
                old?.optBoolean("deleted") == true || (old?.getInt("version") ?: 0) != op.getInt("expectedVersion") -> "conflict"
                op.getString("entity") == "record" && types[op.getJSONObject("data").getString("eventTypeId")]?.optBoolean("deleted") != false -> "parent_deleted"
                else -> "applied"
            }
            val row = if (status == "applied") JSONObject().put("id", op.getString("id")).put("version", (old?.getInt("version") ?: 0) + 1)
                .put("data", JSONObject(op.getJSONObject("data").toString())).put("deleted", op.getBoolean("deleted")).also { rows[op.getString("id")] = it } else old
            val result = JSONObject().put("opId", opId).put("status", status).put("row", row ?: JSONObject.NULL)
            receipts[opId] = JSONObject(result.toString()); results.put(result)
        }
        beforeResponse?.also { beforeResponse = null }?.invoke()
        if (dropNextResponse) { dropNextResponse = false; throw IOException("response lost") }
        return JSONObject().put("types", JSONArray(types.values.map { JSONObject(it.toString()) })).put("records", JSONArray(records.values.map { JSONObject(it.toString()) })).put("results", results)
    }
    fun webRecord(parent: String, note: String) {
        val id = UUID.randomUUID().toString(); val now = System.currentTimeMillis()
        records[id] = JSONObject().put("id", id).put("version", 1).put("deleted", false).put("data", JSONObject()
            .put("eventTypeId", parent).put("occurredAt", now).put("createdAt", now).put("updatedAt", now)
            .put("note", note).put("value", JSONObject.NULL).put("valueUnit", JSONObject.NULL))
    }
}
