package com.thelast.app.domain.usecase

import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.domain.model.EventStats

class CalculateEventStatsUseCase {
    operator fun invoke(records: List<EventRecordEntity>): EventStats {
        if (records.isEmpty()) return EventStats()

        val ordered = records.sortedBy { it.occurredAt }
        val intervals = ordered.zipWithNext { first, second ->
            second.occurredAt - first.occurredAt
        }.filter { it >= 0L }

        return EventStats(
            recordCount = records.size,
            latestOccurredAt = records.maxOf { it.occurredAt },
            averageIntervalMillis = intervals.takeIf { it.isNotEmpty() }?.average()?.toLong(),
            shortestIntervalMillis = intervals.minOrNull(),
            longestIntervalMillis = intervals.maxOrNull(),
        )
    }
}
