// shared/src/commonMain/kotlin/com/dphascow/app/App.kt
package com.dphascow.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dphascow.app.screens.HomeScreen
import i18n.ProvideAppLocale
import ui.theme.AppTheme
import ui.theme.T

private sealed interface Screens {
    data object Main : Screens
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf<Screens>(Screens.Main) }
    val prefs = remember { settings.Prefs() }
    var lang by remember { mutableStateOf(prefs.lang ?: "ru") } // TODO get default lang
    var theme by remember { mutableStateOf(prefs.theme) } // TODO get default theme
    val accessToken by prefs.accessTokenFlow.collectAsState()
    val refreshToken by prefs.refreshTokenFlow.collectAsState()
    LaunchedEffect(lang) { prefs.lang = lang }
    LaunchedEffect(theme) { prefs.theme = theme }
    LaunchedEffect(Unit) {
        prefs.accessToken = "accessToken"
        prefs.refreshToken = "refreshToken"
    }
    LaunchedEffect(accessToken, refreshToken) {
        if (prefs.accessToken == null || prefs.refreshToken == null) {
            currentScreen = Screens.Main
        }
    }
//    LaunchedEffect(lang) { prefs.lang = lang }
//    LaunchedEffect(theme) { prefs.theme = theme }


    AppTheme(mode= theme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = T.c.background
        ) {
            ProvideAppLocale(lang) {
                when (currentScreen) {
                    Screens.Main -> {
                        HomeScreen()
                    }
                }
            }
        }
    }
}


