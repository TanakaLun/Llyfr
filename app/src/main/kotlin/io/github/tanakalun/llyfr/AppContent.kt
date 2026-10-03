package io.github.tanakalun.llyfr

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.tanakalun.llyfr.core.LocalOnBackSink
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.ui.about.AboutPage
import io.github.tanakalun.llyfr.ui.about.LicensePage
import io.github.tanakalun.llyfr.ui.editor.ChecklistPage
import io.github.tanakalun.llyfr.ui.editor.NoteEditorPage
import io.github.tanakalun.llyfr.ui.home.Home
import io.github.tanakalun.llyfr.ui.home.MainPagerState
import io.github.tanakalun.llyfr.ui.home.rememberMainPagerState
import io.github.tanakalun.llyfr.ui.navigation.Navigator
import io.github.tanakalun.llyfr.ui.navigation.Route
import io.github.tanakalun.llyfr.ui.util.shouldExpandNavigationRail
import io.github.tanakalun.llyfr.ui.util.shouldShowSplitPane
import io.github.tanakalun.llyfr.ui.viewer.ImageViewerPage
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.WindowNavigationEventScope

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator found") }
val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }
val LocalIsWideScreen = staticCompositionLocalOf { false }

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

    val isWideScreen = shouldShowSplitPane()
    val expandRail = shouldExpandNavigationRail()

    val navCornerRadius = if (isWideScreen) 0.dp else rememberNavSystemCornerRadius()
    val backdropColor = MiuixTheme.colorScheme.surface

    val effects = remember(navCornerRadius, backdropColor) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = navCornerRadius,
            cornerClipMode = NavCornerClipMode.Leading,
            dimAmount = 0.5f,
            backdropColor = backdropColor,
        )
    }

    val swipeBackDirection = when (LocalLayoutDirection.current) {
        LayoutDirection.Rtl -> NavSwipeDirection.RightToLeft
        else -> NavSwipeDirection.LeftToRight
    }
    // 自定义 in-app 边缘滑返默认关闭（对齐 example enableSwipeBack=false）。
    // 系统 predictive back 独立于它，始终可用。
    val swipeDismiss = if (SettingsStore.swipeBackEnabled) swipeBackDirection else NavSwipeDirection.None

    // Home 在非首个 tab 时，返回键/手势回到列表 tab（example 的 MainScreenBackHandler 模式）。
    MainScreenBackHandler(mainPagerState, navigator)

    var currentBack by remember { mutableStateOf<(() -> Unit)?>(null) }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalMainPagerState provides mainPagerState,
        LocalIsWideScreen provides isWideScreen,
    ) {
        CompositionLocalProvider(
            LocalOnBackSink provides { currentBack = it },
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isWideScreen) {
                    PersistentNavigationRail(
                        navigationItems = navigationItems,
                        mainPagerState = mainPagerState,
                        navigator = navigator,
                        expandRail = expandRail,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(if (isWideScreen) Modifier.clipToBounds() else Modifier),
                ) {
                    // 从平台 view tree 桥接 back dispatcher：缺失时 miuix-nav 的 PredictiveBackHandler 会
                    // 因 LocalNavigationEventDispatcherOwner 为 null 而 inert（预测性返回完全失效）。
                    WindowNavigationEventScope {
                        NavDisplay(
                            backStack = backStack,
                            onBack = {
                                val cb = currentBack
                                if (cb != null) cb() else navigator.pop()
                            },
                            transition = NavTransitions.MiuixDefault,
                            effects = effects,
                        ) {
                            entry<Route.NotesList>(swipeDismiss = swipeDismiss) {
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
                            entry<Route.NoteEditor>(swipeDismiss = swipeDismiss) { route ->
                                NoteEditorPage(noteId = route.noteId)
                            }
                            entry<Route.ChecklistEditor>(swipeDismiss = swipeDismiss) { route ->
                                ChecklistPage(noteId = route.noteId)
                            }
                            entry<Route.About>(swipeDismiss = swipeDismiss) {
                                AboutPage()
                            }
                            entry<Route.License>(swipeDismiss = swipeDismiss) {
                                LicensePage()
                            }
                            entry<Route.ImageViewer>(swipeDismiss = swipeDismiss) { route ->
                                ImageViewerPage(imagePath = route.imagePath)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 宽屏常驻左栏 rail（对齐 example 的 PersistentNavigationRail）：
 * 点 tab 先把 nav 栈弹回笔记列表根，再切 pager。
 */
@Composable
private fun PersistentNavigationRail(
    navigationItems: List<NavigationItem>,
    mainPagerState: MainPagerState,
    navigator: Navigator,
    expandRail: Boolean,
) {
    val railState = rememberNavigationRailState(
        initialValue = if (expandRail) NavigationRailValue.Expanded else NavigationRailValue.Collapsed,
    )
    LaunchedEffect(expandRail) {
        if (expandRail) railState.expand() else railState.collapse()
    }
    NavigationRail(
        state = railState,
    ) {
        navigationItems.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = mainPagerState.selectedPage == index,
                onClick = {
                    while (navigator.backStackSize() > 1) navigator.pop()
                    mainPagerState.animateToPage(index)
                },
                icon = item.icon,
                label = item.label,
            )
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navigator: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navigator.current() is Route.NotesList &&
                navigator.backStackSize() == 1 &&
                mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        },
    )
}