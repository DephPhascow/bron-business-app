package com.dphascow.app.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.mock.MockBusinessData
import com.dphascow.app.mock.MockEmployee
import com.dphascow.app.mock.MockGalleryPhoto
import com.dphascow.app.mock.MockOrder
import com.dphascow.app.mock.MockService
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

@Composable
internal fun PageLayout(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        if (onBack != null) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(Res.string.common_back))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(T.d.xs)) {
            Text(
                text = title,
                color = T.c.onBackground,
                style = T.t.headingH3,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = T.c.dark7,
                    style = T.t.t2Regular,
                )
            }
        }

        content()
    }
}

@Composable
internal fun InfoCard(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T.c.surface),
        shape = RoundedCornerShape(T.d.lg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(T.d.lg),
            verticalArrangement = Arrangement.spacedBy(T.d.sm),
        ) {
            Text(
                text = title,
                color = T.c.onSurface,
                style = T.t.t2Bold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = T.c.dark7,
                    style = T.t.t2Regular,
                )
            }
            if (actionText != null && onClick != null) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
internal fun ActionRow(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(T.d.md),
    ) {
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(primaryText)
        }
        if (secondaryText != null && onSecondaryClick != null) {
            OutlinedButton(
                onClick = onSecondaryClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(secondaryText)
            }
        }
    }
}

@Composable
fun DashboardScreen(
    state: AppUiState.Authorized,
    onOpenBusinessSettings: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onChangeBusinessClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val open = stringResource(Res.string.common_open)
    PageLayout(
        title = stringResource(Res.string.dashboard_title),
        subtitle = state.business.name,
    ) {
        InfoCard(stringResource(Res.string.dashboard_today_title), stringResource(Res.string.dashboard_today_subtitle))
        InfoCard(stringResource(Res.string.analytics_orders_title), stringResource(Res.string.dashboard_orders_subtitle), open, onOpenOrders)
        InfoCard(stringResource(Res.string.employees_title), stringResource(Res.string.dashboard_employees_subtitle), open, onOpenEmployees)
        InfoCard(stringResource(Res.string.services_title), stringResource(Res.string.dashboard_services_subtitle), open, onOpenServices)
        InfoCard(stringResource(Res.string.gallery_title), stringResource(Res.string.dashboard_gallery_subtitle), open, onOpenGallery)
        InfoCard(stringResource(Res.string.analytics_title), stringResource(Res.string.dashboard_analytics_subtitle), open, onOpenAnalytics)
        InfoCard(stringResource(Res.string.business_settings_title), stringResource(Res.string.dashboard_business_settings_subtitle), open, onOpenBusinessSettings)
        if (state.canSwitchBusiness) {
            InfoCard(stringResource(Res.string.home_change_business), stringResource(Res.string.dashboard_change_business_subtitle), stringResource(Res.string.common_change), onChangeBusinessClick)
        }
        InfoCard(stringResource(Res.string.dashboard_account_title), state.email, stringResource(Res.string.home_logout), onLogoutClick)
    }
}

@Composable
fun BusinessSettingsScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
) {
    val defaultAddress = stringResource(Res.string.business_settings_default_address)
    var name by remember(workspace?.name) { mutableStateOf(workspace?.name ?: "Bron Beauty") }
    var phone by remember(workspace?.phone) { mutableStateOf(workspace?.phone ?: "+998 90 000 00 00") }
    var address by remember(workspace?.address, defaultAddress) { mutableStateOf(workspace?.address ?: defaultAddress) }

    PageLayout(
        title = stringResource(Res.string.business_settings_title),
        subtitle = stringResource(Res.string.business_settings_subtitle),
        onBack = onBack,
    ) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.business_create_name_label)) }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.business_settings_phone_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.business_settings_address_label)) })
        ActionRow(stringResource(Res.string.common_save), onBack)
    }
}

@Composable
fun EmployeesScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onEmployeeClick: (String) -> Unit,
    onAddEmployeeClick: () -> Unit,
) {
    PageLayout(stringResource(Res.string.employees_title), stringResource(Res.string.employees_subtitle), onBack) {
        InfoCard(stringResource(Res.string.employees_add_title), stringResource(Res.string.employees_add_subtitle), stringResource(Res.string.common_add), onAddEmployeeClick)
        val apiEmployees = workspace?.employees
        if (apiEmployees != null) {
            apiEmployees.forEach { employee ->
                InfoCard(employee.name, listOfNotNull(employee.role, employee.phone).joinToString(" · "), stringResource(Res.string.common_open)) { onEmployeeClick(employee.id) }
            }
        } else {
            MockBusinessData.employees.forEach { employee ->
                InfoCard(employeeName(employee), "${employeeRole(employee)} · ${employee.phone}", stringResource(Res.string.common_open)) { onEmployeeClick(employee.id) }
            }
        }
    }
}

