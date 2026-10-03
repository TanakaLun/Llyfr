package io.github.tanakalun.llyfr.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.HeadingStyle
import com.mohamedrejeb.richeditor.model.RichSpanStyle
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.ui.util.adaptiveContentWindowInsets
import io.github.tanakalun.llyfr.LocalIsWideScreen
import io.github.tanakalun.llyfr.core.AppViewModelFactory
import io.github.tanakalun.llyfr.core.LocalOnBackSink
import io.github.tanakalun.llyfr.LocalNavigator
import io.github.tanakalun.llyfr.ui.navigation.Route
import io.github.tanakalun.llyfr.data.model.ContentSegment
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.ui.highlightCode
import io.github.tanakalun.llyfr.ui.util.BackNavigationIcon
import top.yukonga.miuix.kmp.basic.FloatingToolbar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Unlock
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.LocalContentColor

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun NoteEditorPage(
    noteId: String,
    viewModel: NoteEditorViewModel = viewModel(factory = AppViewModelFactory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWideScreen = LocalIsWideScreen.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val onBack = { navigator.pop() }

    LaunchedEffect(noteId) { viewModel.load(noteId) }

    val segments = uiState.segments
    val title = uiState.title
    val colorIndex = uiState.colorIndex
    val readOnly = uiState.readOnly

    val richTextStates = remember { mutableMapOf<String, RichTextState>() }

    DisposableEffect(Unit) {
        onDispose { richTextStates.clear() }
    }

    val markdownResolver: (String) -> String? = { segId -> richTextStates[segId]?.toMarkdown() }

    val onBackSink = LocalOnBackSink.current
    DisposableEffect(Unit) {
        onBackSink {
            if (io.github.tanakalun.llyfr.data.settings.SettingsStore.saveOnBack) {
                viewModel.saveAndBack(markdownResolver, onBack)
            } else {
                onBack()
            }
        }
        onDispose { onBackSink(null) }
    }

    var focusedSegmentId by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        val splitProvider: () -> SplitRequest? = {
            val focusedIdx = focusedSegmentId
                ?.let { id -> segments.indexOfFirst { it.id == id } }
                ?.takeIf { it >= 0 }
            if (focusedIdx == null || focusedIdx >= segments.size) {
                null
            } else {
                val seg = segments[focusedIdx]
                if (seg is ContentSegment.Text) {
                    val state = richTextStates[seg.id]
                    if (state != null) {
                        runCatching {
                            val plain = state.annotatedString.text
                            val caret = state.selection.max
                            val nl = plain.indexOf('\n', caret)
                            val splitPlain = if (nl >= 0) nl + 1 else plain.length
                            if (splitPlain in 1 until plain.length) {
                                val full = state.toMarkdown()
                                val before = state.toMarkdown(TextRange(0, splitPlain))
                                val after = state.toMarkdown(TextRange(splitPlain, plain.length))
                                val stripNewlines = { s: String -> s.replace("\n", "") }
                                if (before.isNotEmpty() && after.isNotEmpty() &&
                                    stripNewlines(full) == stripNewlines(before + after)
                                ) {
                                    SplitRequest(focusedIndex = focusedIdx, before = before, after = after)
                                } else {
                                    SplitRequest(focusedIndex = focusedIdx, before = null, after = null)
                                }
                            } else {
                                SplitRequest(focusedIndex = focusedIdx, before = null, after = null)
                            }
                        }.getOrElse { SplitRequest(focusedIndex = focusedIdx, before = null, after = null) }
                    } else {
                        SplitRequest(focusedIndex = focusedIdx, before = null, after = null)
                    }
                } else {
                    SplitRequest(focusedIndex = focusedIdx, before = null, after = null)
                }
            }
        }
        viewModel.onImagesSelected(uris, splitProvider)
    }

    if (uiState.unreadable) {
        UnreadableNoteScaffold(
            noteId = noteId,
            onBack = onBack,
        )
        return
    }

    Scaffold(
        contentWindowInsets = adaptiveContentWindowInsets(isWideScreen),
        topBar = {
            SmallTopAppBar(
                title = stringResource(
                    if (uiState.isNew) R.string.new_note else R.string.edit_note
                ),
                color = MiuixTheme.colorScheme.surface,
                defaultWindowInsetsPadding = !isWideScreen,
                navigationIcon = {
                    BackNavigationIcon(onClick = { viewModel.save(markdownResolver, onBack) })
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleReadOnly() }) {
                        Icon(
                            imageVector = if (readOnly) MiuixIcons.Unlock else MiuixIcons.Lock,
                            contentDescription = stringResource(R.string.read_only),
                        )
                    }
                    IconButton(onClick = { viewModel.save(markdownResolver, onBack) }) {
                        Icon(
                            imageVector = MiuixIcons.Ok,
                            contentDescription = stringResource(R.string.save),
                        )
                    }
                },
            )
        },
    ) { scaffoldPadding ->
        val topPadding = scaffoldPadding.calculateTopPadding()
        val bottomPadding = scaffoldPadding.calculateBottomPadding()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = topPadding,
                    bottom = bottomPadding,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                        listOf(defaultSwatch) + io.github.tanakalun.llyfr.data.model.Note.COLORS.map { Color(it) }
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
                    label = stringResource(R.string.title_field),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    readOnly = readOnly,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MiuixTheme.colorScheme.secondaryContainer)
                    .padding(16.dp),
            ) {
                val contentColor = LocalContentColor.current

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    segments.forEachIndexed { index, segment ->
                        key(segment.id) {
                            when (segment) {
                                is ContentSegment.Text -> {
                                    val richState = rememberRichTextState()
                                    val focusRequester = remember { FocusRequester() }
                                    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                    val uriHandler = LocalUriHandler.current
                                    val linkColor = MiuixTheme.colorScheme.primary

                                    val pendingFocusId = uiState.pendingFocusId
                                    LaunchedEffect(segment.id, segment.markdown) {
                                        val currentMd = richState.toMarkdown()
                                        if (currentMd != segment.markdown) {
                                            richState.setMarkdown(segment.markdown)
                                        }
                                    }
                                    LaunchedEffect(segment.id, pendingFocusId) {
                                        richTextStates[segment.id] = richState
                                        if (pendingFocusId == segment.id) {
                                            viewModel.consumeFocus(segment.id)
                                            runCatching { focusRequester.requestFocus() }
                                        }
                                    }
                                    LaunchedEffect(richState, linkColor) {
                                        richState.config.linkColor = linkColor
                                    }
                                    BasicRichTextEditor(
                                        state = richState,
                                        readOnly = readOnly,
                                        onTextLayout = { textLayoutResult = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused) {
                                                    focusedSegmentId = segment.id
                                                }
                                            }
                                            .pointerInput(richState, uriHandler) {
                                                awaitPointerEventScope {
                                                    var downPos: Offset? = null
                                                    var downAt = 0L
                                                    while (true) {
                                                        val event =
                                                            awaitPointerEvent(PointerEventPass.Initial)
                                                        val change = event.changes.firstOrNull()
                                                            ?: continue
                                                        val wasPressed = change.previousPressed
                                                        val isPressed = change.pressed
                                                        if (isPressed && !wasPressed) {
                                                            downPos = change.position
                                                            downAt = change.uptimeMillis
                                                        } else if (downPos != null) {
                                                            val dist =
                                                                (change.position - downPos).getDistance()
                                                            if (isPressed) {
                                                                if (dist > viewConfiguration.touchSlop) {
                                                                    downPos = null
                                                                }
                                                            } else if (wasPressed) {
                                                                val pos = downPos
                                                                downPos = null
                                                                val duration =
                                                                    change.uptimeMillis - downAt
                                                                if (dist <= viewConfiguration.touchSlop &&
                                                                    duration < viewConfiguration.longPressTimeoutMillis
                                                                ) {
                                                                    openLinkAtPosition(
                                                                        state = richState,
                                                                        layout = textLayoutResult,
                                                                        position = pos,
                                                                        uriHandler = uriHandler,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        textStyle = TextStyle(
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            color = contentColor,
                                        ),
                                        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                                        decorationBox = @Composable { innerTextField ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.TopStart,
                                            ) {
                                                if (richState.annotatedString.isEmpty() && segments.size == 1) {
                                                    Text(
                                                        text = stringResource(R.string.start_writing),
                                                        style = MiuixTheme.textStyles.body1.copy(
                                                            fontSize = 16.sp,
                                                            lineHeight = 24.sp,
                                                            color = contentColor.copy(alpha = 0.7f),
                                                        ),
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        },
                                    )
                                }
                                is ContentSegment.Code -> {
                                    val codeBlockWrap = SettingsStore.codeBlockWrap
                                    val highlighted = remember(segment.code, segment.language) {
                                        highlightCode(segment.code, segment.language)
                                    }
                                    val codeScrollState = rememberScrollState()

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MiuixTheme.colorScheme.surfaceContainer)
                                            .padding(12.dp),
                                    ) {
                                        if (segment.language.isNotBlank()) {
                                            Text(
                                                text = segment.language,
                                                style = MiuixTheme.textStyles.footnote1.copy(
                                                    fontSize = 11.sp,
                                                ),
                                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                                modifier = Modifier.padding(bottom = 4.dp),
                                            )
                                        }
                                        SelectionContainer {
                                            if (codeBlockWrap) {
                                                Text(
                                                    text = highlighted,
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        color = contentColor,
                                                    ),
                                                )
                                            } else {
                                                Text(
                                                    text = highlighted,
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        color = contentColor,
                                                    ),
                                                    modifier = Modifier.horizontalScroll(codeScrollState),
                                                    maxLines = Int.MAX_VALUE,
                                                )
                                            }
                                        }
                                    }
                                }
                                is ContentSegment.Image -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(segment.path)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.note_image),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(
                                                    if (segment.width > 0 && segment.height > 0) {
                                                        segment.width.toFloat() / segment.height.toFloat()
                                                    } else {
                                                        16f / 9f
                                                    }
                                                )
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { navigator.push(Route.ImageViewer(segment.path)) },
                                            contentScale = ContentScale.Crop,
                                        )
                                        IconButton(
                                            onClick = {
                                                if (readOnly) return@IconButton
                                                viewModel.removeImage(index, { id ->
                                                    richTextStates[id]?.toMarkdown() ?: ""
                                                })
                                            },
                                            enabled = !readOnly,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Delete,
                                                contentDescription = stringResource(R.string.delete_image),
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!readOnly) {
                FloatingToolbar(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    cornerRadius = 20.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormatButton(label = "B", description = stringResource(R.string.toolbar_bold)) {
                            focusedSegmentId?.let { segId ->
                                richTextStates[segId]?.toggleSpanStyle(
                                    SpanStyle(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        FormatButton(label = "I", description = stringResource(R.string.toolbar_italic)) {
                            focusedSegmentId?.let { segId ->
                                richTextStates[segId]?.toggleSpanStyle(
                                    SpanStyle(fontStyle = FontStyle.Italic)
                                )
                            }
                        }
                        FormatButton(label = "U", description = stringResource(R.string.toolbar_underline)) {
                            focusedSegmentId?.let { segId ->
                                richTextStates[segId]?.toggleSpanStyle(
                                    SpanStyle(textDecoration = TextDecoration.Underline)
                                )
                            }
                        }
                        FormatButton(label = "H1", description = stringResource(R.string.toolbar_heading1)) {
                            focusedSegmentId?.let { segId ->
                                richTextStates[segId]?.setHeadingStyle(HeadingStyle.H1)
                            }
                        }
                        FormatButton(label = "H2", description = stringResource(R.string.toolbar_heading2)) {
                            focusedSegmentId?.let { segId ->
                                richTextStates[segId]?.setHeadingStyle(HeadingStyle.H2)
                            }
                        }
                        FormatButton(label = "•", description = stringResource(R.string.toolbar_unordered_list)) {
                            focusedSegmentId?.let { segId -> richTextStates[segId]?.toggleUnorderedList() }
                        }
                        FormatButton(label = "1.", description = stringResource(R.string.toolbar_ordered_list)) {
                            focusedSegmentId?.let { segId -> richTextStates[segId]?.toggleOrderedList() }
                        }
                        FormatIconButton(
                            description = stringResource(R.string.add_image),
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun UnreadableNoteScaffold(
    noteId: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.edit_note),
                color = MiuixTheme.colorScheme.surface,
                navigationIcon = {
                    BackNavigationIcon(onClick = onBack)
                },
            )
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.note_unreadable_title),
                style = MiuixTheme.textStyles.title1,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.note_unreadable_hint),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                text = stringResource(R.string.back_to_list),
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FormatIconButton(
    description: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        Icon(
            imageVector = MiuixIcons.AddCircle,
            contentDescription = description,
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalRichTextApi::class)
private fun openLinkAtPosition(
    state: RichTextState,
    layout: TextLayoutResult?,
    position: Offset,
    uriHandler: UriHandler,
) {
    val layoutResult = layout ?: return
    val length = layoutResult.layoutInput.text.length
    if (length == 0) return
    val index = layoutResult.getOffsetForPosition(position)
    val candidates = listOf(index, index - 1).filter { it in 0 until length }
    for (i in candidates) {
        val span = state.getRichSpanStyle(TextRange(i, i + 1))
        if (span is RichSpanStyle.Link) {
            openUrl(uriHandler, span.url)
            return
        }
    }
}

private fun openUrl(uriHandler: UriHandler, url: String) {
    val target = if (url.contains("://")) url else "https://$url"
    try {
        uriHandler.openUri(target)
    } catch (_: Exception) {
    }
}