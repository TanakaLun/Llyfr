package io.github.tanakalun.llyfr.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tanakalun.llyfr.data.model.CheckItem
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.model.NoteType
import io.github.tanakalun.llyfr.data.repository.NoteRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChecklistUiState(
    val title: String = "",
    val colorIndex: Int = 0,
    val items: List<CheckItem> = listOf(CheckItem(id = "", text = "")),
    val readOnly: Boolean = false,
    val loading: Boolean = true,
    val isNew: Boolean = true,
)

class ChecklistViewModel(
    private val repo: NoteRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private var noteId: String = ""
    private var existing: Note? = null

    fun load(noteId: String) {
        if (this.noteId.isNotEmpty()) return
        this.noteId = noteId
        viewModelScope.launch {
            val existingNote = if (noteId.isNotBlank()) repo.getById(noteId) else null
            existing = existingNote
            val items = existingNote?.checklist?.ifEmpty { defaultItem() } ?: defaultItem()
            _uiState.update {
                it.copy(
                    title = existingNote?.title ?: "",
                    colorIndex = existingNote?.colorIndex ?: 0,
                    items = items,
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

    fun addItem() {
        _uiState.update { it.copy(items = it.items + newItem()) }
    }

    fun updateItemText(id: String, text: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(text = text) else it })
        }
    }

    fun toggleItemChecked(id: String) {
        _uiState.update { state ->
            state.copy(items = state.items.map { if (it.id == id) it.copy(checked = !it.checked) else it })
        }
    }

    fun removeItem(id: String) {
        _uiState.update { state ->
            val next = state.items.filter { it.id != id }
            state.copy(items = next.ifEmpty { defaultItem() })
        }
    }

    fun save(onDone: () -> Unit) {
        val note = buildNoteOrNull()
        if (note == null) {
            onDone()
        } else {
            viewModelScope.launch {
                repo.save(note)
                onDone()
            }
        }
    }

    /** 手势/系统返回路径：同步弹栈 + 应用级作用域异步落盘。 */
    fun saveAndBack(onBack: () -> Unit) {
        val note = buildNoteOrNull()
        onBack()
        if (note != null) {
            appScope.launch { repo.save(note) }
        }
    }

    private fun buildNoteOrNull(): Note? {
        val state = _uiState.value
        val items = state.items.filter { it.text.isNotBlank() }
        if (state.title.isNotBlank() || items.isNotEmpty()) {
            return existing?.copy(
                title = state.title,
                checklist = items,
                colorIndex = state.colorIndex,
            ) ?: Note(
                id = UUID.randomUUID().toString(),
                type = NoteType.Checklist,
                title = state.title,
                content = "",
                checklist = items,
                colorIndex = state.colorIndex,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        }
        return null
    }

    private fun defaultItem() = listOf(newItem())
    private fun newItem() = CheckItem(id = UUID.randomUUID().toString(), text = "")
}