@Composable
fun EmployeeDetailsScreen(
    workspace: BusinessWorkspace?,
    employeeId: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
) {
    val apiEmployee = workspace?.employees?.firstOrNull { it.id == employeeId }
    val employee = MockBusinessData.employees.firstOrNull { it.id == employeeId }
    val role = apiEmployee?.role ?: employee?.let { employeeRole(it) }
    val name = apiEmployee?.name ?: employee?.let { employeeName(it) }
    val contacts = listOfNotNull(apiEmployee?.phone ?: employee?.phone, apiEmployee?.email ?: employee?.email).joinToString("\n")

    PageLayout(name ?: stringResource(Res.string.employee_title_fallback), role ?: stringResource(Res.string.employee_profile_subtitle), onBack) {
        InfoCard(stringResource(Res.string.employee_contacts_title), contacts)
        InfoCard(stringResource(Res.string.employee_schedule_title), stringResource(Res.string.employee_schedule_value))
        InfoCard(stringResource(Res.string.employee_access_title), stringResource(Res.string.employee_role_value, role ?: "—"), stringResource(Res.string.common_edit), onEditClick)
    }
}

@Composable
fun EmployeeEditScreen(
    employeeId: String?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val employee = MockBusinessData.employees.firstOrNull { it.id == employeeId }
    val defaultName = employee?.let { employeeName(it) }.orEmpty()
    val defaultRole = employee?.let { employeeRole(it) }.orEmpty()
    var name by remember(employeeId, defaultName) { mutableStateOf(defaultName) }
    var role by remember(employeeId, defaultRole) { mutableStateOf(defaultRole) }
    var phone by remember(employeeId) { mutableStateOf(employee?.phone.orEmpty()) }
    var email by remember(employeeId) { mutableStateOf(employee?.email.orEmpty()) }

    PageLayout(
        if (employeeId == null) stringResource(Res.string.employee_add_title) else stringResource(Res.string.employee_edit_title),
        stringResource(Res.string.employee_edit_subtitle),
        onBack,
    ) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.employee_name_label)) }, singleLine = true)
        OutlinedTextField(role, { role = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.employee_position_label)) }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.business_settings_phone_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.auth_email_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        ActionRow(stringResource(Res.string.common_save), onSaveClick, stringResource(Res.string.common_cancel), onBack)
    }
}

@Composable
fun ServicesScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onServiceClick: (String) -> Unit,
    onAddServiceClick: () -> Unit,
) {
    PageLayout(stringResource(Res.string.services_title), stringResource(Res.string.services_subtitle), onBack) {
        InfoCard(stringResource(Res.string.services_create_title), stringResource(Res.string.services_create_subtitle), stringResource(Res.string.common_create), onAddServiceClick)
        val apiServices = workspace?.services
        if (apiServices != null) {
            apiServices.forEach { service ->
                InfoCard(service.name, "${service.duration} · ${service.price}", stringResource(Res.string.common_open)) { onServiceClick(service.id) }
            }
        } else {
            MockBusinessData.services.forEach { service ->
                InfoCard(serviceName(service), "${stringResource(Res.string.service_duration_value, service.durationMinutes)} · ${service.price}", stringResource(Res.string.common_open)) { onServiceClick(service.id) }
            }
        }
    }
}

@Composable
fun ServiceDetailsScreen(
    workspace: BusinessWorkspace?,
    serviceId: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
) {
    val apiService = workspace?.services?.firstOrNull { it.id == serviceId }
    val service = MockBusinessData.services.firstOrNull { it.id == serviceId }

    PageLayout(apiService?.name ?: service?.let { serviceName(it) } ?: stringResource(Res.string.service_title_fallback), stringResource(Res.string.service_details_subtitle), onBack) {
        InfoCard(stringResource(Res.string.service_price_title), apiService?.price ?: service?.price ?: "—")
        InfoCard(stringResource(Res.string.service_duration_title), apiService?.duration ?: stringResource(Res.string.service_duration_value, service?.durationMinutes ?: 0))
        InfoCard(stringResource(Res.string.service_description_title), stringResource(Res.string.service_description_placeholder), stringResource(Res.string.common_edit), onEditClick)
    }
}

