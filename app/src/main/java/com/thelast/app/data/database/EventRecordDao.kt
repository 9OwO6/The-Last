package com.thelast.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventRecordWithType
import kotlinx.coroutines.flow.Flow

@Dao
interface EventRecordDao {
    @Query(
        """
        SELECT * FROM event_records
        WHERE eventTypeId = :eventTypeId
        ORDER BY occurredAt DESC
        """,
    )
    fun observeRecordsForEvent(eventTypeId: Long): Flow<List<EventRecordEntity>>

    @Query("SELECT * FROM event_records ORDER BY eventTypeId ASC, occurredAt DESC")
    suspend fun getAllRecords(): List<EventRecordEntity>

    @Query(
        """
        SELECT
            records.id AS recordId,
            records.eventTypeId AS eventTypeId,
            records.occurredAt AS occurredAt,
            records.note AS note,
            event_types.name AS eventName,
            event_types.icon AS eventIcon,
            event_types.color AS eventColor
        FROM event_records AS records
        INNER JOIN event_types ON event_types.id = records.eventTypeId
        WHERE event_types.isArchived = 0
        ORDER BY records.occurredAt DESC
        """,
    )
    fun observeActiveRecordsWithEventTypes(): Flow<List<EventRecordWithType>>

    @Query(
        """
        SELECT * FROM event_records
        WHERE eventTypeId = :eventTypeId
        ORDER BY occurredAt DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestRecord(eventTypeId: Long): EventRecordEntity?

    @Query("SELECT * FROM event_records WHERE id = :id LIMIT 1")
    suspend fun getRecord(id: Long): EventRecordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: EventRecordEntity): Long

    @Update
    suspend fun update(record: EventRecordEntity)

    @Delete
    suspend fun delete(record: EventRecordEntity)
}
