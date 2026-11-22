package io.github.tanakalun.llyfr.ui.notes

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.ui.util.adaptiveContentWindowInsets
import io.github.tanakalun.llyfr.LocalIsWideScreen
import io.github.tanakalun.llyfr.core.AppViewModelFactory
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.model.NoteType
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.ui.components.glass.AppBottomSheet
import io.github.tanakalun.llyfr.ui.components.glass.AppButtonRole
import io.github.tanakalun.llyfr.ui.components.glass.AppDialog
import io.github.tanakalun.llyfr.ui.components.glass.AppDialogButton
import io.github.tanakalun.llyfr.ui.components.glass.AppIconButton
import io.github.tanakalun.llyfr.ui.components.glass.AppSegmentedTabRow
import io.github.tanakalun.llyfr.ui.components.glass.AppTopAppBar
import io.github.tanakalun.llyfr.ui.components.glass.sheetCardColors
import io.github.tanakalun.llyfr.ui.components.glass.isGlassEffectActive
import io.github.tanakalun.llyfr.ui.components.glass.rememberSurfaceBackdrop
import io.github.tanakalun.llyfr.util.stripMarkdown
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.glass.Add
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.icon.glass.Ok
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.glass.GlassFloatingActionButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
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
    onEditTodo: (String) -> Unit,
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    onCreateTodo: () -> Unit,
    viewModel: NotesListViewModel = viewModel(factory = AppViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWideScreen = LocalIsWideScreen.current
    val notes = uiState.notes
    val searchQuery = uiState.query
    val selectionMode = uiState.selectionMode
    val selectedIds = uiState.selectedIds

    var searchActive by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showNewDialog by remember { mutableStateOf(false) }
    var typeFilter by rememberSaveable { mutableIntStateOf(0) }
    val glass = isGlassEffectActive()

    val backdrop = rememberSurfaceBackdrop()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val contentUnderTopBar by remember(topAppBarScrollBehavior) {
        derivedStateOf {
            topAppBarScrollBehavior.state.contentOffset < -1f ||
                topAppBarScrollBehavior.state.overscrollOffset < -1f
        }
    }

    val filterTabs = listOf(
        stringResource(R.string.filter_all),
        stringResource(R.string.filter_note),
        stringResource(R.string.filter_checklist),
        stringResource(R.string.filter_todo),
    )
    val visibleNotes = remember(notes, typeFilter) {
        when (typeFilter) {
            1 -> notes.filter { it.type == NoteType.Note }
            2 -> notes.filter { it.type == NoteType.Checklist }
            3 -> notes.filter { it.type == NoteType.Todo }
            else -> notes
        }
    }

    AppDialog(
        show = showBatchDialog,
        title = stringResource(R.string.batch_actions),
        summary = pluralStringResource(
            R.plurals.selected_count,
            selectedIds.size,
            selectedIds.size,
        ),
        onDismissRequest = { showBatchDialog = false },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppDialogButton(
                text = stringResource(R.string.delete),
                onClick = {
                    viewModel.deleteSelected()
                    showBatchDialog = false
                },
                modifier = Modifier.weight(1f),
                role = AppButtonRole.Destructive,
            )
            AppDialogButton(
                text = stringResource(R.string.pin),
                onClick = {
                    viewModel.togglePinSelected()
                    showBatchDialog = false
                },
                modifier = Modifier.weight(1f),
                role = AppButtonRole.Primary,
            )
        }
        AppDialogButton(
            text = stringResource(R.string.cancel),
            onClick = { showBatchDialog = false },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Scaffold(
        contentWindowInsets = adaptiveContentWindowInsets(isWideScreen),
        topBar = {
            AppTopAppBar(
                title = if (selectionMode) {
                    pluralStringResource(
                        R.plurals.selected_count,
                        selectedIds.size,
                        selectedIds.size,
                    )
                } else {
                    "Llyfr"
                },
                backdrop = backdrop,
                defaultWindowInsetsPadding = !isWideScreen,
                scrollBehavior = topAppBarScrollBehavior,
                actions = {
                    if (selectionMode) {
                        AppIconButton(
                            icon = MiuixIcons.Glass.Close,
                            contentDescription = stringResource(R.string.cancel),
                            onClick = { viewModel.exitSelection() },
                        )
                        AppIconButton(
                            icon = MiuixIcons.Glass.Ok,
                            contentDescription = stringResource(R.string.done),
                            onClick = { if (selectedIds.isNotEmpty()) showBatchDialog = true },
                        )
                    }
                },
                bottomContent = {
                    Column {
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
                        AppSegmentedTabRow(
                            tabs = filterTabs,
                            selectedIndex = typeFilter,
                            onSelect = { typeFilter = it },
                            backdrop = backdrop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 8.dp),
                            surfaceAlpha = if (contentUnderTopBar) 1f else 0f,
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            // 只录制列表内容：FAB 必须留在录制层外，否则玻璃按钮会采样包含自己的 layer
            // 造成 RenderNode 自引用，RenderThread 会栈溢出崩溃。
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
                if (visibleNotes.isEmpty()) {
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

                items(visibleNotes, key = { it.id }) { note ->
                    val selected = note.id in selectedIds
                    val handleClick = {
                        if (selectionMode) {
                            viewModel.toggleSelect(note.id)
                        } else {
                            when (viewModel.onOpenRequested(note)) {
                                NoteType.Note -> onEditNote(note.id)
                                NoteType.Checklist -> onEditChecklist(note.id)
                                NoteType.Todo -> onEditTodo(note.id)
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
                        NoteType.Todo -> TodoSummaryCard(
                            note = note,
                            selected = selected,
                            onClick = handleClick,
                            onLongPress = handleLongPress,
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
            }

            AppBottomSheet(
                show = showNewDialog,
                title = stringResource(R.string.new_item),
                startAction = {
                    AppIconButton(
                        icon = MiuixIcons.Glass.Close,
                        contentDescription = stringResource(R.string.cancel),
                        onClick = { showNewDialog = false },
                    )
                },
                onDismissRequest = { showNewDialog = false },
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = sheetCardColors(),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.note),
                        summary = stringResource(R.string.new_item_note_summary),
                        startAction = {
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                Icon(
                                    imageVector = MiuixIcons.Edit,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            showNewDialog = false
                            onCreateNote()
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.checklist),
                        summary = stringResource(R.string.new_item_checklist_summary),
                        startAction = {
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                Icon(
                                    imageVector = MiuixIcons.Tasks,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            showNewDialog = false
                            onCreateChecklist()
                        },
                    )
                    ArrowPreference(
                        title = stringResource(R.string.todo),
                        summary = stringResource(R.string.new_item_todo_summary),
                        startAction = {
                            Box(modifier = Modifier.padding(end = 8.dp)) {
                                Icon(
                                    imageVector = MiuixIcons.Alarm,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            showNewDialog = false
                            onCreateTodo()
                        },
                    )
                }
            }

            if (!selectionMode) {
                if (glass) {
                    GlassFloatingActionButton(
                        onClick = { showNewDialog = true },
                        backdrop = backdrop,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = bottomPadding + 12.dp),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Glass.Add,
                            contentDescription = stringResource(R.string.new_item),
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = { showNewDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = bottomPadding + 12.dp),
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Glass.Add,
                            contentDescription = stringResource(R.string.new_item),
                            tint = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
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
@Composable
private fun TodoSummaryCard(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val untitledText = stringResource(R.string.untitled)
    val cardColors = noteCardColors(note.colorIndex)
    val reminderText = if (note.reminderAt > 0L) {
        dateFormat.format(Date(note.reminderAt))
    } else {
        stringResource(R.string.reminder_not_set)
    }

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
                    maxLines = 2,
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
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.Alarm,
                    contentDescription = null,
                    tint = cardColors.contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = reminderText,
                    style = MiuixTheme.textStyles.body1.copy(fontSize = 13.sp),
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
