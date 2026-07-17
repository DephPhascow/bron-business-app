package com.dphascow.app.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.dphascow.app.business.BusinessWorkspaceRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.business.BookingStatus
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.DayInterval
import com.dphascow.app.business.WeeklySchedule
import com.dphascow.app.business.Weekday
import com.dphascow.app.business.serviceSummary
import com.dphascow.app.business.total
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.ui.NetworkImage
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

@Composable
internal fun PageLayout(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
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
        if (onMenu != null) {
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, contentDescription = stringResource(Res.string.account_title))
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
    onOpenReviews: () -> Unit,
    onOpenChat: () -> Unit,
    onChangeBusinessClick: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val open = stringResource(Res.string.common_open)
    PageLayout(
        title = stringResource(Res.string.dashboard_title),
        subtitle = state.business.name,
        onMenu = onOpenMenu,
    ) {
        InfoCard(stringResource(Res.string.dashboard_today_title), stringResource(Res.string.dashboard_today_subtitle))
        InfoCard(stringResource(Res.string.analytics_orders_title), stringResource(Res.string.dashboard_orders_subtitle), open, onOpenOrders)
        InfoCard(stringResource(Res.string.employees_title), stringResource(Res.string.dashboard_employees_subtitle), open, onOpenEmployees)
        InfoCard(stringResource(Res.string.services_title), stringResource(Res.string.dashboard_services_subtitle), open, onOpenServices)
        InfoCard(stringResource(Res.string.gallery_title), stringResource(Res.string.dashboard_gallery_subtitle), open, onOpenGallery)
        InfoCard(stringResource(Res.string.analytics_title), stringResource(Res.string.dashboard_analytics_subtitle), open, onOpenAnalytics)
        InfoCard(stringResource(Res.string.reviews_title), stringResource(Res.string.dashboard_reviews_subtitle), open, onOpenReviews)
        InfoCard(stringResource(Res.string.chat_title), stringResource(Res.string.dashboard_chat_subtitle), open, onOpenChat)
        InfoCard(stringResource(Res.string.business_settings_title), stringResource(Res.string.dashboard_business_settings_subtitle), open, onOpenBusinessSettings)
        if (state.canSwitchBusiness) {
            InfoCard(stringResource(Res.string.home_change_business), stringResource(Res.string.dashboard_change_business_subtitle), stringResource(Res.string.common_change), onChangeBusinessClick)
        }
        InfoCard(stringResource(Res.string.dashboard_account_title), state.phone, stringResource(Res.string.common_open), onOpenMenu)
    }
}

@Composable
fun BusinessSettingsScreen(
    workspace: BusinessWorkspace?,
    repository: BusinessWorkspaceRepository?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember(workspace?.name) { mutableStateOf(workspace?.name.orEmpty()) }
    var phone by remember(workspace?.phone) { mutableStateOf(workspace?.phone.orEmpty()) }
    var logo by remember(workspace?.id) { mutableStateOf<PickedPhoto?>(null) }
    var workTime by remember(workspace?.id) { mutableStateOf(workspace?.workTime ?: WeeklySchedule()) }
    var breakTime by remember(workspace?.id) { mutableStateOf(workspace?.breakTime ?: WeeklySchedule()) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val logoPicker = rememberPhotoPickerLauncher(onPhotoPicked = { logo = it })

    PageLayout(
        title = stringResource(Res.string.business_settings_title),
        subtitle = stringResource(Res.string.business_settings_subtitle),
        onBack = onBack,
    ) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.business_create_name_label)) }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.business_settings_phone_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))

        workspace?.logoUrl?.takeIf { logo == null }?.let { url ->
            NetworkImage(
                url = url,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(T.d.lg)),
            )
        }
        OutlinedButton(onClick = logoPicker::launch, enabled = logoPicker.isAvailable && !saving, modifier = Modifier.fillMaxWidth()) {
            Text(logo?.fileName ?: stringResource(Res.string.business_settings_logo_action))
        }

        ScheduleEditor(
            title = stringResource(Res.string.business_settings_worktime_title),
            schedule = workTime,
            enabled = !saving,
            onChange = { workTime = it },
        )
        ScheduleEditor(
            title = stringResource(Res.string.business_settings_breaktime_title),
            schedule = breakTime,
            enabled = !saving,
            onChange = { breakTime = it },
        )

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        Button(
            onClick = {
                val id = workspace?.id ?: return@Button
                if (repository == null) return@Button
                saving = true
                error = null
                scope.launch {
                    runCatching {
                        repository.saveBusinessDetails(
                            businessId = id,
                            name = name,
                            contactPhone = phone,
                            logoPhoto = logo,
                            workTime = workTime,
                            breakTime = breakTime,
                        )
                    }.onSuccess { onSaved() }
                        .onFailure { saving = false; error = it.message }
                }
            },
            enabled = !saving && workspace != null && repository != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.onPrimary)
            } else {
                Text(stringResource(Res.string.common_save))
            }
        }
    }
}

/** Default interval used when a previously-closed day is switched on. */
private val DefaultDayInterval = DayInterval(start = "09:00", stop = "18:00")

