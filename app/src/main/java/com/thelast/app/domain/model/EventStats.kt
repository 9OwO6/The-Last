package com.thelast.app.domain.model

data class EventStats(
    val recordCount: Int = 0,
    val latestOccurredAt: Long? = null,
    val averageIntervalMillis: Long? = null,
    val shortestIntervalMillis: Long? = null,
    val longestIntervalMillis: Long? = null,
)
