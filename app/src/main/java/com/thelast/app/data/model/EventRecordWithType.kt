package com.thelast.app.data.model

data class EventRecordWithType(
    val recordId: Long,
    val eventTypeId: Long,
    val occurredAt: Long,
    val note: String?,
    val eventName: String,
    val eventIcon: String,
    val eventColor: Long,
)
