package com.thelast.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thelast.app.data.model.EventTypeEntity
import com.thelast.app.data.model.EventTypeWithLastRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {
    @Query(
        """
        SELECT
            event_types.id,
            event_types.name,
            event_types.icon,
            event_types.color,
            event_types.category,
            event_types.createdAt,
            event_types.updatedAt,
            event_types.isArchived,
            event_types.sortOrder,
            (
                SELECT latest.occurredAt
                FROM event_records AS latest
                WHERE latest.eventTypeId = event_types.id
                ORDER BY latest.occurredAt DESC
                LIMIT 1
            ) AS lastOccurredAt,
            (
                SELECT previous.occurredAt
                FROM event_records AS previous
                WHERE previous.eventTypeId = event_types.id
                ORDER BY previous.occurredAt DESC
                LIMIT 1 OFFSET 1
            ) AS previousOccurredAt,
            (
                SELECT CAST(COUNT(records.id) AS INTEGER)
                FROM event_records AS records
                WHERE records.eventTypeId = event_types.id
            ) AS recordCount
        FROM event_types
        WHERE event_types.isArchived = 0
        ORDER BY event_types.sortOrder ASC, event_types.createdAt ASC
        """,
    )
    fun observeActiveEventTypesWithLastRecord(): Flow<List<EventTypeWithLastRecord>>

    @Query("SELECT * FROM event_types ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllEventTypes(): List<EventTypeEntity>

    @Query("SELECT * FROM event_types WHERE id = :id LIMIT 1")
    fun observeEventType(id: Long): Flow<EventTypeEntity?>

    @Query("SELECT * FROM event_types WHERE id = :id LIMIT 1")
    suspend fun getEventType(id: Long): EventTypeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(eventType: EventTypeEntity): Long

    @Update
    suspend fun update(eventType: EventTypeEntity)

    @Delete
    suspend fun delete(eventType: EventTypeEntity)

    @Query("UPDATE event_types SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archive(id: Long, updatedAt: Long)
}