@Composable
private fun ScheduleEditor(
    title: String,
    schedule: WeeklySchedule,
    enabled: Boolean,
    onChange: (WeeklySchedule) -> Unit,
) {
    Text(title, style = T.t.t1, color = T.c.onBackground, modifier = Modifier.padding(top = T.d.sm))
    Weekday.entries.forEach { day ->
        val interval = schedule[day]
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = T.d.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day.label(), style = T.t.t3, color = T.c.onBackground, modifier = Modifier.width(44.dp))
            Switch(
                checked = interval != null,
                onCheckedChange = { on -> onChange(schedule.with(day, if (on) DefaultDayInterval else null)) },
                enabled = enabled,
            )
            Spacer(Modifier.width(T.d.sm))
            if (interval != null) {
                TimeField(
                    value = interval.start,
                    enabled = enabled,
                    onValueChange = { onChange(schedule.with(day, interval.copy(start = it))) },
                )
                Text(" – ", style = T.t.t3, color = T.c.dark7)
                TimeField(
                    value = interval.stop,
                    enabled = enabled,
                    onValueChange = { onChange(schedule.with(day, interval.copy(stop = it))) },
                )
            } else {
                Text(stringResource(Res.string.business_settings_day_closed), style = T.t.t3, color = T.c.dark7)
            }
        }
    }
}

@Composable
private fun TimeField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() || ch == ':' }.take(5)) },
        modifier = Modifier.width(96.dp),
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun Weekday.label(): String = stringResource(
    when (this) {
        Weekday.MONDAY -> Res.string.weekday_mon
        Weekday.TUESDAY -> Res.string.weekday_tue
        Weekday.WEDNESDAY -> Res.string.weekday_wed
        Weekday.THURSDAY -> Res.string.weekday_thu
        Weekday.FRIDAY -> Res.string.weekday_fri
        Weekday.SATURDAY -> Res.string.weekday_sat
        Weekday.SUNDAY -> Res.string.weekday_sun
    }
)

@Composable
fun ServicesScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onServiceClick: (String) -> Unit,
) {
    PageLayout(stringResource(Res.string.services_title), stringResource(Res.string.services_subtitle), onBack) {
        val services = workspace?.services.orEmpty()
        if (services.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.services_empty))
        }
        services.forEach { service ->
            InfoCard(service.name, "${service.duration} · ${service.price}", stringResource(Res.string.common_open)) { onServiceClick(service.id) }
        }
    }
}

@Composable
fun ServiceDetailsScreen(
    workspace: BusinessWorkspace?,
    serviceId: String,
    onBack: () -> Unit,
) {
    val service = workspace?.services?.firstOrNull { it.id == serviceId }

    PageLayout(service?.name ?: stringResource(Res.string.service_title_fallback), stringResource(Res.string.service_details_subtitle), onBack) {
        InfoCard(stringResource(Res.string.service_price_title), service?.price ?: "—")
        InfoCard(stringResource(Res.string.service_duration_title), service?.duration ?: "—")
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
        val gallery = workspace?.gallery.orEmpty()
        if (gallery.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.gallery_empty))
        }
        gallery.forEach { photo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = T.c.surface),
                shape = RoundedCornerShape(T.d.lg),
            ) {
                NetworkImage(
                    url = photo.imageUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(T.d.lg)),
                )
            }
        }
    }
}

@Composable
fun GalleryUploadScreen(
    workspace: BusinessWorkspace?,
    repository: BusinessWorkspaceRepository?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<PickedPhoto?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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
        OutlinedButton(onClick = photoPicker::launch, enabled = photoPicker.isAvailable && !uploading, modifier = Modifier.fillMaxWidth()) {
            Text(photo?.fileName ?: stringResource(Res.string.gallery_choose_photo_action))
        }

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        Button(
            onClick = {
                val id = workspace?.id ?: return@Button
                val picked = photo ?: return@Button
                if (repository == null) return@Button
                uploading = true
                error = null
                scope.launch {
                    runCatching { repository.addGalleryPhoto(id, picked) }
                        .onSuccess { onSaved() }
                        .onFailure { uploading = false; error = it.message }
                }
            },
            enabled = !uploading && photo != null && workspace != null && repository != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uploading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.onPrimary)
            } else {
                Text(stringResource(Res.string.common_save))
            }
        }
    }
}

@Composable
fun OrdersScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    PageLayout(stringResource(Res.string.orders_title), stringResource(Res.string.orders_subtitle), onBack) {
        SearchField(query) { query = it }
        val orders = workspace?.orders.orEmpty()
            .sortedByDescending { it.dateTime }
            .filter { query.isBlank() || it.clientName.contains(query, ignoreCase = true) || it.serviceSummary.contains(query, ignoreCase = true) }
        if (orders.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.orders_empty))
        }
        orders.forEach { order ->
            val employeeName = workspace?.employees?.firstOrNull { it.id == order.employeeId }?.name.orEmpty()
            InfoCard(
                title = "${order.clientName} · ${order.dateTime}",
                subtitle = listOf(order.serviceSummary, employeeName, order.status.label()).filter { it.isNotBlank() }.joinToString(" · "),
                actionText = stringResource(Res.string.common_open),
                onClick = { onOrderClick(order.id) },
            )
        }
    }
}

