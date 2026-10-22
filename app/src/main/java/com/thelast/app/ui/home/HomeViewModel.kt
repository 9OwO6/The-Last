package com.thelast.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thelast.app.TheLastApplication
import com.thelast.app.data.datastore.ThemeMode
import com.thelast.app.domain.model.BackupExport
import com.thelast.app.domain.model.BackupExportFormat
import com.thelast.app.domain.model.BackupImportDraft
import com.thelast.app.domain.model.BackupImportPreview
import com.thelast.app.domain.model.EventSummary
import com.thelast.app.domain.model.HomeCalendarRecord
import com.thelast.app.domain.usecase.BuildBackupExportUseCase
import com.thelast.app.domain.usecase.ImportBackupUseCase
import com.thelast.app.domain.usecase.ObserveEventSummariesUseCase
import com.thelast.app.domain.usecase.ParseBackupImportUseCase
import com.thelast.app.domain.voice.VoiceCommand
import com.thelast.app.domain.voice.VoiceCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

data class HomeUiState(
    val events: List<EventSummary> = emptyList(),
    val calendarRecords: List<HomeCalendarRecord> = emptyList(),
    val recordingEventId: Long? = null,
    val userMessage: HomeUserMessage? = null,
    val pendingExport: BackupExport? = null,
    val pendingImportPreview: BackupImportPreview? = null,
    val themeMode: ThemeMode = ThemeMode.System,
    val eventGroups: List<EventGroup> = emptyList(),
    val rhythmSummary: String = "今天也轻轻记一下吧",
    val templates: List<EventTemplate> = DefaultEventTemplates,
    val latestRecordFeedback: HomeRecordFeedback? = null,
    val voiceConfirmation: VoiceConfirmation? = null,
)

data class HomeUserMessage(
    val message: String,
    val undoRecordId: Long? = null,
)

data class EventGroup(
    val title: String,
    val subtitle: String,
    val events: List<EventSummary>,
)

data class EventTemplate(
    val name: String,
    val icon: String,
    val color: Long,
    val category: String,
)

data class HomeRecordFeedback(
    val eventTypeId: Long,
    val message: String,
)

enum class VoiceConfirmationAction {
    CreateEvent,
    CreateAndRecord,
}

