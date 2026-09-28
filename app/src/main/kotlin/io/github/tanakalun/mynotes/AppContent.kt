package io.github.tanakalun.mynotes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import android.util.Log
import io.github.tanakalun.mynotes.data.NoteRepository
import io.github.tanakalun.mynotes.navigation.Navigator
import io.github.tanakalun.mynotes.navigation.Route
import io.github.tanakalun.mynotes.screens.AboutPage
import io.github.tanakalun.mynotes.screens.ChecklistPage
import io.github.tanakalun.mynotes.screens.ImageViewerPage
import io.github.tanakalun.mynotes.screens.LicensePage
import io.github.tanakalun.mynotes.screens.NoteEditorPage
import io.github.tanakalun.mynotes.screens.NotesListPage
import io.github.tanakalun.mynotes.screens.SettingsPage
import io.github.tanakalun.mynotes.ui.components.liquid.IosLiquidGlassNavigationBar
import io.github.tanakalun.mynotes.utils.rememberBlurBackdrop
import io.github.tanakalun.mynotes.utils.AppBlur
import io.github.tanakalun.mynotes.utils.isPowerSave
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.springAnimateToPage
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppContent() {
    val backStack = rememberNavBackStack<Route>(Route.NotesList)
    val navigator = remember { Navigator(backStack) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val mainPagerState = rememberMainPagerState(pagerState)
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    val navNotesLabel = stringResource(R.string.nav_notes)
    val navSettingsLabel = stringResource(R.string.nav_settings)
    val navigationItems = remember(navNotesLabel, navSettingsLabel) {
        listOf(
            NavigationItem(navNotesLabel, MiuixIcons.Notes),
            NavigationItem(navSettingsLabel, MiuixIcons.Settings),
        )
    }

    val navCornerRadius = rememberNavSystemCornerRadius()
    val backdropColor = MiuixTheme.colorScheme.surface

    val effects = remember(navCornerRadius, backdropColor) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = navCornerRadius,
            dimAmount = 0.5f,
            backdropColor = backdropColor,
        )
    }

    val swipeBackDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        else -> NavSwipeDirection.LeftToRight
    }

    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        transition = NavTransitions.MiuixDefault,
        effects = effects,
    ) {
        entry<Route.NotesList>(swipeDismiss = swipeBackDirection) {
            Home(
                navigationItems = navigationItems,
                mainPagerState = mainPagerState,
                onEditNote = { id -> navigator.push(Route.NoteEditor(id)) },
                onEditChecklist = { id -> navigator.push(Route.ChecklistEditor(id)) },
                onCreateNote = { navigator.push(Route.NoteEditor()) },
                onCreateChecklist = { navigator.push(Route.ChecklistEditor()) },
                onAboutClick = { navigator.push(Route.About) },
            )
        }
        entry<Route.NoteEditor>(swipeDismiss = swipeBackDirection) { route ->
            Scaffold { subPadding ->
                NoteEditorPage(
                    noteId = route.noteId,
                    innerPadding = subPadding,
                    onBack = { navigator.pop() },
                    navigator = navigator,
                )
            }
        }
        entry<Route.ChecklistEditor>(swipeDismiss = swipeBackDirection) { route ->
            Scaffold { subPadding ->
                ChecklistPage(
                    noteId = route.noteId,
                    innerPadding = subPadding,
                    onBack = { navigator.pop() },
                )
            }
        }
        entry<Route.About>(swipeDismiss = swipeBackDirection) {
            Scaffold { subPadding ->
                AboutPage(
                    innerPadding = subPadding,
                    onBack = { navigator.pop() },
                    navigator = navigator,
                )
            }
        }
        entry<Route.License>(swipeDismiss = swipeBackDirection) {
            Scaffold { subPadding ->
                LicensePage(
                    innerPadding = subPadding,
                    onBack = { navigator.pop() },
                )
            }
        }
        entry<Route.ImageViewer>(swipeDismiss = swipeBackDirection) { route ->
            ImageViewerPage(
                imagePath = route.imagePath,
                innerPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                onBack = { navigator.pop() },
            )
        }
    }
}