@Composable
internal fun BookingStatus.label(): String = when (this) {
    BookingStatus.WAITING -> stringResource(Res.string.booking_status_waiting)
    BookingStatus.SUCCESS -> stringResource(Res.string.booking_status_success)
    BookingStatus.CANCELLED -> stringResource(Res.string.booking_status_cancelled)
    BookingStatus.CLIENT_MISSING -> stringResource(Res.string.booking_status_client_missing)
    BookingStatus.UNKNOWN -> "—"
}

@Composable
fun OrderDetailsScreen(
    workspace: BusinessWorkspace?,
    orderId: String,
    repository: BusinessWorkspaceRepository?,
    chatRepository: com.dphascow.app.chat.ChatRepository?,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onOpenConversation: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var messaging by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val apiOrder = workspace?.orders?.firstOrNull { it.id == orderId }
    val apiEmployeeName = apiOrder?.let { api -> workspace?.employees?.firstOrNull { it.id == api.employeeId }?.name }

    fun run(action: suspend () -> Unit) {
        busy = true
        error = null
        scope.launch {
            runCatching { action() }
                .onSuccess { onChanged() }
                .onFailure { busy = false; error = it.message }
        }
    }

    PageLayout(stringResource(Res.string.order_title), apiOrder?.clientName ?: stringResource(Res.string.order_details_subtitle), onBack) {
        InfoCard(stringResource(Res.string.order_client_title), apiOrder?.clientName ?: "—")
        InfoCard(stringResource(Res.string.order_service_title), apiOrder?.serviceSummary ?: "—")
        apiOrder?.let { InfoCard(stringResource(Res.string.order_total_title), it.total.toString()) }
        InfoCard(stringResource(Res.string.order_employee_title), apiEmployeeName ?: "—")
        InfoCard(stringResource(Res.string.order_time_title), apiOrder?.dateTime ?: "—")
        InfoCard(stringResource(Res.string.order_status_title), apiOrder?.status?.label() ?: "—")

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        if (apiOrder != null && chatRepository != null) {
            Button(
                onClick = {
                    messaging = true
                    error = null
                    scope.launch {
                        runCatching { chatRepository.startChatWith(apiOrder.clientUserId) }
                            .onSuccess { chatId -> messaging = false; onOpenConversation(chatId) }
                            .onFailure { messaging = false; error = it.message }
                    }
                },
                enabled = !messaging && !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (messaging) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.onPrimary)
                } else {
                    Text(stringResource(Res.string.order_message_client))
                }
            }
        }

        // Status actions are only available for pending bookings.
        if (apiOrder != null && repository != null && apiOrder.status == BookingStatus.WAITING) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.primary)
            } else {
                Button(
                    onClick = { run { repository.updateBookingStatus(apiOrder.id, BookingStatus.SUCCESS) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.order_mark_done)) }
                OutlinedButton(
                    onClick = { run { repository.updateBookingStatus(apiOrder.id, BookingStatus.CLIENT_MISSING) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.order_mark_no_show)) }
                OutlinedButton(
                    onClick = { confirmCancel = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.order_cancel_action)) }
            }
        }
    }

    if (confirmCancel && apiOrder != null && repository != null) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_cancel_booking),
            confirmText = stringResource(Res.string.order_cancel_action),
            onConfirm = {
                confirmCancel = false
                run { repository.cancelBooking(apiOrder.id) }
            },
            onDismiss = { confirmCancel = false },
        )
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
fun ReviewsScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
) {
    PageLayout(stringResource(Res.string.reviews_title), stringResource(Res.string.reviews_subtitle), onBack) {
        val reviews = workspace?.reviews.orEmpty()
        InfoCard(
            title = "★ ${workspace?.rating.oneDecimal()}",
            subtitle = stringResource(Res.string.reviews_count_value, workspace?.reviewsCount ?: 0),
        )
        if (reviews.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.reviews_empty))
        }
        reviews.forEach { review ->
            InfoCard(title = "★ ${review.mark}", subtitle = review.comment)
        }
    }
}

private fun Double?.oneDecimal(): String {
    val value = this ?: 0.0
    return (kotlin.math.round(value * 10) / 10.0).toString()
}

@Composable
internal fun WorkspaceStatusScreen(
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    PageLayout(stringResource(Res.string.app_title), null, onBack) {
        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(T.d.lg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = T.c.primary)
            }
        } else {
            EmptyStateCard(error ?: stringResource(Res.string.workspace_load_error))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.common_retry))
            }
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(stringResource(Res.string.action_irreversible)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
    )
}

@Composable
internal fun SearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(Res.string.common_search)) },
        singleLine = true,
    )
}

@Composable
internal fun EmptyStateCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = T.c.surface),
        shape = RoundedCornerShape(T.d.lg),
    ) {
        Text(
            text = text,
            color = T.c.dark7,
            style = T.t.t2Regular,
            modifier = Modifier.fillMaxWidth().padding(T.d.lg),
        )
    }
}