@Composable
fun ServiceEditScreen(
    serviceId: String?,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    val service = MockBusinessData.services.firstOrNull { it.id == serviceId }
    val defaultName = service?.let { serviceName(it) }.orEmpty()
    var name by remember(serviceId, defaultName) { mutableStateOf(defaultName) }
    var duration by remember(serviceId) { mutableStateOf(service?.durationMinutes?.toString().orEmpty()) }
    var price by remember(serviceId) { mutableStateOf(service?.price.orEmpty()) }
    var description by remember(serviceId) { mutableStateOf("") }

    PageLayout(
        if (serviceId == null) stringResource(Res.string.service_create_title) else stringResource(Res.string.service_edit_title),
        stringResource(Res.string.service_edit_subtitle),
        onBack,
    ) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.service_name_label)) }, singleLine = true)
        OutlinedTextField(duration, { duration = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.service_duration_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.service_price_label)) }, singleLine = true)
        OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.service_description_label)) })
        ActionRow(stringResource(Res.string.common_save), onSaveClick, stringResource(Res.string.common_cancel), onBack)
    }
}

@Composable
fun GalleryScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
) {
    PageLayout(stringResource(Res.string.gallery_title), stringResource(Res.string.gallery_subtitle), onBack) {
        InfoCard(stringResource(Res.string.gallery_upload_title_card), stringResource(Res.string.gallery_upload_subtitle_card), stringResource(Res.string.common_upload), onUploadClick)
        val apiGallery = workspace?.gallery
        if (apiGallery != null) {
            apiGallery.forEach { photo ->
                InfoCard(photo.imageUrl, stringResource(Res.string.gallery_mock_photo_subtitle))
            }
        } else {
            MockBusinessData.gallery.forEach { photo ->
                InfoCard(galleryPhotoTitle(photo), stringResource(Res.string.gallery_mock_photo_subtitle))
            }
        }
    }
}

@Composable
fun GalleryUploadScreen(
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<PickedPhoto?>(null) }
    val photoPicker = rememberPhotoPickerLauncher(onPhotoPicked = { photo = it })

    PageLayout(stringResource(Res.string.gallery_upload_title), stringResource(Res.string.gallery_upload_subtitle), onBack) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.3f)
                .clip(RoundedCornerShape(T.d.lg))
                .background(if (photo == null) T.c.dark3 else T.c.graniteGreen7),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (photo == null) Icons.Outlined.AddAPhoto else Icons.Outlined.Image,
                contentDescription = null,
                tint = T.c.dark1,
            )
        }
        OutlinedButton(onClick = photoPicker::launch, enabled = photoPicker.isAvailable, modifier = Modifier.fillMaxWidth()) {
            Text(photo?.fileName ?: stringResource(Res.string.gallery_choose_photo_action))
        }
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.gallery_description_label)) })
        ActionRow(stringResource(Res.string.common_save), onSaveClick, stringResource(Res.string.common_cancel), onBack)
    }
}

@Composable
fun OrdersScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    PageLayout(stringResource(Res.string.orders_title), stringResource(Res.string.orders_subtitle), onBack) {
        val apiOrders = workspace?.orders
        if (apiOrders != null) {
            apiOrders.forEach { order ->
                val employeeName = workspace.employees.firstOrNull { it.id == order.employeeId }?.name.orEmpty()
                InfoCard(
                    title = "${order.clientName} · ${order.dateTime}",
                    subtitle = listOf(order.serviceName, employeeName, order.status).filter { it.isNotBlank() }.joinToString(" · "),
                    actionText = stringResource(Res.string.common_open),
                    onClick = { onOrderClick(order.id) },
                )
            }
        } else {
            MockBusinessData.orders.forEach { order ->
                InfoCard(
                    title = "${orderClientName(order)} · ${orderDateTime(order)}",
                    subtitle = "${orderServiceName(order)} · ${orderEmployeeName(order)} · ${orderStatus(order)}",
                    actionText = stringResource(Res.string.common_open),
                    onClick = { onOrderClick(order.id) },
                )
            }
        }
    }
}

@Composable
fun OrderDetailsScreen(
    workspace: BusinessWorkspace?,
    orderId: String,
    onBack: () -> Unit,
) {
    val apiOrder = workspace?.orders?.firstOrNull { it.id == orderId }
    val order = MockBusinessData.orders.firstOrNull { it.id == orderId }
    val apiEmployeeName = apiOrder?.let { api -> workspace?.employees?.firstOrNull { it.id == api.employeeId }?.name }

    PageLayout(stringResource(Res.string.order_title), apiOrder?.clientName ?: order?.let { orderClientName(it) } ?: stringResource(Res.string.order_details_subtitle), onBack) {
        InfoCard(stringResource(Res.string.order_client_title), apiOrder?.clientName ?: order?.let { orderClientName(it) } ?: "—")
        InfoCard(stringResource(Res.string.order_service_title), apiOrder?.serviceName ?: order?.let { orderServiceName(it) } ?: "—")
        InfoCard(stringResource(Res.string.order_employee_title), apiEmployeeName ?: order?.let { orderEmployeeName(it) } ?: "—")
        InfoCard(stringResource(Res.string.order_time_title), apiOrder?.dateTime ?: order?.let { orderDateTime(it) } ?: "—")
        InfoCard(stringResource(Res.string.order_status_title), apiOrder?.status ?: order?.let { orderStatus(it) } ?: "—")
    }
}

