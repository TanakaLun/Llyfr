package io.github.tanakalun.llyfr.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tanakalun.llyfr.data.media.ImageUtils
import io.github.tanakalun.llyfr.data.model.ContentSegment
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.model.parseSegmentsFromMarkdown
import io.github.tanakalun.llyfr.data.model.serializeSegmentsToMarkdown
import io.github.tanakalun.llyfr.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NoteEditorUiState(
    val title: String = "",
    val colorIndex: Int = 0,
    val segments: List<ContentSegment> = listOf(ContentSegment.Text()),
    val readOnly: Boolean = false,
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val unreadable: Boolean = false,
    val pendingFocusId: String? = null,
)

/** 插入图片时的分段请求：before/after 为 null 表示不可分段。 */
data class SplitRequest(
    val focusedIndex: Int,
    val before: String?,
    val after: String?,
)

class NoteEditorViewModel(
    private val repo: NoteRepository,
    private val appContext: Context,
    private val appScope: CoroutineScope,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var noteId: String = ""
    private var existing: Note? = null

    fun load(noteId: String) {
        if (this.noteId.isNotEmpty()) return
        this.noteId = noteId
        viewModelScope.launch {
            val restored = savedState.get<String>("draft_note_id") == noteId
            val draftMarkdown = if (restored) savedState.get<String>("draft_markdown") else null
            if (restored && draftMarkdown != null) {
                existing = null
                _uiState.update {
                    it.copy(
                        title = savedState.get<String>("draft_title") ?: "",
                        colorIndex = savedState.get<Int>("draft_color") ?: 0,
                        segments = parseSegmentsFromMarkdown(draftMarkdown),
                        loading = false,
                        isNew = noteId.isBlank(),
                    )
                }
                return@launch
            }

            val existingNote = if (noteId.isNotBlank()) repo.getById(noteId) else null
            existing = existingNote
            if (existingNote?.unreadable == true) {
                _uiState.update { it.copy(unreadable = true, loading = false, isNew = false) }
            } else {
                val segments = existingNote?.let { parseSegmentsFromMarkdown(it.content) }
                    ?: listOf(ContentSegment.Text())
                _uiState.update {
                    it.copy(
                        title = existingNote?.title ?: "",
                        colorIndex = existingNote?.colorIndex ?: 0,
                        segments = segments,
                        loading = false,
                        isNew = existingNote == null,
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
        persistDraft()
    }

    fun onColorChange(colorIndex: Int) {
        _uiState.update { it.copy(colorIndex = colorIndex) }
        persistDraft()
    }

    fun toggleReadOnly() {
        _uiState.update { it.copy(readOnly = !it.readOnly) }
    }

    fun consumeFocus(id: String) {
        if (_uiState.value.pendingFocusId == id) {
            _uiState.update { it.copy(pendingFocusId = null) }
        }
    }

    fun onImagesSelected(uris: List<Uri>, splitProvider: () -> SplitRequest?) {
        viewModelScope.launch {
            val images = uris.mapNotNull { uri ->
                val path = withContext(Dispatchers.IO) {
                    ImageUtils.copyImageToInternal(appContext, uri)
                } ?: return@mapNotNull null
                val (w, h) = withContext(Dispatchers.Default) { ImageUtils.getImageDimensions(path) }
                ContentSegment.Image(path = path, width = w, height = h)
            }
            if (images.isEmpty()) return@launch
            applyInsertion(images, splitProvider())
            persistDraft()
        }
    }

    private fun applyInsertion(images: List<ContentSegment.Image>, split: SplitRequest?) {
        val state = _uiState.value
        val segments = state.segments.toMutableList()
        val focusedIndex = split?.focusedIndex ?: -1

        if (split != null && split.before != null && split.after != null &&
            focusedIndex in segments.indices && segments[focusedIndex] is ContentSegment.Text
        ) {
            segments[focusedIndex] = (segments[focusedIndex] as ContentSegment.Text).copy(markdown = split.before)
            val insertAt = focusedIndex + 1
            images.forEachIndexed { k, image -> segments.add(insertAt + k, image) }
            val afterSegment = ContentSegment.Text(markdown = split.after)
            segments.add(insertAt + images.size, afterSegment)
            _uiState.update { it.copy(segments = segments, pendingFocusId = afterSegment.id) }
        } else {
            val insertIndex = focusedIndex.plus(1).takeIf { it in 0..segments.size } ?: segments.size
            images.forEachIndexed { k, image -> segments.add(insertIndex + k, image) }
            val trailingSegment = ContentSegment.Text()
            segments.add(insertIndex + images.size, trailingSegment)
            _uiState.update { it.copy(segments = segments, pendingFocusId = trailingSegment.id) }
        }
    }

    fun removeImage(index: Int, markdownResolver: (String) -> String) {
        val state = _uiState.value
        if (index !in state.segments.indices) return
        val segments = state.segments.toMutableList()
        val textBefore = segments.getOrNull(index - 1)
        val textAfter = segments.getOrNull(index + 1)
        segments.removeAt(index)
        if (textBefore is ContentSegment.Text && textAfter is ContentSegment.Text) {
            val beforeMd = markdownResolver(textBefore.id)
            val afterMd = markdownResolver(textAfter.id)
            val separator = if (beforeMd.isEmpty() || afterMd.isEmpty() ||
                beforeMd.endsWith("\n") || afterMd.startsWith("\n")
            ) "" else "\n"
            segments[index - 1] = textBefore.copy(markdown = beforeMd + separator + afterMd)
            segments.removeAt(index)
        }
        _uiState.update { it.copy(segments = segments) }
        persistDraft()
    }

    fun save(markdownResolver: (String) -> String?, onDone: () -> Unit) {
        val note = buildNoteOrNull(markdownResolver)
        if (note == null) {
            clearDraft()
            onDone()
        } else {
            viewModelScope.launch {
                repo.save(note)
                clearDraft()
                onDone()
            }
        }
    }

    /**
     * 手势/系统返回路径：必须同步弹栈（miuix-nav 的 back 提交协议要求 onBack 同步修改栈，
     * 否则 predictive-back 收敛动画会卡在松手进度）。保存改到应用级作用域异步落盘，
     * 避免 entry 销毁后 viewModelScope 取消导致写丢失。
     */
    fun saveAndBack(markdownResolver: (String) -> String?, onBack: () -> Unit) {
        val note = buildNoteOrNull(markdownResolver)
        clearDraft()
        onBack()
        if (note != null) {
            appScope.launch { repo.save(note) }
        }
    }

    private fun buildNoteOrNull(markdownResolver: (String) -> String?): Note? {
        val state = _uiState.value
        if (state.unreadable) return null
        val resolved = state.segments.map { segment ->
            if (segment is ContentSegment.Text) {
                segment.copy(markdown = markdownResolver(segment.id) ?: segment.markdown)
            } else {
                segment
            }
        }
        val allMarkdown = serializeSegmentsToMarkdown(resolved)
        if (state.title.isNotBlank() || allMarkdown.isNotBlank()) {
            return existing?.copy(
                title = state.title,
                content = allMarkdown,
                colorIndex = state.colorIndex,
                images = resolved.filterIsInstance<ContentSegment.Image>().map { it.path },
            ) ?: Note.createNote(
                title = state.title,
                content = allMarkdown,
                colorIndex = state.colorIndex,
            )
        }
        return null
    }

    private fun persistDraft() {
        val state = _uiState.value
        if (state.loading || state.unreadable) return
        savedState["draft_note_id"] = noteId
        savedState["draft_title"] = state.title
        savedState["draft_color"] = state.colorIndex
        savedState["draft_markdown"] = serializeSegmentsToMarkdown(state.segments)
    }

    private fun clearDraft() {
        savedState.remove<String>("draft_note_id")
        savedState.remove<String>("draft_title")
        savedState.remove<String>("draft_color")
        savedState.remove<String>("draft_markdown")
    }
}