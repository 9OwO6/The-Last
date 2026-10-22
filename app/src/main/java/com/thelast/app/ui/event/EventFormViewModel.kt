package com.thelast.app.ui.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thelast.app.TheLastApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventIconOption(
    val key: String,
    val label: String,
)

data class EventColorOption(
    val value: Long,
    val label: String,
)

data class EventFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val category: String = "",
    val selectedIcon: String = DefaultEventIcon,
    val selectedColor: Long = DefaultEventColor,
    val nameError: String? = null,
    val isFinished: Boolean = false,
    val isArchived: Boolean = false,
)

const val DefaultEventIcon = "history"
const val DefaultEventColor = 0xFF4E6751

val EventIconOptions = listOf(
    EventIconOption("history", "常规"),
    EventIconOption("food", "吃饭"),
    EventIconOption("gas", "加油"),
    EventIconOption("cut", "剪发"),
    EventIconOption("car", "用车"),
    EventIconOption("wash", "洗车"),
    EventIconOption("bed", "床品"),
    EventIconOption("home", "家务"),
    EventIconOption("build", "保养"),
    EventIconOption("heart", "健康"),
    EventIconOption("star", "重要"),
    EventIconOption("check", "完成"),
)

val EventColorOptions = listOf(
    EventColorOption(0xFF4E6751, "松绿"),
    EventColorOption(0xFFE9AFA3, "蜜桃"),
    EventColorOption(0xFFF1B85B, "奶黄"),
    EventColorOption(0xFF7FB7D9, "晴蓝"),
    EventColorOption(0xFFA8C686, "青芽"),
    EventColorOption(0xFFC9A6DD, "软紫"),
)

class EventFormViewModel(
    application: Application,
    private val eventTypeId: Long?,
) : AndroidViewModel(application) {
    private val repository = (application as TheLastApplication).eventRepository

    private val _uiState = MutableStateFlow(
        EventFormUiState(
            isEditMode = eventTypeId != null,
            isLoading = eventTypeId != null,
        ),
    )
    val uiState: StateFlow<EventFormUiState> = _uiState.asStateFlow()

    init {
        if (eventTypeId != null) {
            viewModelScope.launch {
                val eventType = repository.getEventType(eventTypeId)
                if (eventType == null) {
                    _uiState.update { it.copy(isLoading = false, isFinished = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = eventType.name,
                            category = eventType.category.orEmpty(),
                            selectedIcon = eventType.icon,
                            selectedColor = eventType.color,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                name = value,
                nameError = if (value.isBlank()) "请输入事件名称" else null,
            )
        }
    }

    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun onIconSelected(value: String) {
        _uiState.update { it.copy(selectedIcon = value) }
    }

    fun onColorSelected(value: Long) {
        _uiState.update { it.copy(selectedColor = value) }
    }

    fun save() {
        val current = _uiState.value
        if (current.name.isBlank()) {
            _uiState.update { it.copy(nameError = "请输入事件名称") }
            return
        }

        viewModelScope.launch {
            if (eventTypeId == null) {
                repository.createEventType(
                    name = current.name,
                    icon = current.selectedIcon,
                    color = current.selectedColor,
                    category = current.category,
                    sortOrder = 0,
                )
            } else {
                repository.updateEventType(
                    id = eventTypeId,
                    name = current.name,
                    icon = current.selectedIcon,
                    color = current.selectedColor,
                    category = current.category,
                )
            }
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun archive() {
        val id = eventTypeId ?: return
        viewModelScope.launch {
            repository.archiveEventType(id)
            _uiState.update { it.copy(isArchived = true) }
        }
    }

    companion object {
        fun createFactory(eventTypeId: Long? = null): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: androidx.lifecycle.viewmodel.CreationExtras,
                ): T {
                    val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                    return EventFormViewModel(application, eventTypeId) as T
                }
            }
        }
    }
}
