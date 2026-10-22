package com.thelast.app.domain.model

data class EventSummary(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val lastOccurredAt: Long?,
    val previousOccurredAt: Long?,
    val recordCount: Int,
)
