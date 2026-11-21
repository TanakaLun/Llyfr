package io.github.tanakalun.llyfr.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.tanakalun.llyfr.LocalNavigator
import io.github.tanakalun.llyfr.ui.util.adaptiveContentWindowInsets
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.LocalIsWideScreen
import io.github.tanakalun.llyfr.ui.components.glass.AppTopAppBar
import io.github.tanakalun.llyfr.ui.components.glass.isGlassEffectActive
import io.github.tanakalun.llyfr.ui.util.BackNavigationIcon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ImageViewerPage(
    imagePath: String,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isWideScreen = LocalIsWideScreen.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val glass = isGlassEffectActive()
    // 全屏图片页以纯黑作为采样底，避免玻璃顶栏取到主题 surface 色。
    val backdrop = if (glass) {
        rememberLayerBackdrop {
            drawRect(Color.Black)
            drawContent()
        }
    } else {
        null
    }

    Scaffold(
        contentWindowInsets = adaptiveContentWindowInsets(isWideScreen),
        topBar = {
            if (glass) {
                AppTopAppBar(
                    title = "",
                    backdrop = backdrop,
                    large = false,
                    defaultWindowInsetsPadding = !isWideScreen,
                    navigationIcon = {
                        BackNavigationIcon(
                            onClick = { navigator.pop() },
                            tint = Color.White,
                        )
                    },
                )
            } else {
                SmallTopAppBar(
                    title = "",
                    color = Color.Black,
                    titleColor = Color.White,
                    defaultWindowInsetsPadding = !isWideScreen,
                    navigationIcon = {
                        BackNavigationIcon(
                            onClick = { navigator.pop() },
                            tint = Color.White,
                        )
                    },
                )
            }
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .padding(
                    top = scaffoldPadding.calculateTopPadding(),
                    bottom = scaffoldPadding.calculateBottomPadding(),
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imagePath)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.full_size_image),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY,
                    ),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