@Composable
private fun Home(
    navigationItems: List<NavigationItem>,
    mainPagerState: MainPagerState,
    onEditNote: (String) -> Unit,
    onEditChecklist: (String) -> Unit,
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    onAboutClick: () -> Unit,
) {
    val backdrop = rememberBlurBackdrop()
    val powerSave = isPowerSave()
    val blurActive = SettingsStore.enableBlur && backdrop != null && !powerSave
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var importNoteCount by remember { mutableStateOf<Int?>(null) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun dumpError(tag: String, e: Throwable, targetUri: android.net.Uri?) {
        Log.e(tag, "Backup error, targetUri=$targetUri", e)
        try {
            val logFile = java.io.File(context.filesDir, "backup_error_${System.currentTimeMillis()}.log")
            logFile.writeText(
                buildString {
                    appendLine("=== MyNotes Backup Error ===")
                    appendLine("time=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine("tag=$tag")
                    appendLine("targetUri=$targetUri")
                    appendLine("exception=${e::class.java.name}: ${e.message}")
                    appendLine()
                    appendLine(e.stackTraceToString())
                },
            )
            Log.e(tag, "Stack trace written to ${logFile.absolutePath}")
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.export_failed, e.message ?: "", logFile.absolutePath),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            }
        } catch (e2: Exception) {
            Log.e(tag, "Failed to write error log", e2)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.error_generic, e.message ?: ""),
                    withDismissAction = true,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-mynotes-backup"),
    ) { uri ->
        if (uri != null) {
            kotlinx.coroutines.MainScope().launch {
                try {
                    Log.d("MyNotesBackup", "Export started, uri=$uri")
                    val data = NoteRepository.exportData(context)
                    Log.d("MyNotesBackup", "Export data size=${data.size} bytes")
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(data)
                        os.flush()
                    }
                    Log.d("MyNotesBackup", "Export written successfully")
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.export_success, data.size),
                        duration = SnackbarDuration.Short,
                    )
                } catch (e: Exception) {
                    dumpError("MyNotesBackup", e, uri)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            kotlinx.coroutines.MainScope().launch {
                try {
                    Log.d("MyNotesBackup", "Import preview started, uri=$uri")
                    val count = NoteRepository.previewNoteCount(context, uri)
                    Log.d("MyNotesBackup", "Import preview: $count notes")
                    pendingImportUri = uri
                    importNoteCount = count
                } catch (e: Exception) {
                    dumpError("MyNotesBackup", e, uri)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(state = snackbarHostState)
        },
        bottomBar = {
            NavigationBarSection(
                navigationItems = navigationItems,
                mainPagerState = mainPagerState,
                backdrop = backdrop,
                blurActive = blurActive,
                barColor = barColor,
                useFloating = SettingsStore.useFloatingNavbar,
                floatingStyle = SettingsStore.floatingNavbarStyle,
                floatingPosition = SettingsStore.floatingNavbarPosition,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
            ),
        ) {
            HorizontalPager(
                state = mainPagerState.pagerState,
                verticalAlignment = Alignment.Top,
            ) { page ->
                when (page) {
                    0 -> NotesListPage(
                        innerPadding = innerPadding,
                        onEditNote = onEditNote,
                        onEditChecklist = onEditChecklist,
                        onCreateNote = onCreateNote,
                        onCreateChecklist = onCreateChecklist,
                    )
                    1 -> SettingsPage(
                        innerPadding = innerPadding,
                        onAboutClick = onAboutClick,
                        onExportClick = {
                            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                            exportLauncher.launch("mynotes-$dateStr.mnbackup")
                        },
                        onImportClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        importNoteCount = importNoteCount,
                        onImportConfirm = { overwriteSettings ->
                            val uri = pendingImportUri
                            if (uri != null) {
                                kotlinx.coroutines.MainScope().launch {
                                    try {
                                        val count = NoteRepository.importData(context, uri, overwriteSettings)
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.imported_notes, count),
                                            duration = SnackbarDuration.Short,
                                        )
                                    } catch (e: Exception) {
                                        dumpError("MyNotesBackup", e, uri)
                                    }
                                }
                            }
                            importNoteCount = null
                            pendingImportUri = null
                        },
                        onImportDismiss = {
                            importNoteCount = null
                            pendingImportUri = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationBarSection(
    navigationItems: List<NavigationItem>,
    mainPagerState: MainPagerState,
    backdrop: LayerBackdrop?,
    blurActive: Boolean,
    barColor: Color,
    useFloating: Boolean,
    floatingStyle: Int,
    floatingPosition: Int,
) {
    if (useFloating) {
        if (floatingStyle == 1) {
            IosLiquidGlassNavigationBar(
                items = navigationItems,
                selectedIndex = mainPagerState.selectedPage.coerceIn(0, navigationItems.size - 1),
                onItemClick = { index -> mainPagerState.animateToPage(index) },
                backdrop = backdrop,
                isBlurActive = blurActive && backdrop != null,
            )
            return
        }

        val floatingColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surfaceContainer
        val floatingShape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius)
        val isDark = isSystemInDarkTheme()
        val floatingHighlight = remember(isDark) {
            if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight
        }
        FloatingNavigationBar(
            color = floatingColor,
            horizontalAlignment = when (floatingPosition) {
                1 -> Alignment.Start
                2 -> Alignment.End
                else -> Alignment.CenterHorizontally
            },
            modifier = if (blurActive && backdrop != null) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = floatingShape,
                    blurRadius = AppBlur.NAVBAR_RADIUS,
                    colors = BlurDefaults.blurColors(
                        blendColors = listOf(
                            BlendColorEntry(
                                color = MiuixTheme.colorScheme.surfaceContainer.copy(AppBlur.NAVBAR_FLOATING_SURFACE_ALPHA),
                            ),
                        ),
                    ),
                    highlight = floatingHighlight,
                )
            } else {
                Modifier
            },
        ) {
            navigationItems.forEachIndexed { index, item ->
                FloatingNavigationBarItem(
                    selected = mainPagerState.selectedPage == index,
                    onClick = { mainPagerState.animateToPage(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
        return
    }

    Box(
        modifier = if (blurActive && backdrop != null) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = AppBlur.NAVBAR_RADIUS,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(AppBlur.NAVBAR_SURFACE_ALPHA)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        NavigationBar(
            color = barColor,
        ) {
            navigationItems.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = mainPagerState.selectedPage == index,
                    onClick = { mainPagerState.animateToPage(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}

@Stable
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                pagerState.springAnimateToPage(targetIndex)
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): MainPagerState = remember(pagerState, coroutineScope) {
    MainPagerState(pagerState, coroutineScope)
}