@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    PageLayout(stringResource(Res.string.analytics_title), stringResource(Res.string.analytics_subtitle), onBack) {
        InfoCard(stringResource(Res.string.analytics_revenue_title), stringResource(Res.string.analytics_revenue_value))
        InfoCard(stringResource(Res.string.analytics_orders_title), stringResource(Res.string.analytics_orders_value))
        InfoCard(stringResource(Res.string.analytics_average_bill_title), stringResource(Res.string.analytics_average_bill_value))
        InfoCard(stringResource(Res.string.analytics_employee_load_title), stringResource(Res.string.analytics_employee_load_value))
        InfoCard(stringResource(Res.string.analytics_popular_services_title), stringResource(Res.string.analytics_popular_services_value))
    }
}

@Composable
private fun employeeName(employee: MockEmployee): String = when (employee.id) {
    "employee-1" -> stringResource(Res.string.mock_employee_anna_name)
    "employee-2" -> stringResource(Res.string.mock_employee_maria_name)
    "employee-3" -> stringResource(Res.string.mock_employee_dilnoza_name)
    else -> stringResource(Res.string.employee_title_fallback)
}

@Composable
private fun employeeRole(employee: MockEmployee): String = when (employee.id) {
    "employee-1" -> stringResource(Res.string.mock_employee_anna_role)
    "employee-2" -> stringResource(Res.string.mock_employee_maria_role)
    "employee-3" -> stringResource(Res.string.mock_employee_dilnoza_role)
    else -> "—"
}

@Composable
private fun serviceName(service: MockService): String = when (service.id) {
    "service-1" -> stringResource(Res.string.mock_service_manicure)
    "service-2" -> stringResource(Res.string.mock_service_pedicure)
    "service-3" -> stringResource(Res.string.mock_service_coloring)
    else -> stringResource(Res.string.service_title_fallback)
}

@Composable
private fun galleryPhotoTitle(photo: MockGalleryPhoto): String = when (photo.id) {
    "photo-1" -> stringResource(Res.string.mock_gallery_photo_1)
    "photo-2" -> stringResource(Res.string.mock_gallery_photo_2)
    "photo-3" -> stringResource(Res.string.mock_gallery_photo_3)
    else -> stringResource(Res.string.gallery_title)
}

@Composable
private fun orderClientName(order: MockOrder): String = when (order.id) {
    "order-1" -> stringResource(Res.string.mock_client_elena)
    "order-2" -> stringResource(Res.string.mock_client_dinara)
    "order-3" -> stringResource(Res.string.mock_client_sabina)
    else -> stringResource(Res.string.order_client_title)
}

@Composable
private fun orderServiceName(order: MockOrder): String = when (order.id) {
    "order-1" -> stringResource(Res.string.mock_service_manicure)
    "order-2" -> stringResource(Res.string.mock_service_pedicure)
    "order-3" -> stringResource(Res.string.mock_service_coloring)
    else -> stringResource(Res.string.order_service_title)
}

@Composable
private fun orderEmployeeName(order: MockOrder): String = when (order.id) {
    "order-1", "order-2" -> stringResource(Res.string.mock_employee_anna_name)
    "order-3" -> stringResource(Res.string.mock_employee_dilnoza_name)
    else -> stringResource(Res.string.order_employee_title)
}

@Composable
private fun orderDateTime(order: MockOrder): String = when (order.id) {
    "order-1" -> stringResource(Res.string.mock_order_today)
    "order-2" -> stringResource(Res.string.mock_order_tomorrow)
    "order-3" -> stringResource(Res.string.mock_order_friday)
    else -> "—"
}

@Composable
private fun orderStatus(order: MockOrder): String = when (order.id) {
    "order-1" -> stringResource(Res.string.mock_status_confirmed)
    "order-2" -> stringResource(Res.string.mock_status_new)
    "order-3" -> stringResource(Res.string.mock_status_waiting_payment)
    else -> "—"
}



