package io.github.tanakalun.llyfr.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.reminder.ReminderScheduler
import io.github.tanakalun.llyfr.data.repository.NoteRepository
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoEditorUiState(
    val title: String = "",
    val colorIndex: Int = 0,
    val reminderAt: Long = 0L,
    val readOnly: Boolean = false,
    val loading: Boolean = true,
    val isNew: Boolean = true,
)

class TodoEditorViewModel(
    private val repo: NoteRepository,
    private val scheduler: ReminderScheduler,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoEditorUiState())
    val uiState: StateFlow<TodoEditorUiState> = _uiState.asStateFlow()

    private var noteId: String = ""
    private var existing: Note? = null

    fun load(noteId: String) {
        if (this.noteId.isNotEmpty()) return
        this.noteId = noteId
        viewModelScope.launch {
            val existingNote = if (noteId.isNotBlank()) repo.getById(noteId) else null
            existing = existingNote
            _uiState.update {
                it.copy(
                    title = existingNote?.title ?: "",
                    colorIndex = existingNote?.colorIndex ?: 0,
                    reminderAt = existingNote?.reminderAt ?: 0L,
                    loading = false,
                    isNew = existingNote == null,
                )
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onColorChange(colorIndex: Int) {
        _uiState.update { it.copy(colorIndex = colorIndex) }
    }

    fun toggleReadOnly() {
        _uiState.update { it.copy(readOnly = !it.readOnly) }
    }

    fun setReminder(at: Long) {
        _uiState.update { it.copy(reminderAt = at) }
    }

    fun save(onDone: () -> Unit) {
        val note = buildNoteOrNull() ?: return onDone()
        viewModelScope.launch {
            repo.save(note)
            reschedule(note)
            onDone()
        }
    }

    /** 手势/系统返回路径：同步弹栈 + 应用级作用域异步落盘并重排提醒。 */
    fun saveAndBack(onBack: () -> Unit) {
        val note = buildNoteOrNull()
        onBack()
        if (note != null) {
            appScope.launch {
                repo.save(note)
                reschedule(note)
            }
        }
    }

    private fun reschedule(note: Note) {
        if (note.reminderAt > 0L) {
            scheduler.schedule(note.id, note.title, note.reminderAt, SettingsStore.reminderNotifStyle)
        } else {
            scheduler.cancel(note.id)
        }
    }

    private fun buildNoteOrNull(): Note? {
        val state = _uiState.value
        if (state.title.isBlank() && state.reminderAt == 0L) return null
        return existing?.copy(
            title = state.title,
            colorIndex = state.colorIndex,
            reminderAt = state.reminderAt,
        ) ?: Note.createTodo(title = state.title, reminderAt = state.reminderAt, colorIndex = state.colorIndex)
    }
}