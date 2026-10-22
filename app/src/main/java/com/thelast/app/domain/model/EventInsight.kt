package com.thelast.app.domain.model

import java.time.LocalDate

data class IntervalPoint(
    val index: Int,
    val intervalMillis: Long,
)

data class EventInsight(
    val intervalPoints: List<IntervalPoint> = emptyList(),
    val trendSlopeMillis: Double = 0.0,
    val recordsByDate: Map<LocalDate, Int> = emptyMap(),
)
