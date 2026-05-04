package com.dphascow.desktopapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dphascow.app.App   // твой @Composable из shared/commonMain

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "app") {
        App()
    }
}
