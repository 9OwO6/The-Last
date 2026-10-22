package com.thelast.app.domain.usecase

import com.thelast.app.data.repository.EventRepository
import com.thelast.app.domain.model.EventSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveEventSummariesUseCase(
    private val repository: EventRepository,
) {
    operator fun invoke(): Flow<List<EventSummary>> {
        return repository.observeActiveEventTypes().map { eventTypes ->
            eventTypes.map { eventType ->
                EventSummary(
                    id = eventType.id,
                    name = eventType.name,
                    icon = eventType.icon,
                    color = eventType.color,
                    lastOccurredAt = eventType.lastOccurredAt,
                    previousOccurredAt = eventType.previousOccurredAt,
                    recordCount = eventType.recordCount,
                )
            }
        }
    }
}
