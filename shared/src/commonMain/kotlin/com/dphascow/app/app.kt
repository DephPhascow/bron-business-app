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
    coil3.compose.setSingletonImageLoaderFactory { context ->
        coil3.ImageLoader.Builder(context)
            .components { add(coil3.network.ktor3.KtorNetworkFetcherFactory()) }
            .build()
    }
    val prefs = remember { Prefs() }
    val scope = rememberCoroutineScope()
    var lang by remember { mutableStateOf(prefs.lang ?: "ru") } // TODO get default lang
    var theme by remember { mutableStateOf(prefs.theme) } // TODO get default theme

    // Requester is created with a deferred auth-error callback so that
    // coordinator (created below) can be referenced from the lambda.
    var onAuthError: (() -> Unit)? by remember { mutableStateOf(null) }
    val requester = remember { AppDependencies.createRequester(prefs) { onAuthError?.invoke() } }
    val coordinator = remember {
        AppCoordinator(prefs, AppDependencies.createAuthRepository(requester))
    }
    val businessWorkspaceRepository = remember { AppDependencies.createBusinessWorkspaceRepository(requester) }
    val profileRepository = remember { AppDependencies.createProfileRepository(requester) }
    val chatRepository = remember { AppDependencies.createChatRepository(requester, prefs) }
    val pushRepository = remember { AppDependencies.createPushRepository(requester) }

    LaunchedEffect(Unit) {
        onAuthError = coordinator::clearSession
        coordinator.bootstrap()
        com.dphascow.app.push.PushTokenBridge.handler = { token ->
            scope.launch { runCatching { pushRepository.registerToken(token) } }
        }
    }
    LaunchedEffect(lang) { prefs.lang = lang }
    LaunchedEffect(theme) { prefs.theme = theme }

    val state by coordinator.state.collectAsState()

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
                        onPhoneChanged = coordinator::updatePhone,
                        onCodeChanged = coordinator::updateCode,
                        onGetCodeClick = {
                            scope.launch {
                                coordinator.requestCode()
                            }
                        },
                        onVerifyClick = {
                            scope.launch {
                                coordinator.verifyCode()
                            }
                        },
                        onResendClick = {
                            scope.launch {
                                coordinator.resendCode()
                            }
                        },
                        onBackClick = coordinator::backToPhone,
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
                        onLogoutClick = { scope.launch { coordinator.logout() } },
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
                        theme = theme,
                        businessWorkspaceRepository = businessWorkspaceRepository,
                        profileRepository = profileRepository,
                        chatRepository = chatRepository,
                        onLangChange = { lang = it },
                        onThemeChange = { theme = it },
                        onChangeBusinessClick = coordinator::openBusinessSelection,
                        onLogoutClick = { allDevices -> scope.launch { coordinator.logout(allDevices) } },
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


