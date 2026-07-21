package com.dphascow.app.navigation

import androidx.compose.runtime.mutableStateListOf

class MainNavigator(
    startRoute: AppRoute = AppRoute.Dashboard,
) {
    private val backStack = mutableStateListOf(startRoute)

    val currentRoute: AppRoute
        get() = backStack.last()

    val canGoBack: Boolean
        get() = backStack.size > 1

    fun open(route: AppRoute) {
        backStack += route
    }

    fun replace(route: AppRoute) {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
        backStack += route
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun reset(route: AppRoute = AppRoute.Dashboard) {
        backStack.clear()
        backStack += route
    }
}

