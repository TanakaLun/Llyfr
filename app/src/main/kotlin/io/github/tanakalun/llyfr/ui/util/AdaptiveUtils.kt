package io.github.tanakalun.llyfr.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** 页面 Scaffold 的自适应内容 insets：宽屏排除 displayCutout.Start（对齐 example WideScreenContent）。 */
@Composable
fun adaptiveContentWindowInsets(isWideScreen: Boolean): WindowInsets =
    if (isWideScreen) {
        WindowInsets.systemBars.union(
            WindowInsets.displayCutout.exclude(
                WindowInsets.displayCutout.only(WindowInsetsSides.Start),
            ),
        )
    } else {
        WindowInsets.systemBars.union(WindowInsets.displayCutout)
    }

/** 宽屏（平板/折叠展开/大窗）：切换为 rail + 拆分布局。 */
@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        val heightDp = windowInfo.containerSize.height.toDp()
        val ratio = heightDp / widthDp
        widthDp >= 840.dp || (widthDp >= 600.dp && ratio < 1.2f)
    }
}

/** 足够宽时展开 rail（显示标签）。 */
@Composable
fun shouldExpandNavigationRail(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        val widthDp = windowInfo.containerSize.width.toDp()
        widthDp >= 1200.dp
    }
}