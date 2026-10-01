package io.github.tanakalun.mynotes.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tanakalun.mynotes.LocalNavigator
import io.github.tanakalun.mynotes.ui.util.adaptiveContentWindowInsets
import io.github.tanakalun.mynotes.R
import io.github.tanakalun.mynotes.LocalIsWideScreen
import io.github.tanakalun.mynotes.core.AppViewModelFactory
import io.github.tanakalun.mynotes.ui.util.BackNavigationIcon
import io.github.tanakalun.mynotes.ui.util.BlurredBar
import io.github.tanakalun.mynotes.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun LicensePage(
    viewModel: LicenseViewModel = viewModel(factory = AppViewModelFactory),
) {
    val navigator = LocalNavigator.current
    val isWideScreen = LocalIsWideScreen.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backdrop = rememberBlurBackdrop()
    val barColor = if (backdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    Scaffold(
        contentWindowInsets = adaptiveContentWindowInsets(isWideScreen),
        topBar = {
            BlurredBar(
                backdrop = backdrop,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                SmallTopAppBar(
                    title = stringResource(R.string.third_party_licenses),
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    defaultWindowInsetsPadding = !isWideScreen,
                    navigationIcon = {
                        BackNavigationIcon(onClick = { navigator.pop() })
                    },
                )
            }
        },
    ) { scaffoldPadding ->
        val uriHandler = LocalUriHandler.current
        val lazyListState = rememberLazyListState()
        val contentPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp,
            start = if (isWideScreen) 0.dp else WindowInsets.displayCutout.asPaddingValues().calculateLeftPadding(LayoutDirection.Ltr),
            end = WindowInsets.displayCutout.asPaddingValues().calculateRightPadding(LayoutDirection.Ltr),
        )
        Box(
            modifier = Modifier.fillMaxSize().then(
                if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier
            ),
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .scrollEndHaptic()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                contentPadding = contentPadding,
            ) {
                uiState.libraries?.let { libs ->
                    items(libs, key = { it.uniqueId }) { library ->
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 12.dp),
                        ) {
                            ArrowPreference(
                                title = library.name,
                                summary = "${library.artifactVersion}, ${library.licenses.firstOrNull()}",
                                onClick = {
                                    library.website?.let {
                                        uriHandler.openUri(library.website)
                                    }
                                },
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}