package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dphascow.app.business.BookingStatus
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.business.EmployeeBooking
import com.dphascow.app.business.serviceSummary
import com.dphascow.app.business.total
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

/**
 * The signed-in specialist's own day. Marking the visit here is what puts a booking
 * into revenue — an unmarked booking is auto-flagged as a no-show after closing time.
 */
@Composable
fun MyScheduleScreen(
    repository: BusinessWorkspaceRepository?,
    businessId: String,
    lang: String,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var bookings by remember(businessId) { mutableStateOf<List<EmployeeBooking>>(emptyList()) }
    var loading by remember(businessId) { mutableStateOf(true) }
    var error by remember(businessId) { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember(businessId) { mutableStateOf(0) }

    LaunchedEffect(repository, businessId, lang, reloadKey) {
        loading = true
        error = null
        val repo = repository
        if (repo == null) {
            loading = false
            return@LaunchedEffect
        }
        runCatching { repo.loadMyBookings(businessId = businessId, lang = lang) }
            .onSuccess { bookings = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    fun mark(bookingId: String, action: suspend () -> Unit) {
        busyId = bookingId
        error = null
        scope.launch {
            runCatching { action() }
                .onSuccess { busyId = null; reloadKey++ }
                .onFailure { busyId = null; error = it.message }
        }
    }

    PageLayout(stringResource(Res.string.schedule_title), stringResource(Res.string.schedule_subtitle), onBack) {
        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(T.d.lg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = T.c.primary)
            }
            return@PageLayout
        }

        if (bookings.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.schedule_empty))
        }

        bookings.forEach { booking ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = T.c.surface),
                shape = RoundedCornerShape(T.d.lg),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(T.d.lg),
                    verticalArrangement = Arrangement.spacedBy(T.d.sm),
                ) {
                    Text("${booking.startsAt} — ${booking.endsAt}", color = T.c.onSurface, style = T.t.t2Bold)
                    Text(
                        listOfNotNull(booking.clientName, booking.clientPhone).joinToString(" · "),
                        color = T.c.dark7,
                        style = T.t.t2Regular,
                    )
                    Text(
                        "${booking.serviceSummary} · ${booking.total} · ${booking.status.label()}",
                        color = T.c.dark7,
                        style = T.t.t3,
                    )
                    booking.comment?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = T.c.dark7, style = T.t.t4)
                    }

                    // Only a booking still waiting can be closed out.
                    if (booking.status == BookingStatus.WAITING && repository != null) {
                        if (busyId == booking.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(vertical = 2.dp),
                                strokeWidth = 2.dp,
                                color = T.c.primary,
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(T.d.md),
                            ) {
                                Button(
                                    onClick = { mark(booking.id) { repository.completeBooking(businessId, booking.id) } },
                                    enabled = busyId == null,
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(Res.string.schedule_client_came)) }
                                OutlinedButton(
                                    onClick = {
                                        mark(booking.id) { repository.markBookingClientMissing(businessId, booking.id) }
                                    },
                                    enabled = busyId == null,
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(Res.string.schedule_client_missing)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Books a walk-in or a caller: the salon picks the specialist, services and time. */
@Composable
fun BookClientScreen(
    workspace: BusinessWorkspace?,
    repository: BusinessWorkspaceRepository?,
    businessId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var employeeId by remember { mutableStateOf<String?>(null) }
    var serviceIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var date by remember { mutableStateOf("") }
    var clientPhone by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val employees = workspace?.employees.orEmpty().filter { it.isActive }
    val selectedEmployee = employees.firstOrNull { it.id == employeeId }
    // Services belong to the specialist, so the list only makes sense once one is picked.
    val services = selectedEmployee?.services.orEmpty().filter { it.isActive }

    PageLayout(stringResource(Res.string.book_client_title), stringResource(Res.string.book_client_subtitle), onBack) {
        Text(stringResource(Res.string.book_client_employee_label), color = T.c.dark7, style = T.t.t4SamiBold)
        employees.forEach { employee ->
            InfoCard(
                title = employee.name,
                subtitle = employee.role.label(),
                actionText = if (employee.id == employeeId) {
                    stringResource(Res.string.book_client_selected)
                } else {
                    stringResource(Res.string.book_client_select)
                },
                onClick = { employeeId = employee.id; serviceIds = emptySet(); error = null },
            )
        }

        if (selectedEmployee != null) {
            Text(stringResource(Res.string.book_client_services_label), color = T.c.dark7, style = T.t.t4SamiBold)
            if (services.isEmpty()) {
                EmptyStateCard(stringResource(Res.string.services_empty))
            }
            services.forEach { service ->
                InfoCard(
                    title = service.name,
                    subtitle = "${service.duration} · ${service.cost}",
                    actionText = if (service.id in serviceIds) {
                        stringResource(Res.string.book_client_selected)
                    } else {
                        stringResource(Res.string.book_client_select)
                    },
                    onClick = {
                        serviceIds = if (service.id in serviceIds) serviceIds - service.id else serviceIds + service.id
                        error = null
                    },
                )
            }
        }

        DateTimeField(
            label = stringResource(Res.string.book_client_date_label),
            value = date,
            enabled = !submitting,
            onValueChange = { date = it; error = null },
        )
        OutlinedTextField(
            value = clientPhone,
            onValueChange = { clientPhone = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.auth_phone_label)) },
            placeholder = { Text(stringResource(Res.string.auth_phone_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        OutlinedTextField(
            value = clientName,
            onValueChange = { clientName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.book_client_name_label)) },
            singleLine = true,
        )

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        Button(
            onClick = {
                val repo = repository ?: return@Button
                val employee = employeeId ?: return@Button
                submitting = true
                error = null
                scope.launch {
                    runCatching {
                        repo.bookClientForDate(
                            businessId = businessId,
                            employeeId = employee,
                            serviceIds = serviceIds.toList(),
                            date = date,
                            clientPhone = clientPhone,
                            clientName = clientName,
                        )
                    }
                        .onSuccess { onSaved() }
                        .onFailure { submitting = false; error = it.message }
                }
            },
            enabled = !submitting && repository != null && employeeId != null && serviceIds.isNotEmpty() &&
                date.isNotBlank() && clientPhone.count { it.isDigit() } >= 9,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (submitting) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.onPrimary)
            } else {
                Text(stringResource(Res.string.book_client_submit))
            }
        }
    }
}
