package com.thelast.app.domain.model

data class HomeCalendarRecord(
    val recordId: Long,
    val eventTypeId: Long,
    val eventName: String,
    val eventIcon: String,
    val eventColor: Long,
    val occurredAt: Long,
    val note: String?,
)
