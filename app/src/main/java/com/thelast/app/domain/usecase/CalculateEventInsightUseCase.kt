package com.thelast.app.domain.usecase

import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.domain.model.EventInsight
import com.thelast.app.domain.model.IntervalPoint
import java.time.Instant
import java.time.ZoneId

class CalculateEventInsightUseCase {
    operator fun invoke(records: List<EventRecordEntity>): EventInsight {
        val ordered = records.sortedBy { it.occurredAt }
        val intervalPoints = ordered.zipWithNext().mapIndexedNotNull { index, pair ->
            val interval = pair.second.occurredAt - pair.first.occurredAt
            if (interval >= 0L) {
                IntervalPoint(index = index + 1, intervalMillis = interval)
            } else {
                null
            }
        }

        val recordsByDate = records
            .groupingBy {
                Instant.ofEpochMilli(it.occurredAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .eachCount()

        return EventInsight(
            intervalPoints = intervalPoints,
            trendSlopeMillis = calculateSlope(intervalPoints),
            recordsByDate = recordsByDate,
        )
    }

    private fun calculateSlope(points: List<IntervalPoint>): Double {
        if (points.size < 2) return 0.0

        val averageX = points.map { it.index }.average()
        val averageY = points.map { it.intervalMillis }.average()
        val numerator = points.sumOf { (it.index - averageX) * (it.intervalMillis - averageY) }
        val denominator = points.sumOf { (it.index - averageX) * (it.index - averageX) }

        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
}
