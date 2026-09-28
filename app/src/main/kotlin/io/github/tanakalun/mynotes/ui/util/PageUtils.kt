package io.github.tanakalun.mynotes.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.tanakalun.mynotes.core.PowerSaveModeTracker
import io.github.tanakalun.mynotes.data.settings.SettingsStore
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

object AppBlur {
    const val RADIUS = 18f
    const val SURFACE_ALPHA = 0.7f
    const val PROGRESSIVE_RADIUS = 10f
    const val PROGRESSIVE_SURFACE_ALPHA = 0.3f

    const val NAVBAR_RADIUS = 25f
    const val NAVBAR_SURFACE_ALPHA = 0.8f
    const val NAVBAR_FLOATING_SURFACE_ALPHA = 0.6f

    const val STYLE_GAUSSIAN = 0
    const val STYLE_PROGRESSIVE = 1

    @Composable
    fun barBlurActive(backdrop: LayerBackdrop?): Boolean =
        SettingsStore.enableBlur && backdrop != null && !isPowerSave()
}

@Composable
fun isPowerSave(): Boolean =
    PowerSaveModeTracker.isPowerSaveMode.collectAsState().value

@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    val powerSave by PowerSaveModeTracker.isPowerSaveMode.collectAsState()
    if (!SettingsStore.enableBlur || powerSave || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = backdrop != null,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val enabled = blurActive && backdrop != null && SettingsStore.enableBlur && !isPowerSave()
    val isProgressive = enabled && SettingsStore.blurStyle == AppBlur.STYLE_PROGRESSIVE
    val barBackgroundColor = MiuixTheme.colorScheme.surface

    Box(
        modifier = if (enabled && !isProgressive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = AppBlur.RADIUS,
                colors = BlurDefaults.blurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = barBackgroundColor.copy(AppBlur.SURFACE_ALPHA)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        if (isProgressive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = scrollBehavior?.state
                            ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                            ?: 1f
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = AppBlur.PROGRESSIVE_RADIUS,
                        colors = BlurDefaults.blurColors(
                            blendColors = listOf(
                                BlendColorEntry(color = barBackgroundColor.copy(AppBlur.PROGRESSIVE_SURFACE_ALPHA)),
                            ),
                        ),
                    ),
            )
        }
        content()
    }
}

@Composable
fun BackNavigationIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MiuixTheme.colorScheme.onBackground,
) {
    val layoutDirection = LocalLayoutDirection.current
    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.graphicsLayer {
                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            },
            imageVector = MiuixIcons.Back,
            contentDescription = null,
            tint = tint,
        )
    }
}
