package io.github.tanakalun.llyfr.core

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 当前页面注册“返回前保存”回调的入口。
 * 编辑器页通过它在 AppContent 的 NavDisplay.onBack 中注入 save-then-pop，
 * 避免系统返回/侧滑返回丢弃草稿。
 *
 * 注意：清除时用 `onBackSink(null)`，不要传空 lambda —— 空 lambda 的
 * `invoke()` 返回非 null 的 Unit，会让调用方的 `?: navigator.pop()` 永不走
 * 弹栈分支，导致手势返回被静默吞掉。
 */
val LocalOnBackSink = staticCompositionLocalOf<((() -> Unit)?) -> Unit> { { } }