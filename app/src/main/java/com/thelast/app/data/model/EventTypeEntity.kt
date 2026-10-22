package com.thelast.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_types")
data class EventTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Long,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
)
