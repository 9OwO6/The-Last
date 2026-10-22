package com.thelast.app.data.sync

import androidx.room.withTransaction
import com.thelast.app.data.database.TheLastDatabase
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventTypeEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Durable request freezing makes retries identical even if a local edit happens in flight. */
class CloudSyncEngine(private val db: TheLastDatabase, private val transport: CloudTransport) {
    suspend fun sync(credential: CloudCredential) {
        var first = true
        while (true) {
            val batch = freezeBatch()
            if (batch.isEmpty() && !first) return
            first = false
            val ops = JSONArray().apply { batch.forEach { put(JSONObject(it.frozenOperation!!)) } }
            val snapshot = transport.post(credential.server, "/api/thelast/sync", JSONObject().put("operations", ops), credential.token)
            db.withTransaction {
                val results = snapshot.getJSONArray("results")
                for (i in 0 until results.length()) {
                    val result = results.getJSONObject(i)
                    val sent = batch.firstOrNull { JSONObject(it.frozenOperation!!).getString("opId") == result.getString("opId") } ?: error("未知同步回执")
                    val current = db.syncDao().get(sent.key) ?: continue
                    if (result.getString("status") == "applied") {
                        val version = result.getJSONObject("row").getInt("version")
                        db.syncDao().put(current.copy(version = version, dirty = current.mutationId != JSONObject(sent.frozenOperation!!).getString("opId"), frozenOperation = null, conflict = null))
                    } else db.syncDao().put(current.copy(frozenOperation = null, conflict = result.toString()))
                }
                applySnapshot(snapshot)
            }
            if (batch.isEmpty()) return
        }
    }

    private suspend fun freezeBatch(): List<SyncEntity> = db.withTransaction {
        val all = db.syncDao().all()
        val byCloudId = all.associateBy { it.cloudId }
        val ready = all.filter { row ->
            row.dirty && row.conflict == null && (row.kind == "type" ||
                byCloudId[JSONObject(row.payload).getString("eventTypeId")]?.let { !it.deleted && it.conflict == null } == true)
        }.sortedBy { if (it.kind == "type") 0 else 1 }.take(100)
        ready.map { row ->
            if (row.frozenOperation != null) row else row.copy(frozenOperation = JSONObject()
                .put("opId", row.mutationId).put("entity", row.kind).put("id", row.cloudId)
                .put("expectedVersion", row.version).put("data", JSONObject(row.payload)).put("deleted", row.deleted).toString()
            ).also { db.syncDao().put(it) }
        }
    }

    private suspend fun applySnapshot(snapshot: JSONObject) {
        val types = snapshot.getJSONArray("types")
        for (i in 0 until types.length()) {
            val row = types.getJSONObject(i); val id = row.getString("id"); val data = row.getJSONObject("data")
            val old = db.syncDao().byCloudId(id)
            if (old?.dirty == true || old?.conflict != null || old?.frozenOperation != null) continue
            val deleted = row.getBoolean("deleted")
            val event = EventTypeEntity(id = old?.localId ?: 0, name = data.getString("name"), icon = data.getString("icon"),
                color = data.getLong("color"), category = data.nullableString("category"), createdAt = data.getLong("createdAt"),
                updatedAt = data.getLong("updatedAt"), isArchived = data.getBoolean("isArchived"), sortOrder = data.getInt("sortOrder"))
            val localId = if (old != null) {
                if (deleted) {
                    val drafts = db.syncDao().all().filter { it.kind == "record" && it.dirty && JSONObject(it.payload).optString("eventTypeId") == id }
                    drafts.forEach { draft -> db.syncDao().put(draft.copy(conflict = "parent_deleted", frozenOperation = null)) }
                    val hasDrafts = drafts.isNotEmpty()
                    if (!hasDrafts) db.eventTypeDao().getEventType(old.localId)?.let { db.eventTypeDao().delete(it) }
                } else if (db.eventTypeDao().getEventType(old.localId) != null) db.eventTypeDao().update(event)
                else db.eventTypeDao().insert(event)
                old.localId
            } else if (!deleted) db.eventTypeDao().insert(event) else -1L
            db.syncDao().put(SyncEntity(old?.key ?: if (localId >= 0) "type:$localId" else "type:deleted:$id", "type", localId, id,
                version = row.getInt("version"), dirty = false, deleted = deleted,
                mutationId = UUID.randomUUID().toString(), payload = data.toString()))
        }
        val records = snapshot.getJSONArray("records")
        for (i in 0 until records.length()) {
            val row = records.getJSONObject(i); val id = row.getString("id"); val data = row.getJSONObject("data")
            val old = db.syncDao().byCloudId(id)
            if (old?.dirty == true || old?.conflict != null || old?.frozenOperation != null) continue
            val parent = db.syncDao().byCloudId(data.getString("eventTypeId")) ?: continue
            val deleted = row.getBoolean("deleted") || parent.deleted
            val record = EventRecordEntity(id = old?.localId ?: 0, eventTypeId = parent.localId, occurredAt = data.getLong("occurredAt"),
                note = data.nullableString("note"), value = if (data.isNull("value")) null else data.getDouble("value"),
                valueUnit = data.nullableString("valueUnit"), createdAt = data.getLong("createdAt"), updatedAt = data.getLong("updatedAt"))
            val localId = if (old != null) {
                if (deleted) db.eventRecordDao().getRecord(old.localId)?.let { db.eventRecordDao().delete(it) }
                else if (db.eventRecordDao().getRecord(old.localId) != null) db.eventRecordDao().update(record)
                else db.eventRecordDao().insert(record)
                old.localId
            } else if (!deleted) db.eventRecordDao().insert(record) else -1L
            db.syncDao().put(SyncEntity(old?.key ?: if (localId >= 0) "record:$localId" else "record:deleted:$id", "record", localId, id,
                version = row.getInt("version"), dirty = false, deleted = deleted,
                mutationId = UUID.randomUUID().toString(), payload = data.toString()))
        }
    }

    suspend fun discardConflicts() = db.withTransaction {
        db.syncDao().all().filter { it.conflict != null }.forEach {
            val parentDeleted = it.conflict == "parent_deleted" || runCatching { JSONObject(it.conflict!!).optString("status") == "parent_deleted" }.getOrDefault(false)
            if (parentDeleted && it.kind == "record") db.eventRecordDao().getRecord(it.localId)?.let { record -> db.eventRecordDao().delete(record) }
            db.syncDao().put(it.copy(dirty = false, conflict = null, frozenOperation = null, deleted = it.deleted || parentDeleted))
        }
    }
}
private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
