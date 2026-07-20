package com.dphascow.desktopapp

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.auth.BusinessOption
import com.dphascow.app.screens.main.MainShell
import i18n.ProvideAppLocale
import settings.ThemeMode
import ui.theme.AppTheme

// TEMPORARY: renders the dashboard without going through SMS auth, so the shell
// layout can be eyeballed. Delete after verifying.
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "preview") {
        ProvideAppLocale("ru") {
            AppTheme(ThemeMode.LIGHT) {
                    MainShell(
                        state = AppUiState.Authorized(
                            phone = "998901112233",
                            business = BusinessOption(id = "1", name = "Bron Center", role = "OWNER"),
                            canSwitchBusiness = true,
                        ),
                        lang = "ru",
                        theme = ThemeMode.LIGHT,
                        businessWorkspaceRepository = null,
                        profileRepository = null,
                        chatRepository = null,
                        onLangChange = {},
                        onThemeChange = {},
                        onChangeBusinessClick = {},
                        onLogoutClick = {},
                    )
            }
        }
    }
}
