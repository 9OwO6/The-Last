package com.thelast.app.data.repository

import androidx.room.withTransaction
import com.thelast.app.data.database.TheLastDatabase
import com.thelast.app.data.database.EventRecordDao
import com.thelast.app.data.database.EventTypeDao
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventRecordWithType
import com.thelast.app.data.model.EventTypeEntity
import com.thelast.app.data.model.EventTypeWithLastRecord
import com.thelast.app.domain.model.BackupImportDraft
import kotlinx.coroutines.flow.Flow
import com.thelast.app.data.sync.SyncTracker

data class BackupSnapshot(
    val eventTypes: List<EventTypeEntity>,
    val eventRecords: List<EventRecordEntity>,
    val exportedAt: Long,
)

data class QuickRecordResult(
    val recordId: Long,
    val occurredAt: Long,
    val previousOccurredAt: Long?,
)

class EventRepository(
    private val database: TheLastDatabase,
    private val eventTypeDao: EventTypeDao,
    private val eventRecordDao: EventRecordDao,
    private val onChanged: () -> Unit = {},
) {
    private val tracker = SyncTracker(database)
    private suspend fun <T> mutate(block: suspend () -> T): T {
        val result = database.withTransaction { block() }
        onChanged()
        return result
    }
    fun observeActiveEventTypes(): Flow<List<EventTypeWithLastRecord>> {
        return eventTypeDao.observeActiveEventTypesWithLastRecord()
    }

    fun observeEventType(id: Long): Flow<EventTypeEntity?> {
        return eventTypeDao.observeEventType(id)
    }

    suspend fun getEventType(id: Long): EventTypeEntity? {
        return eventTypeDao.getEventType(id)
    }

    fun observeRecordsForEvent(eventTypeId: Long): Flow<List<EventRecordEntity>> {
        return eventRecordDao.observeRecordsForEvent(eventTypeId)
    }

    fun observeActiveRecordsWithEventTypes(): Flow<List<EventRecordWithType>> {
        return eventRecordDao.observeActiveRecordsWithEventTypes()
    }

    suspend fun getBackupSnapshot(): BackupSnapshot {
        return BackupSnapshot(
            eventTypes = eventTypeDao.getAllEventTypes(),
            eventRecords = eventRecordDao.getAllRecords(),
            exportedAt = System.currentTimeMillis(),
        )
    }

    suspend fun getRecord(id: Long): EventRecordEntity? {
        return eventRecordDao.getRecord(id)
    }

    suspend fun createEventType(
        name: String,
        icon: String,
        color: Long,
        category: String?,
        sortOrder: Int,
    ): Long {
        val now = System.currentTimeMillis()
        return mutate {
          val id = eventTypeDao.insert(
            EventTypeEntity(
                name = name.trim(),
                icon = icon,
                color = color,
                category = category?.trim()?.ifBlank { null },
                createdAt = now,
                updatedAt = now,
                sortOrder = sortOrder,
            ),
          )
          tracker.type(eventTypeDao.getEventType(id)!!)
          id
        }
    }

    suspend fun updateEventType(
        id: Long,
        name: String,
        icon: String,
        color: Long,
        category: String?,
    ) {
        mutate {
        val existing = eventTypeDao.getEventType(id) ?: return@mutate
        eventTypeDao.update(
            existing.copy(
                name = name.trim(),
                icon = icon,
                color = color,
                category = category?.trim()?.ifBlank { null },
                updatedAt = System.currentTimeMillis(),
            ),
        )
        tracker.type(eventTypeDao.getEventType(id)!!)
        }
    }

    suspend fun archiveEventType(id: Long) {
        mutate {
            eventTypeDao.archive(id = id, updatedAt = System.currentTimeMillis())
            eventTypeDao.getEventType(id)?.let { tracker.type(it) }
        }
    }

    suspend fun deleteEventType(id: Long): Boolean {
        return mutate {
            val existing = eventTypeDao.getEventType(id) ?: return@mutate false
            tracker.deletedType(existing)
            eventTypeDao.delete(existing)
            true
        }
    }

    suspend fun quickRecord(eventTypeId: Long): QuickRecordResult {
      return mutate {
        val previousRecord = eventRecordDao.getLatestRecord(eventTypeId)
        val now = System.currentTimeMillis()
        val recordId = eventRecordDao.insert(
            EventRecordEntity(
                eventTypeId = eventTypeId,
                occurredAt = now,
                note = null,
                value = null,
                valueUnit = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
        tracker.record(eventRecordDao.getRecord(recordId)!!)
        QuickRecordResult(
            recordId = recordId,
            occurredAt = now,
            previousOccurredAt = previousRecord?.occurredAt,
        )
      }
    }

    suspend fun addRecord(
        eventTypeId: Long,
        occurredAt: Long,
        note: String?,
    ): Long {
        val now = System.currentTimeMillis()
        return mutate {
          val id = eventRecordDao.insert(
            EventRecordEntity(
                eventTypeId = eventTypeId,
                occurredAt = occurredAt,
                note = note?.trim()?.ifBlank { null },
                value = null,
                valueUnit = null,
                createdAt = now,
                updatedAt = now,
            ),
          )
          tracker.record(eventRecordDao.getRecord(id)!!)
          id
        }
    }

    suspend fun updateRecord(
        id: Long,
        occurredAt: Long,
        note: String?,
    ) {
        mutate {
        val existing = eventRecordDao.getRecord(id) ?: return@mutate
        eventRecordDao.update(
            existing.copy(
                occurredAt = occurredAt,
                note = note?.trim()?.ifBlank { null },
                updatedAt = System.currentTimeMillis(),
            ),
        )
        tracker.record(eventRecordDao.getRecord(id)!!)
        }
    }

    suspend fun deleteRecord(id: Long) {
        mutate {
            val existing = eventRecordDao.getRecord(id) ?: return@mutate
            tracker.record(existing, deleted = true)
            eventRecordDao.delete(existing)
        }
    }

    suspend fun deleteRecordIfExists(id: Long): Boolean {
        return mutate {
            val existing = eventRecordDao.getRecord(id) ?: return@mutate false
            tracker.record(existing, deleted = true)
            eventRecordDao.delete(existing)
            true
        }
    }

    suspend fun importBackup(draft: BackupImportDraft) {
        mutate {
            val importedEventIds = mutableMapOf<Long, Long>()
            draft.eventTypes.forEach { eventType ->
                val newId = eventTypeDao.insert(
                    EventTypeEntity(
                        name = eventType.name,
                        icon = eventType.icon,
                        color = eventType.color,
                        category = eventType.category,
                        createdAt = eventType.createdAt,
                        updatedAt = eventType.updatedAt,
                        isArchived = eventType.isArchived,
                        sortOrder = eventType.sortOrder,
                    ),
                )
                importedEventIds[eventType.oldId] = newId
                tracker.type(eventTypeDao.getEventType(newId)!!)
            }

            draft.eventRecords.forEach { record ->
                val newEventTypeId = importedEventIds[record.oldEventTypeId]
                    ?: error("Missing imported event type")
                val newId = eventRecordDao.insert(
                    EventRecordEntity(
                        eventTypeId = newEventTypeId,
                        occurredAt = record.occurredAt,
                        note = record.note,
                        value = record.value,
                        valueUnit = record.valueUnit,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                    ),
                )
                tracker.record(eventRecordDao.getRecord(newId)!!)
            }
        }
    }
}
