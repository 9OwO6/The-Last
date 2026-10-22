package com.thelast.app

import android.app.Application
import androidx.room.Room
import com.thelast.app.data.database.TheLastDatabase
import com.thelast.app.data.repository.EventRepository
import com.thelast.app.data.sync.CloudCredential
import com.thelast.app.data.sync.CloudSyncEngine
import com.thelast.app.data.sync.HttpCloudTransport
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.UUID

/** Optional integration test. Uses an explicitly supplied, temporary scoped device token. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CloudLiveTest {
    @Test fun roomToRealApiAndSupabaseRoundTrip() = runBlocking {
        val configPath = System.getProperty("thelast.qa.config").orEmpty()
        assumeTrue("Set THELAST_QA_CONFIG to run the live integration test", configPath.isNotEmpty())
        val config = JSONObject(File(configPath).readText())
        val credential = CloudCredential(config.getString("base"), config.getString("token"), "", true)
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), TheLastDatabase::class.java).allowMainThreadQueries().build()
        val repo = EventRepository(db, db.eventTypeDao(), db.eventRecordDao())
        val transport = HttpCloudTransport(); val engine = CloudSyncEngine(db, transport)
        var ownedParent: String? = null
        try {
            val type = repo.createEventType("QA Android live ${System.currentTimeMillis()}", "history", 0xff4e6751, null, 0)
            val first = repo.quickRecord(type)
            ownedParent = db.syncDao().get("type:$type")!!.cloudId
            engine.sync(credential)
            val parent = db.syncDao().get("type:$type")!!.cloudId
            val now = System.currentTimeMillis(); val remoteId = UUID.randomUUID().toString()
            val data = JSONObject().put("eventTypeId", parent).put("occurredAt", now).put("createdAt", now).put("updatedAt", now)
                .put("note", "来自另一客户端").put("value", JSONObject.NULL).put("valueUnit", JSONObject.NULL)
            val op = JSONObject().put("opId", UUID.randomUUID().toString()).put("entity", "record").put("id", remoteId)
                .put("expectedVersion", 0).put("deleted", false).put("data", data)
            val response = transport.post(credential.server, "/api/thelast/sync", JSONObject().put("operations", JSONArray().put(op)), credential.token)
            assertEquals("applied", response.getJSONArray("results").getJSONObject(0).getString("status"))
            engine.sync(credential)
            assertEquals(2, db.eventRecordDao().getAllRecords().count { it.eventTypeId == type })
            val remote = db.eventRecordDao().getAllRecords().first { it.eventTypeId == type && it.note == "来自另一客户端" }
            repo.updateRecord(remote.id, remote.occurredAt, "本机冲突草稿")
            op.put("opId", UUID.randomUUID().toString()).put("expectedVersion", 1).put("data", data.put("note", "云端并发修改"))
            transport.post(credential.server, "/api/thelast/sync", JSONObject().put("operations", JSONArray().put(op)), credential.token)
            engine.sync(credential)
            assertNotNull(db.syncDao().get("record:${remote.id}")!!.conflict)
            assertEquals("本机冲突草稿", repo.getRecord(remote.id)!!.note)
            engine.discardConflicts(); engine.sync(credential)
            assertEquals("云端并发修改", repo.getRecord(remote.id)!!.note)
            repo.deleteRecord(first.recordId); engine.sync(credential)
            assertEquals(1, db.eventRecordDao().getAllRecords().count { it.eventTypeId == type })
            repo.deleteEventType(type); engine.sync(credential); engine.sync(credential)
            assertTrue(db.eventRecordDao().getAllRecords().none { it.eventTypeId == type })
            assertNull(repo.getEventType(type))
        } finally {
            val owned = db.syncDao().all().filter { it.cloudId == ownedParent || (it.kind == "record" && JSONObject(it.payload).optString("eventTypeId") == ownedParent) }
            File(File(configPath).parentFile, "android-live-ids.json").writeText(JSONArray(owned.map { it.cloudId }).toString())
            db.close()
        }
    }
}
