package io.github.tanakalun.llyfr.ui.components.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.tanakalun.llyfr.core.PowerSaveModeTracker
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import io.github.tanakalun.llyfr.ui.isInDarkTheme
import io.github.tanakalun.llyfr.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextButtonColors
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassNavigationRail
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.rememberGlassNavigationRailState
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面级采样层：包裹整个 Scaffold（含正文与顶底栏），供 GlassDialog / GlassBottomSheet /
 * GlassDropdownPreference 等浮层采样。浮层自身渲染在 Scaffold 的 popup host（独立 window），
 * 因此不会发生自采样。
 */
val LocalPageBackdrop = compositionLocalOf<Backdrop?> { null }

/** 开屏语境下是否启用 glass 材质（省电 / 不支持运行时着色器时自动降级）。 */
@Composable
fun isGlassEffectActive(): Boolean {
    val powerSave by PowerSaveModeTracker.isPowerSaveMode.collectAsState()
    return SettingsStore.materialEffect == SettingsStore.EFFECT_GLASS &&
        !powerSave &&
        isRuntimeShaderSupported()
}

/** 开屏语境下是否启用 miuix blur（省电时自动降级）。 */
@Composable
fun isBlurEffectActive(): Boolean {
    val powerSave by PowerSaveModeTracker.isPowerSaveMode.collectAsState()
    return SettingsStore.materialEffect == SettingsStore.EFFECT_BLUR && !powerSave
}

/**
 * 记录当前内容的采样层；关闭质感或省电/不支持时返回 null（组件自动回退不透明底色）。
 * 录制前先铺一层不透明 surface，避免透明区域采样出黑底。
 */
