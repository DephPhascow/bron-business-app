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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.dphascow.app.business.BusinessWorkspaceRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.business.BookingStatus
import com.dphascow.app.business.BusinessAnalytics
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.DayInterval
import com.dphascow.app.business.WeeklySchedule
import com.dphascow.app.business.Weekday
import com.dphascow.app.business.serviceSummary
import com.dphascow.app.business.total
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.ui.AccentPanel
import com.dphascow.app.ui.AppButton
import com.dphascow.app.ui.AppOutlinedButton
import com.dphascow.app.ui.AppRowItem
import com.dphascow.app.ui.AppTextField
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
            // The client app draws its pages straight on `dark1`, not on the tinted
            // `background` — cards then read as content, not as floating panels.
            .background(T.c.dark1)
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(T.d.xs)) {
            // Title sits on the same line as the back arrow / hamburger, so the header
            // always reads as "<icon> <where you are>".
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(T.d.xs),
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                            tint = T.c.dark10,
                        )
                    }
                }
                if (onMenu != null) {
                    IconButton(onClick = onMenu) {
                        Icon(
                            Icons.Outlined.Menu,
                            contentDescription = stringResource(Res.string.account_title),
                            tint = T.c.dark10,
                        )
                    }
                }
                Text(
                    text = title,
                    color = T.c.dark10,
                    style = T.t.headingH3,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = T.c.dark5,
                    style = T.t.t3,
                )
            }
        }

        content()
    }
}

/**
 * A list entry: title, secondary line, and an optional text affordance on the right.
 * The client app builds its lists from flat rows rather than elevated cards, so a
 * whole screen of these reads as one surface.
 */
@Composable
internal fun InfoCard(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AppRowItem(
        title = title,
        subtitle = subtitle,
        actionText = actionText?.takeIf { onClick != null },
        onClick = onClick,
    )
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
        AppButton(text = primaryText, onClick = onPrimaryClick, modifier = Modifier.weight(1f))
        if (secondaryText != null && onSecondaryClick != null) {
            AppOutlinedButton(text = secondaryText, onClick = onSecondaryClick, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DashboardScreen(
    state: AppUiState.Authorized,
    onOpenBusinessSettings: () -> Unit,
    onOpenEmployees: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSchedule: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenReviews: () -> Unit,
    onChangeBusinessClick: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val open = stringResource(Res.string.common_open)
    PageLayout(
        title = stringResource(Res.string.dashboard_title),
        subtitle = state.business.name,
        onMenu = onOpenMenu,
    ) {
        InfoCard(stringResource(Res.string.analytics_orders_title), stringResource(Res.string.dashboard_orders_subtitle), open, onOpenOrders)
        InfoCard(stringResource(Res.string.schedule_title), stringResource(Res.string.dashboard_schedule_subtitle), open, onOpenSchedule)
        InfoCard(stringResource(Res.string.employees_title), stringResource(Res.string.dashboard_employees_subtitle), open, onOpenEmployees)
        InfoCard(stringResource(Res.string.gallery_title), stringResource(Res.string.dashboard_gallery_subtitle), open, onOpenGallery)
        InfoCard(stringResource(Res.string.analytics_title), stringResource(Res.string.dashboard_analytics_subtitle), open, onOpenAnalytics)
        InfoCard(stringResource(Res.string.reviews_title), stringResource(Res.string.dashboard_reviews_subtitle), open, onOpenReviews)
        // Chat lives in the bottom bar — a card here would be a second door to the same room.
        InfoCard(stringResource(Res.string.business_settings_title), stringResource(Res.string.dashboard_business_settings_subtitle), open, onOpenBusinessSettings)
        if (state.canSwitchBusiness) {
            InfoCard(stringResource(Res.string.home_change_business), stringResource(Res.string.dashboard_change_business_subtitle), stringResource(Res.string.common_change), onChangeBusinessClick)
        }
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
        AppTextField(name, { name = it }, stringResource(Res.string.business_create_name_label), enabled = !saving)
        AppTextField(
            phone,
            { phone = it },
            stringResource(Res.string.business_settings_phone_label),
            enabled = !saving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )

        workspace?.logoUrl?.takeIf { logo == null }?.let { url ->
            NetworkImage(
                url = url,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f).clip(RoundedCornerShape(T.d.lg)),
            )
        }
        AppOutlinedButton(
            text = logo?.fileName ?: stringResource(Res.string.business_settings_logo_action),
            onClick = logoPicker::launch,
            enabled = logoPicker.isAvailable && !saving,
        )

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

        AppButton(
            text = stringResource(Res.string.common_save),
            loading = saving,
            enabled = workspace != null && repository != null,
            onClick = {
                val id = workspace?.id ?: return@AppButton
                if (repository == null) return@AppButton
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
        )
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
    Text(title, style = T.t.t1, color = T.c.dark10, modifier = Modifier.padding(top = T.d.sm))
    Weekday.entries.forEach { day ->
        val interval = schedule[day]
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = T.d.xxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(day.label(), style = T.t.t3, color = T.c.dark10, modifier = Modifier.width(44.dp))
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
                Text(" – ", style = T.t.t3, color = T.c.dark5)
                TimeField(
                    value = interval.stop,
                    enabled = enabled,
                    onValueChange = { onChange(schedule.with(day, interval.copy(stop = it))) },
                )
            } else {
                Text(stringResource(Res.string.business_settings_day_closed), style = T.t.t3, color = T.c.dark5)
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
fun GalleryScreen(
    workspace: BusinessWorkspace?,
    repository: BusinessWorkspaceRepository?,
    onBack: () -> Unit,
    onUploadClick: () -> Unit,
    onChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var deletingId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    PageLayout(stringResource(Res.string.gallery_title), stringResource(Res.string.gallery_subtitle), onBack) {
        AccentPanel(stringResource(Res.string.gallery_upload_title_card), stringResource(Res.string.gallery_upload_subtitle_card), stringResource(Res.string.common_upload), onUploadClick)
        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }
        val gallery = workspace?.gallery.orEmpty()
        if (gallery.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.gallery_empty))
        }
        gallery.forEach { photo ->
            Column(verticalArrangement = Arrangement.spacedBy(T.d.sm)) {
                NetworkImage(
                    url = photo.imageUrl,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(20.dp)),
                )
                AppOutlinedButton(
                    text = stringResource(Res.string.gallery_delete_action),
                    onClick = { confirmDeleteId = photo.id },
                    enabled = repository != null && deletingId == null,
                    loading = deletingId == photo.id,
                )
            }
        }
    }

    confirmDeleteId?.let { photoId ->
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_photo),
            confirmText = stringResource(Res.string.gallery_delete_action),
            onConfirm = {
                confirmDeleteId = null
                val repo = repository ?: return@ConfirmDialog
                val id = workspace?.id ?: return@ConfirmDialog
                deletingId = photoId
                error = null
                scope.launch {
                    runCatching { repo.deleteGalleryPhoto(id, photoId) }
                        .onSuccess { deletingId = null; onChanged() }
                        .onFailure { deletingId = null; error = it.message }
                }
            },
            onDismiss = { confirmDeleteId = null },
        )
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
        AppOutlinedButton(
            text = photo?.fileName ?: stringResource(Res.string.gallery_choose_photo_action),
            onClick = photoPicker::launch,
            enabled = photoPicker.isAvailable && !uploading,
        )

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        AppButton(
            text = stringResource(Res.string.common_save),
            loading = uploading,
            enabled = photo != null && workspace != null && repository != null,
            onClick = {
                val id = workspace?.id ?: return@AppButton
                val picked = photo ?: return@AppButton
                if (repository == null) return@AppButton
                uploading = true
                error = null
                scope.launch {
                    runCatching { repository.addGalleryPhoto(id, picked) }
                        .onSuccess { onSaved() }
                        .onFailure { uploading = false; error = it.message }
                }
            },
        )
    }
}

