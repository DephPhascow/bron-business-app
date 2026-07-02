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
import com.dphascow.app.navigation.AppRoute
import com.dphascow.app.navigation.MainNavigator

@Composable
fun MainShell(
    state: AppUiState.Authorized,
    lang: String,
    businessWorkspaceRepository: BusinessWorkspaceRepository?,
    onChangeBusinessClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val navigator = remember { MainNavigator() }
    var workspace by remember(state.business.id) { mutableStateOf<BusinessWorkspace?>(null) }

    LaunchedEffect(state.business.id, lang, businessWorkspaceRepository) {
        workspace = null
        workspace = businessWorkspaceRepository
            ?.let { repository ->
                runCatching {
                    repository.loadBusinessWorkspace(
                        businessId = state.business.id,
                        lang = lang,
                    )
                }.getOrNull()
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val route = navigator.currentRoute) {
            AppRoute.Dashboard -> DashboardScreen(
                state = state,
                onOpenBusinessSettings = { navigator.open(AppRoute.BusinessSettings) },
                onOpenEmployees = { navigator.open(AppRoute.Employees) },
                onOpenServices = { navigator.open(AppRoute.Services) },
                onOpenGallery = { navigator.open(AppRoute.Gallery) },
                onOpenOrders = { navigator.open(AppRoute.Orders) },
                onOpenAnalytics = { navigator.open(AppRoute.Analytics) },
                onChangeBusinessClick = onChangeBusinessClick,
                onLogoutClick = onLogoutClick,
            )

            AppRoute.BusinessSettings -> BusinessSettingsScreen(
                workspace = workspace,
                onBack = { navigator.back() },
            )

            AppRoute.Employees -> EmployeesScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onEmployeeClick = { navigator.open(AppRoute.EmployeeDetails(it)) },
                onAddEmployeeClick = { navigator.open(AppRoute.EmployeeEdit()) },
            )

            is AppRoute.EmployeeDetails -> EmployeeDetailsScreen(
                workspace = workspace,
                employeeId = route.employeeId,
                onBack = { navigator.back() },
                onEditClick = { navigator.open(AppRoute.EmployeeEdit(route.employeeId)) },
            )

            is AppRoute.EmployeeEdit -> EmployeeEditScreen(
                employeeId = route.employeeId,
                onBack = { navigator.back() },
                onSaveClick = { navigator.back() },
            )

            AppRoute.Services -> ServicesScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onServiceClick = { navigator.open(AppRoute.ServiceDetails(it)) },
                onAddServiceClick = { navigator.open(AppRoute.ServiceEdit()) },
            )

            is AppRoute.ServiceDetails -> ServiceDetailsScreen(
                workspace = workspace,
                serviceId = route.serviceId,
                onBack = { navigator.back() },
                onEditClick = { navigator.open(AppRoute.ServiceEdit(route.serviceId)) },
            )

            is AppRoute.ServiceEdit -> ServiceEditScreen(
                serviceId = route.serviceId,
                onBack = { navigator.back() },
                onSaveClick = { navigator.back() },
            )

            AppRoute.Gallery -> GalleryScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onUploadClick = { navigator.open(AppRoute.GalleryUpload) },
            )

            AppRoute.GalleryUpload -> GalleryUploadScreen(
                onBack = { navigator.back() },
                onSaveClick = { navigator.back() },
            )

            AppRoute.Orders -> OrdersScreen(
                workspace = workspace,
                onBack = { navigator.back() },
                onOrderClick = { navigator.open(AppRoute.OrderDetails(it)) },
            )

            is AppRoute.OrderDetails -> OrderDetailsScreen(
                workspace = workspace,
                orderId = route.orderId,
                onBack = { navigator.back() },
            )

            AppRoute.Analytics -> AnalyticsScreen(
                onBack = { navigator.back() },
            )
        }
    }
}


