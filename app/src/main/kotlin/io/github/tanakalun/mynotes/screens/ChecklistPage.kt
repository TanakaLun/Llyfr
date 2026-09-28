package io.github.tanakalun.mynotes.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tanakalun.mynotes.R
import io.github.tanakalun.mynotes.SettingsStore
import io.github.tanakalun.mynotes.data.CheckItem
import io.github.tanakalun.mynotes.data.Note
import io.github.tanakalun.mynotes.data.NoteRepository
import io.github.tanakalun.mynotes.data.NoteType
import io.github.tanakalun.mynotes.utils.BackNavigationIcon
import io.github.tanakalun.mynotes.utils.BlurredBar
import io.github.tanakalun.mynotes.utils.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.blur.layerBackdrop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistPage(
    noteId: String,
    innerPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val existing = remember(noteId) {
        if (noteId.isNotBlank()) NoteRepository.getById(noteId) else null
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var colorIndex by remember { mutableIntStateOf(existing?.colorIndex ?: 0) }
    var items by remember {
        mutableStateOf(
            existing?.checklist?.ifEmpty {
                listOf(CheckItem(id = "${System.currentTimeMillis()}_0", text = ""))
            } ?: listOf(CheckItem(id = "${System.currentTimeMillis()}_0", text = ""))
        )
    }
    var deleteMode by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<CheckItem?>(null) }
    var readOnly by remember { mutableStateOf(false) }

    val isNew = existing == null

    fun saveAndBack() {
        val nonEmptyItems = items.filter { it.text.isNotBlank() }
        val note = existing?.copy(
            title = title,
            checklist = nonEmptyItems,
            colorIndex = colorIndex,
        ) ?: Note(
            id = System.currentTimeMillis().toString(),
            type = NoteType.Checklist,
            title = title,
            content = "",
            checklist = nonEmptyItems,
            colorIndex = colorIndex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        if (title.isNotBlank() || nonEmptyItems.isNotEmpty()) {
            NoteRepository.upsert(note)
        }
        onBack()
    }

    fun addItem() {
        val newId = "${System.currentTimeMillis()}_${items.size}"
        items = items + CheckItem(id = newId, text = "")
    }

    fun updateItemText(id: String, newText: String) {
        items = items.map { if (it.id == id) it.copy(text = newText) else it }
    }

    fun toggleItemChecked(id: String) {
        items = items.map { if (it.id == id) it.copy(checked = !it.checked) else it }
    }

    fun removeItem(id: String) {
        items = items.filter { it.id != id }
        if (items.isEmpty()) {
            items = listOf(CheckItem(id = "${System.currentTimeMillis()}_0", text = ""))
        }
    }

    val backdrop = rememberBlurBackdrop()
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface

    OverlayDialog(
        show = itemToDelete != null,
        title = stringResource(R.string.delete_title),
        summary = stringResource(R.string.delete_item_confirm),
        onDismissRequest = { itemToDelete = null },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                text = stringResource(R.string.cancel),
                onClick = { itemToDelete = null },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.delete),
                onClick = {
                    itemToDelete?.let { removeItem(it.id) }
                    itemToDelete = null
                },
                modifier = Modifier.weight(1f),
                colors = TextButtonColors(
                    color = MiuixTheme.colorScheme.error,
                    disabledColor = MiuixTheme.colorScheme.errorContainer,
                    textColor = MiuixTheme.colorScheme.onError,
                    disabledTextColor = MiuixTheme.colorScheme.onErrorContainer,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop = backdrop, scrollBehavior = topAppBarScrollBehavior) {
                SmallTopAppBar(
                    title = stringResource(
                        if (isNew) R.string.new_checklist else R.string.edit_checklist
                    ),
                    color = barColor,
                    navigationIcon = {
                        BackNavigationIcon(onClick = {
                            deleteMode = false
                            saveAndBack()
                        })
                    },
                    actions = {
                        IconButton(onClick = { readOnly = !readOnly }) {
                            Icon(
                                imageVector = if (readOnly) MiuixIcons.Unlock else MiuixIcons.Lock,
                                contentDescription = stringResource(R.string.read_only),
                            )
                        }
                        if (deleteMode) {
                            IconButton(onClick = { deleteMode = false }) {
                                Text(
                                    text = stringResource(R.string.done),
                                    style = MiuixTheme.textStyles.body1,
                                    color = MiuixTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            IconButton(onClick = ::saveAndBack) {
                                Icon(
                                    imageVector = MiuixIcons.Ok,
                                    contentDescription = stringResource(R.string.save),
                                )
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!deleteMode && !readOnly) {
                FloatingActionButton(onClick = ::addItem) {
                    Icon(
                        imageVector = MiuixIcons.Add,
                        contentDescription = stringResource(R.string.add_item),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(
                    top = topPadding,
                    bottom = bottomPadding + 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    10.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val defaultSwatch = MiuixTheme.colorScheme.surfaceContainer
                val outlineColor = MiuixTheme.colorScheme.outline
                val swatchColors = remember(defaultSwatch) {
                    listOf(defaultSwatch) + Note.COLORS.map { Color(it) }
                }
                swatchColors.forEachIndexed { index, color ->
                    val selected = colorIndex == index
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        shape = CircleShape,
                                    )
                                } else {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = outlineColor.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                    )
                                },
                            )
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = { if (!readOnly) colorIndex = index },
                            ),
                    )
                }
            }

            TextField(
                value = title,
                onValueChange = { if (!readOnly) title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textStyle = MiuixTheme.textStyles.title1.copy(
                    fontSize = 22.sp,
                ),
                label = stringResource(R.string.title_field),
                useLabelAsPlaceholder = true,
                singleLine = true,
                readOnly = readOnly,
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .scrollEndHaptic(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    val focusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .squircleBackground(
                                color = MiuixTheme.colorScheme.secondaryContainer,
                                cornerRadius = 16.dp,
                            )
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (!deleteMode && !readOnly) {
                                        deleteMode = true
                                    }
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            state = if (item.checked) {
                                androidx.compose.ui.state.ToggleableState.On
                            } else {
                                androidx.compose.ui.state.ToggleableState.Off
                            },
                            onClick = { if (!readOnly) toggleItemChecked(item.id) },
                            enabled = !readOnly,
                        )

                        Spacer(modifier = Modifier.padding(start = 6.dp))

                        BasicTextField(
                            value = item.text,
                            onValueChange = { if (!readOnly) updateItemText(item.id, it) },
                            readOnly = readOnly,
                            enabled = !readOnly,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (SettingsStore.enterCreatesItem) {
                                        addItem()
                                        focusManager.clearFocus()
                                    }
                                },
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.height(44.dp),
                                ) {
                                    if (item.text.isEmpty()) {
                                        Text(
                                            text = stringResource(
                                                R.string.item_placeholder,
                                                items.indexOf(item) + 1,
                                            ),
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            ),
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        if (deleteMode) {
                            IconButton(onClick = { itemToDelete = item }) {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MiuixTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