@Composable
fun rememberSurfaceBackdrop(): LayerBackdrop? {
    val effect = SettingsStore.materialEffect
    val powerSave by PowerSaveModeTracker.isPowerSaveMode.collectAsState()
    if (effect == SettingsStore.EFFECT_NONE || powerSave || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

/**
 * 玻璃浮层内嵌 Card 的配色：对齐 Miuix example 的说明——
 * 用 `onBackground` 的低透明度填充，让底层玻璃材质透出来，而不是铺一块不透明 surface。
 * 非 glass 模式返回默认卡片配色。
 */
@Composable
fun glassCardColors(): CardColors {
    if (!isGlassEffectActive()) return CardDefaults.defaultColors()
    val alpha = if (isInDarkTheme()) 36f / 255f else 15f / 255f
    return CardDefaults.defaultColors(
        color = MiuixTheme.colorScheme.onBackground.copy(alpha = alpha),
    )
}

/**
 * 非折叠顶栏必须把 [ScrollBehavior] 的收缩上限钉到 0。
 * 否则 [MiuixScrollBehavior] 会把滚动手势当作“折叠顶栏”吞掉，列表完全滚不动。
 */
@Composable
fun PinScrollBehaviorToRest(scrollBehavior: ScrollBehavior?) {
    SideEffect {
        scrollBehavior?.state?.let { state ->
            if (state.heightOffsetLimit != 0f) state.heightOffsetLimit = 0f
            if (state.heightOffset != 0f) state.heightOffset = 0f
        }
    }
}

/** 顶栏：glass 模式用 GlassTopAppBar，miuix blur 沿用 BlurredBar，无质感时用纯色。 */
@Composable
fun AppTopAppBar(
    title: String,
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    large: Boolean = true,
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    if (!large) PinScrollBehaviorToRest(scrollBehavior)
    when {
        isGlassEffectActive() -> {
            if (large) {
                GlassTopAppBar(
                    title = title,
                    modifier = modifier,
                    expandable = true,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    progressiveBlur = true,
                    style = GlassDefaults.style(isInDarkTheme()),
                    defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    bottomContent = bottomContent,
                )
            } else if (scrollBehavior != null) {
                // 紧凑顶栏也要跟着滚动状态进模糊；写死 false 会让 band 永远不出现。
                GlassTopAppBar(
                    title = title,
                    modifier = modifier,
                    expandable = false,
                    progressiveBlur = true,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    style = GlassDefaults.style(isInDarkTheme()),
                    defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    bottomContent = bottomContent,
                )
            } else {
                GlassTopAppBar(
                    title = title,
                    isContentScrolled = false,
                    modifier = modifier,
                    expandable = false,
                    progressiveBlur = true,
                    backdrop = backdrop,
                    style = GlassDefaults.style(isInDarkTheme()),
                    defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    bottomContent = bottomContent,
                )
            }
        }

        else -> {
            val blurred = isBlurEffectActive() && backdrop != null
            val barColor = if (blurred) Color.Transparent else surfaceColor
            BlurredBar(backdrop = backdrop, blurActive = blurred, scrollBehavior = scrollBehavior) {
                if (large) {
                    TopAppBar(
                        title = title,
                        modifier = modifier,
                        color = barColor,
                        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                        scrollBehavior = scrollBehavior,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        bottomContent = bottomContent,
                    )
                } else {
                    SmallTopAppBar(
                        title = title,
                        modifier = modifier,
                        color = barColor,
                        scrollBehavior = scrollBehavior,
                        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                        navigationIcon = navigationIcon,
                        actions = actions,
                        bottomContent = bottomContent,
                    )
                }
            }
        }
    }
}

/** 图标按钮：glass 模式自动获得 28dp 玻璃按钮与浮动材质。 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    tint: Color = MiuixTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    if (isGlassEffectActive()) {
        GlassIconButton(
            onClick = onClick,
            modifier = modifier,
            backdrop = backdrop,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp),
                tint = tint,
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }
}

/** 文本动作按钮（顶栏里的“完成”等）。 */
@Composable
fun AppTextIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.primary,
) {
    if (isGlassEffectActive()) {
        GlassIconButton(onClick = onClick, modifier = modifier) {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body2,
                color = tint,
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Text(
                text = text,
                style = MiuixTheme.textStyles.body1,
                color = tint,
            )
        }
    }
}

/** 底部导航：glass 模式使用悬浮玻璃胶囊，其余模式保持原有 Miuix NavigationBar 外观。 */
@Composable
fun AppNavigationBar(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    defaultWindowInsetsPadding: Boolean = true,
    visible: Boolean = true,
) {
    if (isGlassEffectActive()) {
        GlassNavigationBar(
            items = items.map { GlassNavigationItem(it.icon, it.label) },
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            modifier = modifier,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
            visible = visible,
            style = GlassDefaults.style(isInDarkTheme()),
        )
    } else {
        NavigationBar(
            modifier = modifier,
            defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}

/** 宽屏侧边栏：glass 模式使用悬浮玻璃 rail，并复用同一导航回调。 */
@Composable
fun GlassNavigationRailHost(
    items: List<NavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop?,
    expandRail: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = rememberGlassNavigationRailState(initialExpanded = expandRail)
    LaunchedEffect(expandRail) { state.expanded = expandRail }
    GlassNavigationRail(
        items = items.map { GlassNavigationItem(it.icon, it.label) },
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        backdrop = backdrop,
        modifier = modifier,
        state = state,
        style = GlassDefaults.style(isInDarkTheme()),
    )
}

/** 分段 TabRow：glass 模式使用 example 中的第三款 GlassSegmentedTabRow。 */
@Composable
fun AppSegmentedTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = 1f,
) {
    if (isGlassEffectActive() && backdrop != null) {
        GlassSegmentedTabRow(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            backdrop = backdrop,
            modifier = modifier,
            style = GlassDefaults.style(isInDarkTheme()),
            surfaceAlpha = surfaceAlpha,
        )
    } else {
        TabRow(
            tabs = tabs,
            selectedTabIndex = selectedIndex,
            onTabSelected = onSelect,
            modifier = modifier,
        )
    }
}

/** 下拉偏好：glass 模式使用 GlassDropdownPreference（玻璃 popup），否则回退 Overlay 下拉。 */
@Composable
fun AppDropdownPreference(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val resolvedBackdrop = backdrop ?: LocalPageBackdrop.current
    if (isGlassEffectActive()) {
        GlassDropdownPreference(
            onSelectedIndexChange = onSelectedIndexChange,
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            backdrop = resolvedBackdrop,
            modifier = modifier,
            enabled = enabled,
            summary = summary,
        )
    } else {
        OverlayDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            modifier = modifier,
            summary = summary,
            enabled = enabled,
            onSelectedIndexChange = onSelectedIndexChange,
        )
    }
}

enum class AppButtonRole { Normal, Primary, Destructive }

/** 对话框：glass 模式采样页面层，按钮使用 GlassDialogButton。 */
@Composable
fun AppDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    summary: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (isGlassEffectActive()) {
        GlassDialog(
            show = show,
            backdrop = LocalPageBackdrop.current,
            onDismissRequest = onDismissRequest,
            title = title,
            summary = summary,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    } else {
        OverlayDialog(
            show = show,
            modifier = modifier,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

/** 对话框按钮：统一 glass / 普通样式与主次角色。 */
@Composable
fun AppDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: AppButtonRole = AppButtonRole.Normal,
) {
    if (isGlassEffectActive()) {
        GlassDialogButton(
            onClick = onClick,
            modifier = modifier,
            role = when (role) {
                AppButtonRole.Primary -> GlassDialogButtonRole.Primary
                AppButtonRole.Destructive -> GlassDialogButtonRole.Destructive
                AppButtonRole.Normal -> GlassDialogButtonRole.Normal
            },
        ) {
            Text(text = text, style = MiuixTheme.textStyles.button)
        }
    } else {
        val colors = when (role) {
            AppButtonRole.Primary -> TextButtonColors(
                color = MiuixTheme.colorScheme.primary,
                disabledColor = MiuixTheme.colorScheme.primaryContainer,
                textColor = MiuixTheme.colorScheme.onPrimary,
                disabledTextColor = MiuixTheme.colorScheme.onPrimaryContainer,
            )

            AppButtonRole.Destructive -> TextButtonColors(
                color = MiuixTheme.colorScheme.error,
                disabledColor = MiuixTheme.colorScheme.errorContainer,
                textColor = MiuixTheme.colorScheme.onError,
                disabledTextColor = MiuixTheme.colorScheme.onErrorContainer,
            )

            AppButtonRole.Normal -> null
        }
        if (colors == null) {
            TextButton(text = text, onClick = onClick, modifier = modifier)
        } else {
            TextButton(text = text, onClick = onClick, modifier = modifier, colors = colors)
        }
    }
}

/** 底部弹层：glass 模式采样页面层。 */
@Composable
fun AppBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String? = null,
    modifier: Modifier = Modifier,
    insideMargin: DpSize = BottomSheetDefaults.insideMargin,
    startAction: (@Composable () -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (isGlassEffectActive()) {
        GlassBottomSheet(
            show = show,
            backdrop = LocalPageBackdrop.current,
            onDismissRequest = onDismissRequest,
            title = title,
            modifier = modifier,
            insideMargin = insideMargin,
            startAction = startAction,
            endAction = endAction,
        ) {
            content()
        }
    } else {
        OverlayBottomSheet(
            show = show,
            modifier = modifier,
            title = title,
            insideMargin = insideMargin,
            startAction = startAction,
            endAction = endAction,
            onDismissRequest = onDismissRequest,
        ) {
            content()
        }
    }
}

/** 对话框内的提示文本排版辅助。 */
@Composable
fun AppDialogNotice(text: String, color: Color = MiuixTheme.colorScheme.error) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = color,
        textAlign = TextAlign.Start,
    )
}
