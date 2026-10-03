package io.github.tanakalun.llyfr.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.core.AppViewModelFactory
import io.github.tanakalun.llyfr.LocalIsWideScreen
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.ui.components.liquid.IosLiquidGlassNavigationBar
import io.github.tanakalun.llyfr.ui.notes.NotesListPage
import io.github.tanakalun.llyfr.ui.settings.BackupDialogState
import io.github.tanakalun.llyfr.ui.settings.BusyDialog
import io.github.tanakalun.llyfr.ui.settings.BusyOp
import io.github.tanakalun.llyfr.ui.settings.ExportPasswordDialog
import io.github.tanakalun.llyfr.ui.settings.ImportConfirmDialog
import io.github.tanakalun.llyfr.ui.settings.ImportPasswordDialog
import io.github.tanakalun.llyfr.ui.settings.SettingsPage
import io.github.tanakalun.llyfr.ui.settings.SettingsViewModel
import io.github.tanakalun.llyfr.ui.util.AppBlur
import io.github.tanakalun.llyfr.ui.util.isPowerSave
import io.github.tanakalun.llyfr.ui.util.rememberBlurBackdrop
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
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Home(
    navigationItems: List<NavigationItem>,
    mainPagerState: MainPagerState,
    onEditNote: (String) -> Unit,
    onEditChecklist: (String) -> Unit,
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    onAboutClick: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelFactory),
) {
    val isWideScreen = LocalIsWideScreen.current
    val backdrop = rememberBlurBackdrop()
    val powerSave = isPowerSave()
    val blurActive = SettingsStore.enableBlur && backdrop != null && !powerSave
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(
                message = message,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-llyfr-backup"),
    ) { uri ->
        if (uri != null) settingsViewModel.onExportUri(uri)
        else settingsViewModel.onExportCancelled()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) settingsViewModel.onImportUri(uri)
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(state = snackbarHostState)
        },
        bottomBar = {
            // 宽屏由左侧 rail 承担导航，隐藏底部导航条
            if (!isWideScreen) {
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
            }
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
                            settingsViewModel.onAskExportPassword()
                        },
                        onImportClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        viewModel = settingsViewModel,
                    )
                }
            }
        }

        when (val dialog = uiState.backupDialog) {
            is BackupDialogState.ExportPassword -> ExportPasswordDialog(
                onConfirm = { password ->
                    settingsViewModel.onExportPasswordFilled(password)
                    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    exportLauncher.launch("llyfr-$dateStr.mnbackup")
                },
                onDismiss = settingsViewModel::dismissBackupDialog,
            )
            is BackupDialogState.ImportPassword -> ImportPasswordDialog(
                onConfirm = settingsViewModel::onImportPasswordConfirmed,
                onDismiss = settingsViewModel::dismissBackupDialog,
            )
            is BackupDialogState.ImportConfirm -> ImportConfirmDialog(
                count = dialog.count,
                onConfirm = settingsViewModel::onImportConfirm,
                onDismiss = settingsViewModel::dismissBackupDialog,
            )
            BackupDialogState.None -> Unit
        }

        when (uiState.busyOp) {
            BusyOp.Export -> BusyDialog(stringResource(R.string.backup_export_busy))
            BusyOp.Import -> BusyDialog(stringResource(R.string.backup_import_busy))
            null -> Unit
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