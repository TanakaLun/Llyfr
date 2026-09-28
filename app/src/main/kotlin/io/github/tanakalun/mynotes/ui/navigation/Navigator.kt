package io.github.tanakalun.mynotes.ui.navigation

import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

class Navigator(val backStack: NavBackStack) {

    fun push(key: NavKey) {
        if (key !in backStack) {
            backStack.add(key)
        }
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun current() = backStack.lastOrNull()

    fun backStackSize() = backStack.size
}