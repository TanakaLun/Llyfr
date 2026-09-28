package io.github.tanakalun.mynotes.ui.viewer

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
import io.github.tanakalun.mynotes.LocalNavigator
import io.github.tanakalun.mynotes.R
import io.github.tanakalun.mynotes.ui.util.BackNavigationIcon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ImageViewerPage(
    imagePath: String,
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "",
                color = Color.Black,
                titleColor = Color.White,
                navigationIcon = {
                    BackNavigationIcon(
                        onClick = { navigator.pop() },
                        tint = Color.White,
                    )
                },
            )
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
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
