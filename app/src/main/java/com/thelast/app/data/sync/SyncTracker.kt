package com.thelast.app.data.sync

import com.thelast.app.data.database.TheLastDatabase
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventTypeEntity
import org.json.JSONObject
import java.util.UUID

fun EventTypeEntity.cloudPayload() = JSONObject().apply {
    put("name", name); put("icon", icon); put("color", color and 0xffffffffL)
    put("category", category ?: JSONObject.NULL); put("isArchived", isArchived)
    put("sortOrder", sortOrder); put("createdAt", createdAt); put("updatedAt", updatedAt)
}
fun EventRecordEntity.cloudPayload(parentId: String) = JSONObject().apply {
    put("eventTypeId", parentId); put("occurredAt", occurredAt); put("note", note ?: JSONObject.NULL)
    put("value", value ?: JSONObject.NULL); put("valueUnit", valueUnit ?: JSONObject.NULL)
    put("createdAt", createdAt); put("updatedAt", updatedAt)
}

class SyncTracker(private val db: TheLastDatabase) {
    suspend fun type(event: EventTypeEntity, deleted: Boolean = false) {
        mark("type", event.id, event.cloudPayload(), deleted)
    }
    suspend fun record(record: EventRecordEntity, deleted: Boolean = false) {
        val parent = db.syncDao().get("type:${record.eventTypeId}") ?: run {
            val event = db.eventTypeDao().getEventType(record.eventTypeId) ?: error("事项不存在")
            type(event); db.syncDao().get("type:${event.id}")!!
        }
        mark("record", record.id, record.cloudPayload(parent.cloudId), deleted)
    }
    private suspend fun mark(kind: String, localId: Long, payload: JSONObject, deleted: Boolean) {
        val key = "$kind:$localId"
        val old = db.syncDao().get(key)
        db.syncDao().put(SyncEntity(key, kind, localId, old?.cloudId ?: UUID.randomUUID().toString(),
            version = old?.version ?: 0, dirty = true, deleted = deleted,
            mutationId = UUID.randomUUID().toString(), payload = payload.toString(),
            frozenOperation = old?.frozenOperation, conflict = old?.conflict))
    }
    suspend fun deletedType(event: EventTypeEntity) {
        type(event, true)
        db.eventRecordDao().getAllRecords().filter { it.eventTypeId == event.id }.forEach { record ->
            val old = db.syncDao().get("record:${record.id}") ?: return@forEach
            db.syncDao().put(old.copy(deleted = true, dirty = false, frozenOperation = null, conflict = null))
        }
    }
}
