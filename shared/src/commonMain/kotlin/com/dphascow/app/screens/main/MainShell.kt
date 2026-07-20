package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.chat.ChatRepository
import com.dphascow.app.navigation.AppRoute
import com.dphascow.app.navigation.MainNavigator
import com.dphascow.app.profile.ProfileRepository
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import org.jetbrains.compose.resources.stringResource
import settings.ThemeMode
import ui.theme.T

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
    onLogoutClick: (allDevices: Boolean) -> Unit,
) {
    val navigator = remember { MainNavigator() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var workspace by remember(state.business.id) { mutableStateOf<BusinessWorkspace?>(null) }
    var currentUserId by remember(profileRepository) { mutableStateOf<String?>(null) }
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

    // The signed-in user's pk — used e.g. to offer "add myself as an employee".
    LaunchedEffect(profileRepository) {
        currentUserId = runCatching { profileRepository?.loadMe()?.id }.getOrNull()
    }

    val route = navigator.currentRoute
    // Content routes need the workspace; dashboard, account and chat screens don't.
    val needsWorkspace = route != AppRoute.Dashboard &&
        route != AppRoute.Account &&
        route != AppRoute.Settings &&
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = route == AppRoute.Dashboard || drawerState.isOpen,
        drawerContent = {
            AccountDrawer(
                onOpenAccount = {
                    scope.launch { drawerState.close() }
                    navigator.open(AppRoute.Account)
                },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    navigator.open(AppRoute.Settings)
                },
                onLogout = { allDevices ->
                    scope.launch { drawerState.close() }
                    onLogoutClick(allDevices)
                },
            )
        },
    ) {
    Scaffold(
        containerColor = T.c.background,
        bottomBar = {
            // Only the two root tabs carry the bar; pushed pages show a back arrow instead.
            if (route == AppRoute.Dashboard || route == AppRoute.Chats) {
                MainBottomBar(
                    current = route,
                    onSelect = { navigator.reset(it) },
                )
            }
        },
    ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        when (route) {
            AppRoute.Dashboard -> DashboardScreen(
                state = state,
                onOpenBusinessSettings = { navigator.open(AppRoute.BusinessSettings) },
                onOpenEmployees = { navigator.open(AppRoute.Employees) },
                onOpenGallery = { navigator.open(AppRoute.Gallery) },
                onOpenOrders = { navigator.open(AppRoute.Orders) },
                onOpenAnalytics = { navigator.open(AppRoute.Analytics) },
                onOpenReviews = { navigator.open(AppRoute.Reviews) },
                onOpenChat = { navigator.open(AppRoute.Chats) },
                onChangeBusinessClick = onChangeBusinessClick,
                onOpenMenu = { scope.launch { drawerState.open() } },
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
                businessId = state.business.id,
                lang = lang,
                onBack = { navigator.back() },
                onEditClick = { navigator.open(AppRoute.EmployeeEdit(route.employeeId)) },
                onAddServiceClick = { navigator.open(AppRoute.EmployeeServiceEdit(route.employeeId)) },
                onEditServiceClick = { navigator.open(AppRoute.EmployeeServiceEdit(route.employeeId, it)) },
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
                currentUserId = currentUserId,
                isCurrentUserEmployee = currentUserId != null &&
                    workspace?.employees?.any { it.userId == currentUserId } == true,
                onBack = { navigator.back() },
                onSaved = {
                    reload()
                    navigator.back()
                },
            )

            is AppRoute.EmployeeServiceEdit -> {
                val employee = workspace?.employees?.firstOrNull { it.id == route.employeeId }
                EmployeeServiceEditScreen(
                    repository = businessWorkspaceRepository,
                    businessId = state.business.id,
                    employeeId = route.employeeId,
                    service = route.serviceId?.let { id -> employee?.services?.firstOrNull { it.id == id } },
                    lang = lang,
                    onBack = { navigator.back() },
                    onSaved = {
                        reload()
                        navigator.back()
                    },
                )
            }

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
                onBack = { navigator.back() },
            )

            AppRoute.Settings -> SettingsScreen(
                lang = lang,
                theme = theme,
                onLangChange = onLangChange,
                onThemeChange = onThemeChange,
                onBack = { navigator.back() },
            )

            AppRoute.Chats -> ChatListScreen(
                repository = chatRepository,
                onOpenChat = { navigator.open(AppRoute.Conversation(it)) },
                onMenu = { scope.launch { drawerState.open() } },
            )

            is AppRoute.Conversation -> ConversationScreen(
                repository = chatRepository,
                chatId = route.chatId,
                onBack = { navigator.back() },
            )
        }
    }
    }
    }
}

/** Root-level tabs: the dashboard and chats. */
@Composable
private fun MainBottomBar(current: AppRoute, onSelect: (AppRoute) -> Unit) {
    NavigationBar(containerColor = T.c.surface) {
        NavigationBarItem(
            selected = current == AppRoute.Dashboard,
            onClick = { onSelect(AppRoute.Dashboard) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text(stringResource(Res.string.dashboard_title)) },
        )
        NavigationBarItem(
            selected = current == AppRoute.Chats,
            onClick = { onSelect(AppRoute.Chats) },
            icon = { Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null) },
            label = { Text(stringResource(Res.string.chat_title)) },
        )
    }
}


