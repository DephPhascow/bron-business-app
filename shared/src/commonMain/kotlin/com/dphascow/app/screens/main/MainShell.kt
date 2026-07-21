package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.chat.ChatRepository
import com.dphascow.app.expects.BackPressHandler
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
    autoEnterBusiness: Boolean,
    onAutoEnterBusinessChange: (Boolean) -> Unit,
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
    // Hiring goes by phone number, so "add myself" needs the signed-in user's phone.
    var currentUserPhone by remember(profileRepository) { mutableStateOf<String?>(null) }
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

    // The signed-in user — used e.g. to offer "add myself as an employee".
    LaunchedEffect(profileRepository) {
        val me = runCatching { profileRepository?.loadMe() }.getOrNull()
        currentUserId = me?.id
        currentUserPhone = me?.phone
    }

    val route = navigator.currentRoute

    // System back / back gesture: close the drawer first, otherwise pop the stack.
    // Declared outside the drawer so the drawer's own handler wins while it is open.
    BackPressHandler(enabled = drawerState.isOpen || navigator.canGoBack) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            navigator.back()
        }
    }

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
        // Edge-drag to open would swallow the system back gesture, so the drawer only
        // opens from the hamburger; the swipe stays available to close it.
        gesturesEnabled = drawerState.isOpen,
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
        // Same surface the pages draw on, so no seam shows around the bottom bar.
        containerColor = T.c.dark1,
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
                onOpenSchedule = { navigator.open(AppRoute.MySchedule) },
                onOpenAnalytics = { navigator.open(AppRoute.Analytics) },
                onOpenReviews = { navigator.open(AppRoute.Reviews) },
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
                currentUserId = currentUserId,
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
                currentUserPhone = currentUserPhone,
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
                repository = businessWorkspaceRepository,
                onBack = { navigator.back() },
                onUploadClick = { navigator.open(AppRoute.GalleryUpload) },
                onChanged = { reload() },
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
                onBookClientClick = { navigator.open(AppRoute.BookClient) },
            )

            is AppRoute.OrderDetails -> OrderDetailsScreen(
                workspace = workspace,
                orderId = route.orderId,
                businessId = state.business.id,
                repository = businessWorkspaceRepository,
                chatRepository = chatRepository,
                onBack = { navigator.back() },
                onChanged = {
                    reload()
                    navigator.back()
                },
                onOpenConversation = { navigator.open(AppRoute.Conversation(it)) },
            )

            AppRoute.BookClient -> BookClientScreen(
                workspace = workspace,
                repository = businessWorkspaceRepository,
                businessId = state.business.id,
                onBack = { navigator.back() },
                onSaved = {
                    reload()
                    navigator.back()
                },
            )

            AppRoute.MySchedule -> MyScheduleScreen(
                repository = businessWorkspaceRepository,
                businessId = state.business.id,
                lang = lang,
                onBack = { navigator.back() },
            )

            AppRoute.Analytics -> AnalyticsScreen(
                repository = businessWorkspaceRepository,
                businessId = state.business.id,
                lang = lang,
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
                autoEnterBusiness = autoEnterBusiness,
                onAutoEnterBusinessChange = onAutoEnterBusinessChange,
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
    // The client app's bar is a filled `graniteGreen7` strip with `dark1` icons and
    // no selection pill.
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = T.c.dark1,
        unselectedIconColor = T.c.dark1,
        selectedTextColor = T.c.dark1,
        unselectedTextColor = T.c.dark1,
        indicatorColor = Color.Transparent,
    )
    NavigationBar(containerColor = T.c.graniteGreen7) {
        NavigationBarItem(
            selected = current == AppRoute.Dashboard,
            onClick = { onSelect(AppRoute.Dashboard) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(26.dp)) },
            label = { Text(stringResource(Res.string.dashboard_title), style = T.t.t5) },
            colors = itemColors,
        )
        NavigationBarItem(
            selected = current == AppRoute.Chats,
            onClick = { onSelect(AppRoute.Chats) },
            icon = { Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(26.dp)) },
            label = { Text(stringResource(Res.string.chat_title), style = T.t.t5) },
            colors = itemColors,
        )
    }
}