@Composable
fun OrdersScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onOrderClick: (String) -> Unit,
    onBookClientClick: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    PageLayout(stringResource(Res.string.orders_title), stringResource(Res.string.orders_subtitle), onBack) {
        AccentPanel(
            stringResource(Res.string.book_client_title),
            stringResource(Res.string.book_client_subtitle),
            stringResource(Res.string.common_add),
            onBookClientClick,
        )
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
    businessId: String,
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
    var rescheduling by remember { mutableStateOf(false) }
    var newDate by remember { mutableStateOf("") }
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
            AppButton(
                text = stringResource(Res.string.order_message_client),
                loading = messaging,
                enabled = !busy,
                onClick = {
                    messaging = true
                    error = null
                    scope.launch {
                        runCatching { chatRepository.startChatWith(apiOrder.clientUserId) }
                            .onSuccess { chatId -> messaging = false; onOpenConversation(chatId) }
                            .onFailure { messaging = false; error = it.message }
                    }
                },
            )
        }

        // Status actions are only available for pending bookings.
        if (apiOrder != null && repository != null && apiOrder.status == BookingStatus.WAITING) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.primary)
            } else {
                AppButton(
                    text = stringResource(Res.string.order_mark_done),
                    onClick = { run { repository.completeBooking(businessId, apiOrder.id) } },
                )
                AppOutlinedButton(
                    text = stringResource(Res.string.order_mark_no_show),
                    onClick = { run { repository.markBookingClientMissing(businessId, apiOrder.id) } },
                )

                if (rescheduling) {
                    DateTimeField(
                        label = stringResource(Res.string.order_reschedule_label),
                        value = newDate,
                        onValueChange = { newDate = it; error = null },
                    )
                    ActionRow(
                        primaryText = stringResource(Res.string.order_reschedule_action),
                        onPrimaryClick = {
                            if (newDate.isNotBlank()) run { repository.rescheduleBooking(apiOrder.id, newDate) }
                        },
                        secondaryText = stringResource(Res.string.common_cancel),
                        onSecondaryClick = { rescheduling = false; newDate = "" },
                    )
                } else {
                    AppOutlinedButton(
                        text = stringResource(Res.string.order_reschedule_action),
                        onClick = { rescheduling = true; newDate = apiOrder.dateTime },
                    )
                }

                AppOutlinedButton(
                    text = stringResource(Res.string.order_cancel_action),
                    onClick = { confirmCancel = true },
                )
            }
        }
    }

    if (confirmCancel && apiOrder != null && repository != null) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_cancel_booking),
            confirmText = stringResource(Res.string.order_cancel_action),
            onConfirm = {
                confirmCancel = false
                run { repository.cancelBookingBySalon(businessId, apiOrder.id) }
            },
            onDismiss = { confirmCancel = false },
        )
    }
}

