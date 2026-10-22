package com.thelast.app.data.model

data class EventTypeWithLastRecord(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val sortOrder: Int,
    val lastOccurredAt: Long?,
    val previousOccurredAt: Long?,
    val recordCount: Int,
)
