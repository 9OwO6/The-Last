package com.thelast.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_records",
    foreignKeys = [
        ForeignKey(
            entity = EventTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventTypeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("eventTypeId"),
        Index("occurredAt"),
    ],
)
data class EventRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventTypeId: Long,
    val occurredAt: Long,
    val note: String?,
    val value: Double?,
    val valueUnit: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