data class VoiceConfirmation(
    val rawText: String,
    val eventName: String,
    val action: VoiceConfirmationAction,
)

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as TheLastApplication
    private val repository = app.eventRepository
    private val settingsStore = app.settingsStore
    private val observeEventSummaries = ObserveEventSummariesUseCase(repository)
    private val buildBackupExport = BuildBackupExportUseCase()
    private val parseBackupImport = ParseBackupImportUseCase()
    private val importBackup = ImportBackupUseCase(repository)
    private val voiceCommandParser = VoiceCommandParser()
    private val recordingEventId = MutableStateFlow<Long?>(null)
    private val userMessage = MutableStateFlow<HomeUserMessage?>(null)
    private val pendingExport = MutableStateFlow<BackupExport?>(null)
    private val pendingImportPreview = MutableStateFlow<BackupImportPreview?>(null)
    private val latestRecordFeedback = MutableStateFlow<HomeRecordFeedback?>(null)
    private val voiceConfirmation = MutableStateFlow<VoiceConfirmation?>(null)
    private var pendingImportDraft: BackupImportDraft? = null

    private val baseWithoutFeedback = combine(
        observeEventSummaries(),
        repository.observeActiveRecordsWithEventTypes(),
        recordingEventId,
        userMessage,
        pendingExport,
    ) { events, records, activeRecordingEventId, message, export ->
        val sortedEvents = events.smartSorted()
        HomeBaseState(
            events = sortedEvents,
            calendarRecords = records.map { record ->
                HomeCalendarRecord(
                    recordId = record.recordId,
                    eventTypeId = record.eventTypeId,
                    eventName = record.eventName,
                    eventIcon = record.eventIcon,
                    eventColor = record.eventColor,
                    occurredAt = record.occurredAt,
                    note = record.note,
                )
            },
            recordingEventId = activeRecordingEventId,
            userMessage = message,
            pendingExport = export,
            latestRecordFeedback = null,
        )
    }

    private val baseState = combine(
        baseWithoutFeedback,
        latestRecordFeedback,
    ) { base, feedback ->
        base.copy(latestRecordFeedback = feedback)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseState,
        pendingImportPreview,
        settingsStore.themeMode,
        voiceConfirmation,
    ) { base, importPreview, themeMode, pendingVoiceConfirmation ->
        val groups = base.events.toEventGroups()
        HomeUiState(
            events = base.events,
            calendarRecords = base.calendarRecords,
            recordingEventId = base.recordingEventId,
            userMessage = base.userMessage,
            pendingExport = base.pendingExport,
            pendingImportPreview = importPreview,
            themeMode = themeMode,
            eventGroups = groups,
            rhythmSummary = base.events.rhythmSummary(),
            latestRecordFeedback = base.latestRecordFeedback,
            voiceConfirmation = pendingVoiceConfirmation,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun quickRecord(eventTypeId: Long) {
        if (recordingEventId.value != null) return
        viewModelScope.launch {
            recordingEventId.value = eventTypeId
            userMessage.value = null
            runCatching {
                repository.quickRecord(eventTypeId)
            }.onSuccess { result ->
                val feedbackMessage = result.previousOccurredAt?.let { previous ->
                    "记好啦，距离上次 ${formatInterval(result.occurredAt - previous)}"
                } ?: "这是第一次留下这个小节点"
                latestRecordFeedback.value = HomeRecordFeedback(
                    eventTypeId = eventTypeId,
                    message = feedbackMessage,
                )
                userMessage.value = HomeUserMessage(
                    message = result.previousOccurredAt?.let { previous ->
                        "已记录，距离上次 ${formatInterval(result.occurredAt - previous)}"
                    } ?: "这是第一次留下这个小节点",
                    undoRecordId = result.recordId,
                )
                delay(2_600)
                if (latestRecordFeedback.value?.eventTypeId == eventTypeId) {
                    latestRecordFeedback.value = null
                }
            }.onFailure {
                userMessage.value = HomeUserMessage("记录失败，请稍后重试")
            }
            recordingEventId.value = null
        }
    }

    fun handleVoiceText(text: String) {
        val command = voiceCommandParser.parse(text)
        when (command) {
            is VoiceCommand.CreateEvent -> handleVoiceCreateCommand(command)
            is VoiceCommand.RecordEvent -> handleVoiceRecordCommand(command)
            is VoiceCommand.Unknown -> {
                voiceConfirmation.value = null
                userMessage.value = HomeUserMessage("没听懂这句，可以试试「记录喝水」或「新建卡片加油」")
            }
        }
    }

    fun dismissVoiceConfirmation() {
        voiceConfirmation.value = null
    }

    fun confirmVoiceAction() {
        val confirmation = voiceConfirmation.value ?: return
        voiceConfirmation.value = null
        viewModelScope.launch {
            when (confirmation.action) {
                VoiceConfirmationAction.CreateEvent -> createVoiceEvent(confirmation.eventName, recordAfterCreate = false)
                VoiceConfirmationAction.CreateAndRecord -> createVoiceEvent(confirmation.eventName, recordAfterCreate = true)
            }
        }
    }

    fun onVoiceUnavailable(message: String) {
        voiceConfirmation.value = null
        userMessage.value = HomeUserMessage(message)
    }

    fun userMessageShown() {
        userMessage.update { null }
    }

    fun undoQuickRecord(recordId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteRecordIfExists(recordId)
            userMessage.value = HomeUserMessage(
                if (deleted) "刚才那次记录已撤销" else "这条记录已经不在了",
            )
        }
    }

    fun prepareExport(format: BackupExportFormat) {
        viewModelScope.launch {
            runCatching {
                buildBackupExport(
                    snapshot = repository.getBackupSnapshot(),
                    format = format,
                )
            }.onSuccess { export ->
                pendingExport.value = export
            }.onFailure {
                userMessage.value = HomeUserMessage("导出失败，请稍后重试")
            }
        }
    }

    fun exportHandled(success: Boolean) {
        pendingExport.value = null
        userMessage.value = HomeUserMessage(if (success) "导出完成" else "已取消导出")
    }

    fun prepareImport(content: String?) {
        if (content == null) return
        viewModelScope.launch {
            runCatching {
                parseBackupImport(content)
            }.onSuccess { draft ->
                pendingImportDraft = draft
                pendingImportPreview.value = draft.preview
            }.onFailure { error ->
                pendingImportDraft = null
                pendingImportPreview.value = null
                userMessage.value = HomeUserMessage(error.message ?: "恢复备份失败，请换个文件试试")
            }
        }
    }

    fun dismissImportPreview() {
        pendingImportDraft = null
        pendingImportPreview.value = null
    }

    fun confirmImport() {
        val draft = pendingImportDraft ?: return
        viewModelScope.launch {
            runCatching {
                importBackup(draft)
            }.onSuccess { preview ->
                pendingImportDraft = null
                pendingImportPreview.value = null
                userMessage.value = HomeUserMessage("已恢复 ${preview.eventTypeCount} 个事件、${preview.recordCount} 条记录")
            }.onFailure {
                userMessage.value = HomeUserMessage("恢复失败，备份没有写入")
            }
        }
    }

    fun createEventFromTemplate(template: EventTemplate) {
        viewModelScope.launch {
            runCatching {
                repository.createEventType(
                    name = template.name,
                    icon = template.icon,
                    color = template.color,
                    category = template.category,
                    sortOrder = 0,
                )
            }.onSuccess {
                userMessage.value = HomeUserMessage("已添加「${template.name}」")
            }.onFailure {
                userMessage.value = HomeUserMessage("添加失败，请稍后重试")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    private fun handleVoiceCreateCommand(command: VoiceCommand.CreateEvent) {
        val existing = findEventMatch(command.eventName, uiState.value.events)
        if (existing is EventMatch.Single) {
            voiceConfirmation.value = null
            userMessage.value = HomeUserMessage("已经有「${existing.event.name}」卡片了")
            return
        }
        voiceConfirmation.value = VoiceConfirmation(
            rawText = command.rawText,
            eventName = command.eventName,
            action = VoiceConfirmationAction.CreateEvent,
        )
    }

    private fun handleVoiceRecordCommand(command: VoiceCommand.RecordEvent) {
        when (val match = findEventMatch(command.eventName, uiState.value.events)) {
            is EventMatch.Single -> quickRecord(match.event.id)
            EventMatch.Ambiguous -> {
                voiceConfirmation.value = null
                userMessage.value = HomeUserMessage("找到多个相似卡片，请说得更具体一点")
            }
            EventMatch.None -> {
                voiceConfirmation.value = VoiceConfirmation(
                    rawText = command.rawText,
                    eventName = command.eventName,
                    action = VoiceConfirmationAction.CreateAndRecord,
                )
            }
        }
    }

    private suspend fun createVoiceEvent(eventName: String, recordAfterCreate: Boolean) {
        runCatching {
            val template = DefaultEventTemplates.firstOrNull { template ->
                val eventKey = eventName.normalizedEventName()
                val templateKey = template.name.normalizedEventName()
                eventKey == templateKey || eventKey.contains(templateKey) || templateKey.contains(eventKey)
            }
            val newId = repository.createEventType(
                name = eventName,
                icon = template?.icon ?: "history",
                color = template?.color ?: 0xFF4E6751,
                category = template?.category,
                sortOrder = 0,
            )
            if (recordAfterCreate) {
                repository.quickRecord(newId)
            }
        }.onSuccess {
            userMessage.value = HomeUserMessage(
                if (recordAfterCreate) "已新建「$eventName」并记录一次" else "已新建「$eventName」",
            )
        }.onFailure {
            userMessage.value = HomeUserMessage("语音操作失败，请稍后重试")
        }
    }

    private fun formatInterval(intervalMillis: Long): String {
        val duration = Duration.ofMillis(intervalMillis.coerceAtLeast(0))
        val days = duration.toDays()
        if (days > 0) return "$days 天"
        val hours = duration.toHours()
        if (hours > 0) return "$hours 小时"
        val minutes = duration.toMinutes()
        return if (minutes > 0) "$minutes 分钟" else "刚刚"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                return HomeViewModel(application) as T
            }
        }
    }
}

private data class HomeBaseState(
    val events: List<EventSummary>,
    val calendarRecords: List<HomeCalendarRecord>,
    val recordingEventId: Long?,
    val userMessage: HomeUserMessage?,
    val pendingExport: BackupExport?,
    val latestRecordFeedback: HomeRecordFeedback?,
)

private sealed interface EventMatch {
    data class Single(val event: EventSummary) : EventMatch
    data object Ambiguous : EventMatch
    data object None : EventMatch
}

private fun findEventMatch(eventName: String, events: List<EventSummary>): EventMatch {
    val target = eventName.normalizedEventName()
    if (target.isBlank()) return EventMatch.None

    val exactMatches = events.filter { it.name.normalizedEventName() == target }
    if (exactMatches.size == 1) return EventMatch.Single(exactMatches.first())
    if (exactMatches.size > 1) return EventMatch.Ambiguous

    val fuzzyMatches = events.filter { event ->
        val name = event.name.normalizedEventName()
        name.contains(target) || target.contains(name)
    }
    return when (fuzzyMatches.size) {
        0 -> EventMatch.None
        1 -> EventMatch.Single(fuzzyMatches.first())
        else -> EventMatch.Ambiguous
    }
}

private fun String.normalizedEventName(): String {
    return lowercase()
        .replace(Regex("[\\s:：;；!！?？\"“”'‘’，。,、.]"), "")
}

private fun List<EventSummary>.smartSorted(): List<EventSummary> {
    return mapIndexed { index, event -> IndexedValue(index, event) }
        .sortedWith(
            compareBy<IndexedValue<EventSummary>> { it.value.lastOccurredAt != null }
                .thenBy { it.value.lastOccurredAt ?: Long.MIN_VALUE }
                .thenBy { it.index },
        )
        .map { it.value }
}

private fun List<EventSummary>.toEventGroups(): List<EventGroup> {
    val never = filter { it.lastOccurredAt == null }
    val idle = filter { event ->
        event.lastOccurredAt?.let { Duration.ofMillis(System.currentTimeMillis() - it).toDays() > 14 } == true
    }
    val recent = filter { it.lastOccurredAt != null && it !in idle }

    return buildList {
        if (never.isNotEmpty()) {
            add(EventGroup("还没开始", "这些小事还在等第一次记录", never))
        }
        if (idle.isNotEmpty()) {
            add(EventGroup("好久没记", "超过两周没有留下新节点", idle))
        }
        if (recent.isNotEmpty()) {
            add(EventGroup("最近记录", "最近已经照顾到的小节奏", recent))
        }
    }
}

private fun List<EventSummary>.rhythmSummary(): String {
    if (isEmpty()) return "挑几个模板，先把生活节奏放进来"
    val neverCount = count { it.lastOccurredAt == null }
    val idleCount = count { event ->
        event.lastOccurredAt?.let { Duration.ofMillis(System.currentTimeMillis() - it).toDays() > 14 } == true
    }
    return when {
        neverCount > 0 && idleCount > 0 -> "$neverCount 件还没开始，$idleCount 件很久没记录了"
        neverCount > 0 -> "$neverCount 件小事还在等第一次记录"
        idleCount > 0 -> "有 $idleCount 件小事很久没记录了"
        else -> "最近节奏被照顾得不错"
    }
}

val DefaultEventTemplates = listOf(
    EventTemplate("加油", "gas", 0xFFE9AFA3, "车辆"),
    EventTemplate("洗车", "wash", 0xFF7FB7D9, "车辆"),
    EventTemplate("换床单", "bed", 0xFFA8C686, "家务"),
    EventTemplate("剪发", "cut", 0xFFF1B85B, "生活"),
    EventTemplate("家务", "home", 0xFFC9A6DD, "家"),
    EventTemplate("保养", "build", 0xFF4E6751, "维护"),
)
