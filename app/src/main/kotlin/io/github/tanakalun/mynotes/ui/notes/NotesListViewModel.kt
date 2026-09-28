package io.github.tanakalun.mynotes.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tanakalun.mynotes.data.model.Note
import io.github.tanakalun.mynotes.data.model.NoteType
import io.github.tanakalun.mynotes.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesListUiState(
    val allNotes: List<Note> = emptyList(),
    val query: String = "",
    val notes: List<Note> = emptyList(),
    val loading: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotesListViewModel(
    private val repo: NoteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val queryFlow = MutableStateFlow(savedStateHandle.get<String>("listQuery") ?: "")

    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            queryFlow
                .onEach { q -> _uiState.update { it.copy(query = q) } }
                .debounce(250)
                .distinctUntilChanged()
                .flatMapLatest { q ->
                    repo.notes.map { notes ->
                        if (q.isBlank()) notes
                        else {
                            val needle = q.lowercase()
                            notes.filter { note ->
                                note.title.lowercase().contains(needle) ||
                                    note.content.lowercase().contains(needle) ||
                                    note.checklist.any { it.text.lowercase().contains(needle) }
                            }
                        }
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { filtered ->
                    _uiState.update { it.copy(allNotes = repo.notes.value, notes = filtered, loading = false) }
                }
        }
    }

    fun onQueryChange(query: String) {
        queryFlow.value = query
    }

    fun toggleSelection() {
        _uiState.update {
            it.copy(
                selectionMode = !it.selectionMode,
                selectedIds = if (it.selectionMode) it.selectedIds else emptySet(),
            )
        }
    }

    fun enterSelection(noteId: String) {
        _uiState.update { it.copy(selectionMode = true, selectedIds = setOf(noteId)) }
    }

    fun toggleSelect(noteId: String) {
        _uiState.update { state ->
            val next = if (noteId in state.selectedIds) {
                state.selectedIds - noteId
            } else {
                state.selectedIds + noteId
            }
            state.copy(selectedIds = next, selectionMode = next.isNotEmpty())
        }
    }

    fun exitSelection() {
        _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun onOpenRequested(note: Note): NoteType {
        exitSelection()
        return note.type
    }

    fun deleteSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repo.deleteAll(ids)
            exitSelection()
        }
    }

    fun togglePinSelected() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repo.togglePinAll(ids)
            exitSelection()
        }
    }
}