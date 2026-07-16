package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.chat.ChatRepository
import com.dphascow.app.navigation.AppRoute
import com.dphascow.app.navigation.MainNavigator
import com.dphascow.app.profile.ProfileRepository
import settings.ThemeMode

@Composable
fun MainShell(
    state: AppUiState.Authorized,
    lang: String,
    theme: ThemeMode,
    businessWorkspaceRepository: BusinessWorkspaceRepository?,
    profileRepository: ProfileRepository?,
    chatRepository: ChatRepository?,
    onLangChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onChangeBusinessClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val navigator = remember { MainNavigator() }
    var workspace by remember(state.business.id) { mutableStateOf<BusinessWorkspace?>(null) }
    var loading by remember(state.business.id) { mutableStateOf(true) }
    var loadError by remember(state.business.id) { mutableStateOf<String?>(null) }
    var reloadKey by remember(state.business.id) { mutableStateOf(0) }

    LaunchedEffect(state.business.id, lang, businessWorkspaceRepository, reloadKey) {
        loading = true
        loadError = null
        val repository = businessWorkspaceRepository
        if (repository == null) {
            loading = false
            loadError = "No repository"
            return@LaunchedEffect
        }
        runCatching {
            repository.loadBusinessWorkspace(businessId = state.business.id, lang = lang)
        }.onSuccess {
            workspace = it
            loading = false
        }.onFailure {
            loadError = it.message
            loading = false
        }
    }
    val reload = { reloadKey++ }

    val route = navigator.currentRoute
    // Content routes need the workspace; dashboard, account and chat screens don't.
    val needsWorkspace = route != AppRoute.Dashboard &&
        route != AppRoute.Account &&
        route != AppRoute.Chats &&
        route !is AppRoute.Conversation
    if (needsWorkspace && (loading || loadError != null)) {
        Column(modifier = Modifier.fillMaxSize()) {
            WorkspaceStatusScreen(
                loading = loading,
                error = loadError,
                onRetry = { reload() },
                onBack = { navigator.back() },
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (route) {
            AppRoute.Dashboard -> DashboardScreen(
                state = state,
                onOpenBusinessSettings = { navigator.open(AppRoute.BusinessSettings) },
                onOpenEmployees = { navigator.open(AppRoute.Employees) },
                onOpenServices = { navigator.open(AppRoute.Services) },
                onOpenGallery = { navigator.open(AppRoute.Gallery) },
                onOpenOrders = { navigator.open(AppRoute.Orders) },
                onOpenAnalytics = { navigator.open(AppRoute.Analytics) },
                onOpenReviews = { navigator.open(AppRoute.Reviews) },
                onOpenChat = { navigator.open(AppRoute.Chats) },
                onChangeBusinessClick = onChangeBusinessClick,
                onOpenAccount = { navigator.open(AppRoute.Account) },
            )

            AppRoute.BusinessSettings -> BusinessSettingsScreen(
                workspace = workspace,
                repository = businessWorkspaceRepository,
                onBack = { navigator.back() },
                onSaved = {
                    reload()
                    navigator.back()
                },
            )

            AppRoute.Employees -> EmployeesScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onEmployeeClick = { navigator.open(AppRoute.EmployeeDetails(it)) },
                onAddEmployeeClick = { navigator.open(AppRoute.EmployeeEdit()) },
            )

            is AppRoute.EmployeeDetails -> EmployeeDetailsScreen(
                employee = workspace?.employees?.firstOrNull { it.id == route.employeeId },
                repository = businessWorkspaceRepository,
                chatRepository = chatRepository,
                lang = lang,
                onBack = { navigator.back() },
                onEditClick = { navigator.open(AppRoute.EmployeeEdit(route.employeeId)) },
                onOpenConversation = { navigator.open(AppRoute.Conversation(it)) },
                onDeleted = {
                    reload()
                    navigator.back()
                },
            )

            is AppRoute.EmployeeEdit -> EmployeeEditScreen(
                repository = businessWorkspaceRepository,
                businessId = state.business.id,
                lang = lang,
                employee = route.employeeId?.let { id -> workspace?.employees?.firstOrNull { it.id == id } },
                onBack = { navigator.back() },
                onSaved = {
                    reload()
                    navigator.back()
                },
            )

            AppRoute.Services -> ServicesScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onServiceClick = { navigator.open(AppRoute.ServiceDetails(it)) },
            )

            is AppRoute.ServiceDetails -> ServiceDetailsScreen(
                workspace = workspace,
                serviceId = route.serviceId,
                onBack = { navigator.back() },
            )

            AppRoute.Gallery -> GalleryScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onUploadClick = { navigator.open(AppRoute.GalleryUpload) },
            )

            AppRoute.GalleryUpload -> GalleryUploadScreen(
                workspace = workspace,
                repository = businessWorkspaceRepository,
                onBack = { navigator.back() },
                onSaved = {
                    reload()
                    navigator.back()
                },
            )

            AppRoute.Orders -> OrdersScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onOrderClick = { navigator.open(AppRoute.OrderDetails(it)) },
            )

            is AppRoute.OrderDetails -> OrderDetailsScreen(
                workspace = workspace,
                orderId = route.orderId,
                repository = businessWorkspaceRepository,
                chatRepository = chatRepository,
                onBack = { navigator.back() },
                onChanged = {
                    reload()
                    navigator.back()
                },
                onOpenConversation = { navigator.open(AppRoute.Conversation(it)) },
            )

            AppRoute.Analytics -> AnalyticsScreen(
                onBack = { navigator.back() },
            )

            AppRoute.Reviews -> ReviewsScreen(
                workspace = workspace,
                onBack = { navigator.back() },
            )

            AppRoute.Account -> AccountScreen(
                profileRepository = profileRepository,
                lang = lang,
                theme = theme,
                onLangChange = onLangChange,
                onThemeChange = onThemeChange,
                onBack = { navigator.back() },
                onLogout = onLogoutClick,
            )

            AppRoute.Chats -> ChatListScreen(
                repository = chatRepository,
                onOpenChat = { navigator.open(AppRoute.Conversation(it)) },
                onBack = { navigator.back() },
            )

            is AppRoute.Conversation -> ConversationScreen(
                repository = chatRepository,
                chatId = route.chatId,
                onBack = { navigator.back() },
            )
        }
    }
}


