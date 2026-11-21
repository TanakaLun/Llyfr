package io.github.tanakalun.llyfr.ui.editor

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.ui.util.adaptiveContentWindowInsets
import io.github.tanakalun.llyfr.LocalIsWideScreen
import io.github.tanakalun.llyfr.core.AppViewModelFactory
import io.github.tanakalun.llyfr.core.LocalOnBackSink
import io.github.tanakalun.llyfr.data.model.CheckItem
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.LocalNavigator
import io.github.tanakalun.llyfr.ui.util.BackNavigationIcon
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.glass.Add
import top.yukonga.miuix.kmp.icon.glass.Ok
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.squircle.squircleBackground
import io.github.tanakalun.llyfr.ui.components.glass.AppButtonRole
import io.github.tanakalun.llyfr.ui.components.glass.AppDialog
import io.github.tanakalun.llyfr.ui.components.glass.AppDialogButton
import io.github.tanakalun.llyfr.ui.components.glass.AppIconButton
import io.github.tanakalun.llyfr.ui.components.glass.AppTextIconButton
import io.github.tanakalun.llyfr.ui.components.glass.AppTopAppBar
import io.github.tanakalun.llyfr.ui.components.glass.isGlassEffectActive
import io.github.tanakalun.llyfr.ui.components.glass.rememberSurfaceBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassFloatingActionButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChecklistPage(
    noteId: String,
    viewModel: ChecklistViewModel = viewModel(factory = AppViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWideScreen = LocalIsWideScreen.current
    val navigator = LocalNavigator.current
    val onBack = { navigator.pop() }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val onBackSink = LocalOnBackSink.current
    DisposableEffect(Unit) {
        onBackSink {
            if (SettingsStore.saveOnBack) {
                viewModel.saveAndBack(onBack)
            } else {
                onBack()
            }
        }
        onDispose { onBackSink(null) }
    }

    val title = uiState.title
    val colorIndex = uiState.colorIndex
    val items = uiState.items
    val readOnly = uiState.readOnly

    var deleteMode by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<CheckItem?>(null) }

    val backdrop = rememberSurfaceBackdrop()
    val glass = isGlassEffectActive()
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    AppDialog(
        show = itemToDelete != null,
        title = stringResource(R.string.delete_title),
        summary = stringResource(R.string.delete_item_confirm),
        onDismissRequest = { itemToDelete = null },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppDialogButton(
                text = stringResource(R.string.cancel),
                onClick = { itemToDelete = null },
                modifier = Modifier.weight(1f),
            )
            AppDialogButton(
                text = stringResource(R.string.delete),
                onClick = {
                    itemToDelete?.let { viewModel.removeItem(it.id) }
                    itemToDelete = null
                },
                modifier = Modifier.weight(1f),
                role = AppButtonRole.Destructive,
            )
        }
    }

    Scaffold(
        contentWindowInsets = adaptiveContentWindowInsets(isWideScreen),
        topBar = {
            AppTopAppBar(
                title = stringResource(
                    if (uiState.isNew) R.string.new_checklist else R.string.edit_checklist
                ),
                backdrop = backdrop,
                large = false,
                defaultWindowInsetsPadding = !isWideScreen,
                navigationIcon = {
                    BackNavigationIcon(onClick = {
                        deleteMode = false
                        viewModel.save(onBack)
                    })
                },
                actions = {
                    AppIconButton(
                        icon = if (readOnly) MiuixIcons.Unlock else MiuixIcons.Lock,
                        contentDescription = stringResource(R.string.read_only),
                        onClick = { viewModel.toggleReadOnly() },
                        backdrop = backdrop,
                    )
                    if (deleteMode) {
                        AppTextIconButton(
                            text = stringResource(R.string.done),
                            onClick = { deleteMode = false },
                        )
                    } else {
                        AppIconButton(
                            icon = if (glass) MiuixIcons.Glass.Ok else MiuixIcons.Ok,
                            contentDescription = stringResource(R.string.save),
                            onClick = { viewModel.save(onBack) },
                            backdrop = backdrop,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!deleteMode && !readOnly) {
                if (glass) {
                    GlassFloatingActionButton(
                        onClick = { viewModel.addItem() },
                        backdrop = backdrop,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Glass.Add,
                            contentDescription = stringResource(R.string.add_item),
                            modifier = Modifier.size(24.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    FloatingActionButton(onClick = { viewModel.addItem() }) {
                        Icon(
                            imageVector = MiuixIcons.Add,
                            contentDescription = stringResource(R.string.add_item),
                            tint = MiuixTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = scaffoldPadding.calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
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
                                onClick = { if (!readOnly) viewModel.onColorChange(index) },
                            ),
                    )
                }
            }

            TextField(
                value = title,
                onValueChange = { if (!readOnly) viewModel.onTitleChange(it) },
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
                                ToggleableState.On
                            } else {
                                ToggleableState.Off
                            },
                            onClick = { if (!readOnly) viewModel.toggleItemChecked(item.id) },
                            enabled = !readOnly,
                        )

                        Spacer(modifier = Modifier.padding(start = 6.dp))

                        BasicTextField(
                            value = item.text,
                            onValueChange = { if (!readOnly) viewModel.updateItemText(item.id, it) },
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
                                        viewModel.addItem()
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
                            AppIconButton(
                                icon = MiuixIcons.Delete,
                                contentDescription = stringResource(R.string.delete),
                                onClick = { itemToDelete = item },
                                tint = MiuixTheme.colorScheme.error,
                                backdrop = backdrop,
                            )
                        }
                    }
                }
            }
        }
        }
    }
}