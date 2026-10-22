package com.thelast.app.ui.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thelast.app.TheLastApplication
import com.thelast.app.data.model.EventRecordEntity
import com.thelast.app.data.model.EventTypeEntity
import com.thelast.app.domain.model.EventInsight
import com.thelast.app.domain.model.EventStats
import com.thelast.app.domain.usecase.CalculateEventInsightUseCase
import com.thelast.app.domain.usecase.CalculateEventStatsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class EventDetailUiState(
    val eventType: EventTypeEntity? = null,
    val records: List<EventRecordEntity> = emptyList(),
    val stats: EventStats = EventStats(),
    val insight: EventInsight = EventInsight(),
    val rhythmInsight: RhythmInsight = RhythmInsight(),
    val message: String? = null,
)

data class RhythmInsight(
    val title: String = "还没有节奏",
    val body: String = "先轻轻记录一次，这件事的小故事就会开始。",
)

data class RecordEditorState(
    val recordId: Long? = null,
    val title: String = "补记记录",
    val occurredAtText: String = "",
    val note: String = "",
    val error: String? = null,
)

class EventDetailViewModel(
    application: Application,
    private val eventTypeId: Long,
) : AndroidViewModel(application) {
    private val repository = (application as TheLastApplication).eventRepository
    private val calculateEventStats = CalculateEventStatsUseCase()
    private val calculateEventInsight = CalculateEventInsightUseCase()
    private val message = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val uiState: StateFlow<EventDetailUiState> = combine(
        repository.observeEventType(eventTypeId),
        repository.observeRecordsForEvent(eventTypeId),
        message,
    ) { eventType, records, activeMessage ->
        val stats = calculateEventStats(records)
        val insight = calculateEventInsight(records)
        EventDetailUiState(
            eventType = eventType,
            records = records,
            stats = stats,
            insight = insight,
            rhythmInsight = buildRhythmInsight(records, stats, insight),
            message = activeMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventDetailUiState(),
    )

    fun newRecordEditorState(): RecordEditorState {
        return RecordEditorState(
            title = "补记记录",
            occurredAtText = formatForInput(System.currentTimeMillis()),
        )
    }

    fun editorStateFor(record: EventRecordEntity): RecordEditorState {
        return RecordEditorState(
            recordId = record.id,
            title = "编辑记录",
            occurredAtText = formatForInput(record.occurredAt),
            note = record.note.orEmpty(),
        )
    }

    fun saveRecord(
        editorState: RecordEditorState,
        onInvalid: (RecordEditorState) -> Unit,
        onSaved: () -> Unit,
    ) {
        val occurredAt = parseInputTime(editorState.occurredAtText)
        if (occurredAt == null) {
            onInvalid(editorState.copy(error = "时间格式应为 yyyy-MM-dd HH:mm"))
            return
        }

        viewModelScope.launch {
            if (editorState.recordId == null) {
                repository.addRecord(
                    eventTypeId = eventTypeId,
                    occurredAt = occurredAt,
                    note = editorState.note,
                )
                message.value = "已补记记录"
            } else {
                repository.updateRecord(
                    id = editorState.recordId,
                    occurredAt = occurredAt,
                    note = editorState.note,
                )
                message.value = "已更新记录"
            }
            onSaved()
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            message.value = "已删除记录"
        }
    }

    fun deleteEvent(onDeleted: () -> Unit) {
        viewModelScope.launch {
            if (repository.deleteEventType(eventTypeId)) {
                onDeleted()
            } else {
                message.value = "事件已经不存在"
            }
        }
    }

    fun messageShown() {
        message.value = null
    }

    private fun formatForInput(timestamp: Long): String {
        return DateTimeFormatter.ofPattern(InputPattern)
            .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime())
    }

    private fun parseInputTime(value: String): Long? {
        return try {
            LocalDateTime.parse(value.trim(), DateTimeFormatter.ofPattern(InputPattern))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    companion object {
        private const val InputPattern = "yyyy-MM-dd HH:mm"

        fun createFactory(eventTypeId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras,
                ): T {
                    val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                    return EventDetailViewModel(application, eventTypeId) as T
                }
            }
        }
    }
}

private fun buildRhythmInsight(
    records: List<EventRecordEntity>,
    stats: EventStats,
    insight: EventInsight,
): RhythmInsight {
    if (records.isEmpty()) {
        return RhythmInsight()
    }
    if (records.size == 1) {
        return RhythmInsight(
            title = "第一个节点已经留下",
            body = "再记录几次后，这里会看见它的间隔和趋势。",
        )
    }

    val daysSinceLatest = stats.latestOccurredAt?.let { latest ->
        java.time.temporal.ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(latest).atZone(ZoneId.systemDefault()).toLocalDate(),
            java.time.LocalDate.now(ZoneId.systemDefault()),
        )
    } ?: 0L

    return when {
        daysSinceLatest > 30 -> RhythmInsight(
            title = "已经安静了一阵子",
            body = "上一次发生在 $daysSinceLatest 天前，也许它暂时不常出现了。",
        )

        insight.trendSlopeMillis < -60L * 60L * 1000L -> RhythmInsight(
            title = "最近更频繁了",
            body = "两次记录之间的间隔正在变短，这件事出现得更密了。",
        )

        insight.trendSlopeMillis > 60L * 60L * 1000L -> RhythmInsight(
            title = "最近慢下来了",
            body = "间隔正在变长，这件事发生得没有之前那么频繁。",
        )

        else -> RhythmInsight(
            title = "节奏挺稳定",
            body = "最近几次间隔变化不大，是一条比较平稳的小节奏。",
        )
    }
}
