package io.github.tanakalun.mynotes.ui.notes

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tanakalun.mynotes.R
import io.github.tanakalun.mynotes.core.AppViewModelFactory
import io.github.tanakalun.mynotes.data.model.Note
import io.github.tanakalun.mynotes.data.model.NoteType
import io.github.tanakalun.mynotes.data.settings.SettingsStore
import io.github.tanakalun.mynotes.ui.util.BlurredBar
import io.github.tanakalun.mynotes.ui.util.rememberBlurBackdrop
import io.github.tanakalun.mynotes.util.stripMarkdown
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesListPage(
    innerPadding: PaddingValues,
    onEditNote: (String) -> Unit,
    onEditChecklist: (String) -> Unit,
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    viewModel: NotesListViewModel = viewModel(factory = AppViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notes = uiState.notes
    val searchQuery = uiState.query
    val selectionMode = uiState.selectionMode
    val selectedIds = uiState.selectedIds

    var searchActive by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showNewDialog by remember { mutableStateOf(false) }

    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    val topAppBarScrollBehavior = MiuixScrollBehavior()

    OverlayDialog(
        show = showBatchDialog,
        title = stringResource(R.string.batch_actions),
        summary = pluralStringResource(
            R.plurals.selected_count,
            selectedIds.size,
            selectedIds.size,
        ),
        onDismissRequest = { showBatchDialog = false },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = stringResource(R.string.delete),
                    onClick = {
                        viewModel.deleteSelected()
                        showBatchDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        disabledColor = MiuixTheme.colorScheme.errorContainer,
                        textColor = MiuixTheme.colorScheme.onError,
                        disabledTextColor = MiuixTheme.colorScheme.onErrorContainer,
                    ),
                )
                TextButton(
                    text = stringResource(R.string.pin),
                    onClick = {
                        viewModel.togglePinSelected()
                        showBatchDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        disabledColor = MiuixTheme.colorScheme.disabledPrimaryButton,
                        textColor = MiuixTheme.colorScheme.onPrimary,
                        disabledTextColor = MiuixTheme.colorScheme.disabledOnPrimaryButton,
                    ),
                )
            }
            TextButton(
                text = stringResource(R.string.cancel),
                onClick = { showBatchDialog = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    OverlayDialog(
        show = showNewDialog,
        title = stringResource(R.string.new_item),
        summary = stringResource(R.string.choose_item_type),
        onDismissRequest = { showNewDialog = false },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = stringResource(R.string.note),
                    onClick = {
                        showNewDialog = false
                        onCreateNote()
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        disabledColor = MiuixTheme.colorScheme.primaryContainer,
                        textColor = MiuixTheme.colorScheme.onPrimary,
                        disabledTextColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                TextButton(
                    text = stringResource(R.string.checklist),
                    onClick = {
                        showNewDialog = false
                        onCreateChecklist()
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextButtonColors(
                        color = MiuixTheme.colorScheme.primary,
                        disabledColor = MiuixTheme.colorScheme.primaryContainer,
                        textColor = MiuixTheme.colorScheme.onPrimary,
                        disabledTextColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            TextButton(
                text = stringResource(R.string.cancel),
                onClick = { showNewDialog = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Scaffold(
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                TopAppBar(
                    title = if (selectionMode) {
                        pluralStringResource(
                            R.plurals.selected_count,
                            selectedIds.size,
                            selectedIds.size,
                        )
                    } else {
                        "MyNotes"
                    },
                    color = barColor,
                    scrollBehavior = topAppBarScrollBehavior,
                    actions = {
                        if (selectionMode) {
                            IconButton(onClick = { viewModel.exitSelection() }) {
                                Icon(
                                    imageVector = MiuixIcons.Close,
                                    contentDescription = stringResource(R.string.cancel),
                                )
                            }
                            IconButton(
                                onClick = { if (selectedIds.isNotEmpty()) showBatchDialog = true },
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = stringResource(R.string.done),
                                )
                            }
                        }
                    },
                    bottomContent = {
                        if (SettingsStore.showSearchBar) {
                            InputField(
                                query = searchQuery,
                                onQueryChange = { viewModel.onQueryChange(it) },
                                onSearch = { searchActive = false },
                                expanded = searchActive,
                                onExpandedChange = { searchActive = it },
                                label = stringResource(R.string.search_notes),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 6.dp),
                            )
                        }
                    },
                )
            }
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        Box(
            modifier = Modifier.fillMaxSize().then(
                if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
            ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .scrollEndHaptic()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = bottomPadding + 80.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (notes.isEmpty()) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.no_matching_notes)
                                } else {
                                    stringResource(R.string.no_notes_yet)
                                },
                                style = MiuixTheme.textStyles.body1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }

                items(notes, key = { it.id }) { note ->
                    val selected = note.id in selectedIds
                    val handleClick = {
                        if (selectionMode) {
                            viewModel.toggleSelect(note.id)
                        } else {
                            when (viewModel.onOpenRequested(note)) {
                                NoteType.Note -> onEditNote(note.id)
                                NoteType.Checklist -> onEditChecklist(note.id)
                            }
                        }
                    }
                    val handleLongPress = { viewModel.enterSelection(note.id) }
                    when (note.type) {
                        NoteType.Note -> NoteCard(
                            note = note,
                            selected = selected,
                            onClick = handleClick,
                            onLongPress = handleLongPress,
                        )
                        NoteType.Checklist -> ChecklistSummaryCard(
                            note = note,
                            selected = selected,
                            onClick = handleClick,
                            onLongPress = handleLongPress,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (!selectionMode) {
                FloatingActionButton(
                    onClick = { showNewDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = bottomPadding + 12.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = stringResource(R.string.new_item),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun noteCardColors(colorIndex: Int): CardColors {
    val hex = Note.colorHexOrNull(colorIndex) ?: return CardDefaults.defaultColors()
    val background = Color(hex)
    val contentColor = if (background.luminance() > 0.5f) {
        Color(0xF0000000)
    } else {
        Color.White
    }
    return CardDefaults.defaultColors(color = background, contentColor = contentColor)
}

@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val untitledText = stringResource(R.string.untitled)
    val imageCountText = pluralStringResource(R.plurals.images_count, note.images.size, note.images.size)
    val cardColors = noteCardColors(note.colorIndex)

    top.yukonga.miuix.kmp.basic.Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MiuixTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        onLongPress = onLongPress,
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = note.title.ifBlank { untitledText },
                    style = MiuixTheme.textStyles.title1.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    ),
                    color = cardColors.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (note.pinned) {
                    Icon(
                        imageVector = MiuixIcons.Pin,
                        contentDescription = null,
                        tint = cardColors.contentColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp),
                    )
                }
            }
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stripMarkdown(note.content),
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp),
                    color = cardColors.contentColor.copy(alpha = 0.75f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (note.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Image,
                        contentDescription = null,
                        tint = cardColors.contentColor.copy(alpha = 0.75f),
                        modifier = Modifier.height(14.dp),
                    )
                    Text(
                        text = imageCountText,
                        style = MiuixTheme.textStyles.body1.copy(fontSize = 12.sp),
                        color = cardColors.contentColor.copy(alpha = 0.75f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(note.updatedAt)),
                style = MiuixTheme.textStyles.footnote1,
                color = cardColors.contentColor.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ChecklistSummaryCard(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val checkedCount = remember(note.checklist) { note.checklist.count { it.checked } }
    val totalCount = note.checklist.size
    val untitledText = stringResource(R.string.untitled)
    val cardColors = noteCardColors(note.colorIndex)

    top.yukonga.miuix.kmp.basic.Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MiuixTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                    )
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        onLongPress = onLongPress,
        colors = cardColors,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Tasks,
                        contentDescription = null,
                        tint = if (Note.colorHexOrNull(note.colorIndex) != null) {
                            cardColors.contentColor
                        } else {
                            MiuixTheme.colorScheme.primary
                        },
                    )
                    Text(
                        text = note.title.ifBlank { untitledText },
                        style = MiuixTheme.textStyles.title1.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                        ),
                        color = cardColors.contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (note.pinned) {
                    Icon(
                        imageVector = MiuixIcons.Pin,
                        contentDescription = null,
                        tint = cardColors.contentColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp),
                    )
                }
            }
            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$checkedCount / $totalCount",
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 14.sp),
                    color = cardColors.contentColor.copy(alpha = 0.75f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(note.updatedAt)),
                style = MiuixTheme.textStyles.footnote1,
                color = cardColors.contentColor.copy(alpha = 0.55f),
            )
        }
    }
}