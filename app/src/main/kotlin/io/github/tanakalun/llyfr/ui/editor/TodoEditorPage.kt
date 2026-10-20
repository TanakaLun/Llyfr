package io.github.tanakalun.llyfr.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tanakalun.llyfr.LocalIsWideScreen
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.core.AppViewModelFactory
import io.github.tanakalun.llyfr.core.LocalOnBackSink
import io.github.tanakalun.llyfr.data.model.Note
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.LocalNavigator
import io.github.tanakalun.llyfr.ui.util.BackNavigationIcon
import io.github.tanakalun.llyfr.ui.util.adaptiveContentWindowInsets
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodoEditorPage(
    noteId: String,
    viewModel: TodoEditorViewModel = viewModel(factory = AppViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWideScreen = LocalIsWideScreen.current
    val navigator = LocalNavigator.current
    val onBack = { navigator.pop() }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val onBackSink = LocalOnBackSink.current
    DisposableEffect(Unit) {
        onBackSink {
            if (SettingsStore.saveOnBack) viewModel.saveAndBack(onBack) else onBack()
        }
        onDispose { onBackSink(null) }
    }

    var showPicker by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf(false) }

    fun requestSave() {
        if (uiState.reminderAt == 0L) {
            pendingSave = true
            showPicker = true
        } else {
            viewModel.save(onBack)
        }
    }

    val title = uiState.title
    val colorIndex = uiState.colorIndex
    val readOnly = uiState.readOnly
    val reminderText = if (uiState.reminderAt > 0L) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(uiState.reminderAt))
    } else {
        stringResource(R.string.reminder_not_set)
    }

    Scaffold(
        contentWindowInsets = adaptiveContentWindowInsets(isWideScreen),
        topBar = {
            SmallTopAppBar(
                title = stringResource(
                    if (uiState.isNew) R.string.new_todo else R.string.edit_todo
                ),
                color = MiuixTheme.colorScheme.surface,
                defaultWindowInsetsPadding = !isWideScreen,
                navigationIcon = {
                    BackNavigationIcon(onClick = ::requestSave)
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleReadOnly() }) {
                        Icon(
                            imageVector = if (readOnly) MiuixIcons.Unlock else MiuixIcons.Lock,
                            contentDescription = stringResource(R.string.read_only),
                        )
                    }
                    IconButton(onClick = ::requestSave) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = stringResource(R.string.save),
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = scaffoldPadding.calculateTopPadding(),
                    bottom = scaffoldPadding.calculateBottomPadding(),
                )
                .padding(horizontal = 20.dp),
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                ),
                label = stringResource(R.string.todo_field),
                useLabelAsPlaceholder = true,
                singleLine = true,
                readOnly = readOnly,
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = !readOnly,
                        interactionSource = null,
                        indication = null,
                        onClick = {
                            pendingSave = false
                            showPicker = true
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Alarm,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reminder_time),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = reminderText,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                    if (!readOnly) {
                        Text(
                            text = stringResource(R.string.set_reminder),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (showPicker) {
            DateTimePickerSheet(
                show = true,
                initialMillis = uiState.reminderAt.takeIf { it > 0L },
                onConfirm = { at ->
                    viewModel.setReminder(at)
                    showPicker = false
                    if (pendingSave) {
                        pendingSave = false
                        viewModel.save(onBack)
                    }
                },
                onDismiss = {
                    showPicker = false
                    pendingSave = false
                },
            )
        }
    }
}