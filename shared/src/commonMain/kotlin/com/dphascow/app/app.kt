// shared/src/commonMain/kotlin/com/dphascow/app/App.kt
package com.dphascow.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dphascow.app.auth.AppCoordinator
import com.dphascow.app.auth.AppDependencies
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.screens.AuthScreen
import com.dphascow.app.screens.BusinessCreationScreen
import com.dphascow.app.screens.BusinessSelectionScreen
import com.dphascow.app.screens.RegistrationScreen
import com.dphascow.app.screens.main.MainShell
import i18n.ProvideAppLocale
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.theme.AppTheme
import ui.theme.T
import settings.Prefs
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.common_loading

@Composable
fun App() {
    val prefs = remember { Prefs() }
    val coordinator = remember { AppCoordinator(prefs, AppDependencies.createAuthRepository(prefs)) }
    val businessWorkspaceRepository = remember { AppDependencies.createBusinessWorkspaceRepository(prefs) }
    val scope = rememberCoroutineScope()
    var lang by remember { mutableStateOf(prefs.lang ?: "ru") } // TODO get default lang
    var theme by remember { mutableStateOf(prefs.theme) } // TODO get default theme
    val state by coordinator.state.collectAsState()
    LaunchedEffect(lang) { prefs.lang = lang }
    LaunchedEffect(theme) { prefs.theme = theme }
    LaunchedEffect(Unit) { coordinator.bootstrap() }

    AppTheme(mode= theme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = T.c.background
        ) {
            ProvideAppLocale(lang) {
                when (val currentState = state) {
                    AppUiState.Loading -> LoadingScreen()
                    is AppUiState.Auth -> AuthScreen(
                        state = currentState,
                        onEmailChanged = coordinator::updateEmail,
                        onPasswordChanged = coordinator::updatePassword,
                        onLoginClick = {
                            scope.launch {
                                coordinator.login()
                            }
                        },
                        onRegistrationClick = coordinator::openRegistration,
                    )
                    is AppUiState.Registration -> RegistrationScreen(
                        state = currentState,
                        onNameChanged = coordinator::updateRegistrationName,
                        onEmailChanged = coordinator::updateRegistrationEmail,
                        onPasswordChanged = coordinator::updateRegistrationPassword,
                        onRegisterClick = {
                            scope.launch {
                                coordinator.register()
                            }
                        },
                        onBackClick = coordinator::cancelRegistration,
                    )
                    is AppUiState.BusinessSelection -> BusinessSelectionScreen(
                        state = currentState,
                        onRememberChoiceChanged = coordinator::updateRememberChoice,
                        onBusinessClick = { businessId ->
                            scope.launch {
                                coordinator.selectBusiness(businessId)
                            }
                        },
                        onCreateBusinessClick = coordinator::openCreateBusiness,
                        onLogoutClick = coordinator::logout
                    )
                    is AppUiState.BusinessCreation -> BusinessCreationScreen(
                        state = currentState,
                        onNameChanged = coordinator::updateCreateBusinessName,
                        onPhotoPicked = coordinator::updateCreateBusinessPhoto,
                        onCreateClick = {
                            scope.launch {
                                coordinator.createBusiness()
                            }
                        },
                        onCancelClick = coordinator::cancelCreateBusiness,
                    )
                    is AppUiState.Authorized -> MainShell(
                        state = currentState,
                        lang = lang,
                        businessWorkspaceRepository = businessWorkspaceRepository,
                        onChangeBusinessClick = coordinator::openBusinessSelection,
                        onLogoutClick = coordinator::logout
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(T.d.lg)
        ) {
            CircularProgressIndicator(color = T.c.primary)
            Text(
                text = stringResource(Res.string.common_loading),
                color = T.c.onBackground,
                style = T.t.t2Regular
            )
        }
    }
}