@Composable
fun AnalyticsScreen(
    repository: BusinessWorkspaceRepository?,
    businessId: String,
    lang: String,
    onBack: () -> Unit,
) {
    var analytics by remember(businessId) { mutableStateOf<BusinessAnalytics?>(null) }
    var loading by remember(businessId) { mutableStateOf(true) }
    var error by remember(businessId) { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, businessId, lang) {
        loading = true
        error = null
        val repo = repository
        if (repo == null) {
            loading = false
            return@LaunchedEffect
        }
        runCatching { repo.loadAnalytics(businessId = businessId, lang = lang) }
            .onSuccess { analytics = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    PageLayout(stringResource(Res.string.analytics_title), stringResource(Res.string.analytics_subtitle), onBack) {
        when {
            loading -> Box(modifier = Modifier.fillMaxWidth().padding(T.d.lg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = T.c.primary)
            }

            analytics == null -> EmptyStateCard(error ?: stringResource(Res.string.workspace_load_error))

            else -> {
                val data = analytics!!
                InfoCard(
                    stringResource(Res.string.analytics_period_title),
                    "${data.periodStart} — ${data.periodEnd}",
                )
                InfoCard(
                    stringResource(Res.string.analytics_revenue_title),
                    listOfNotNull(
                        data.revenue.toString(),
                        data.revenueGrowthPercent.asGrowth(),
                    ).joinToString(" · "),
                )
                InfoCard(stringResource(Res.string.analytics_expected_revenue_title), data.expectedRevenue.toString())
                InfoCard(
                    stringResource(Res.string.analytics_orders_title),
                    listOfNotNull(
                        data.bookingsCount.toString(),
                        data.bookingsGrowthPercent.asGrowth(),
                    ).joinToString(" · "),
                )
                InfoCard(stringResource(Res.string.analytics_average_bill_title), data.averageCheck.toString())
                InfoCard(
                    stringResource(Res.string.analytics_cancelled_title),
                    "${data.cancelledCount} · ${data.noShowCount}",
                )

                Text(stringResource(Res.string.analytics_employee_load_title), color = T.c.dark5, style = T.t.t4SamiBold)
                if (data.employeesLoad.isEmpty()) {
                    EmptyStateCard(stringResource(Res.string.analytics_empty))
                }
                data.employeesLoad.forEach { load ->
                    InfoCard(
                        load.employeeName,
                        "${load.loadPercent.oneDecimal()}% · ${load.bookedMinutes}/${load.availableMinutes}",
                    )
                }

                Text(stringResource(Res.string.analytics_popular_services_title), color = T.c.dark5, style = T.t.t4SamiBold)
                if (data.popularServices.isEmpty()) {
                    EmptyStateCard(stringResource(Res.string.analytics_empty))
                }
                data.popularServices.forEach { service ->
                    InfoCard(service.name, "${service.bookingsCount} · ${service.revenue}")
                }
            }
        }
    }
}

/** Growth is `null` when there is no earlier period — the doc asks for a dash, not "0%". */
private fun Double?.asGrowth(): String = if (this == null) "—" else "${if (this >= 0) "+" else ""}${oneDecimal()}%"

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
            AppButton(text = stringResource(Res.string.common_retry), onClick = onRetry)
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
        containerColor = T.c.dark1,
        titleContentColor = T.c.dark10,
        textContentColor = T.c.dark5,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, style = T.t.t1) },
        text = { Text(stringResource(Res.string.action_irreversible), style = T.t.t3) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = T.c.redError) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel), color = T.c.dark10) }
        },
    )
}

/** Filled `graniteGreen7` search bar, matching the client app's header search. */
@Composable
internal fun SearchField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(T.c.graniteGreen7, RoundedCornerShape(15.dp)),
        singleLine = true,
        maxLines = 1,
        textStyle = T.t.t3,
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = T.c.dark1)
        },
        placeholder = { Text(stringResource(Res.string.common_search), color = T.c.dark1) },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = T.c.graniteGreen7,
            unfocusedIndicatorColor = T.c.graniteGreen7,
            focusedContainerColor = T.c.graniteGreen7,
            unfocusedContainerColor = T.c.graniteGreen7,
            cursorColor = T.c.dark1,
            focusedTextColor = T.c.dark1,
            unfocusedTextColor = T.c.dark1,
        ),
    )
}

@Composable
internal fun EmptyStateCard(text: String) {
    Text(
        text = text,
        color = T.c.dark5,
        style = T.t.t3,
        modifier = Modifier.fillMaxWidth().padding(vertical = T.d.lg),
    )
}



