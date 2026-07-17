package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dphascow.app.business.BusinessEmployee
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.business.EmployeeRole
import com.dphascow.app.business.Specialisation
import com.dphascow.app.ui.NetworkImage
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

@Composable
internal fun EmployeeRole.label(): String = when (this) {
    EmployeeRole.ADMIN -> stringResource(Res.string.employee_role_admin)
    EmployeeRole.SPECIALIST -> stringResource(Res.string.employee_role_specialist)
}

@Composable
fun EmployeesScreen(
    workspace: BusinessWorkspace?,
    onBack: () -> Unit,
    onEmployeeClick: (String) -> Unit,
    onAddEmployeeClick: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    PageLayout(stringResource(Res.string.employees_title), stringResource(Res.string.employees_subtitle), onBack) {
        InfoCard(
            stringResource(Res.string.employees_add_title),
            stringResource(Res.string.employees_add_subtitle),
            stringResource(Res.string.common_add),
            onAddEmployeeClick,
        )
        SearchField(query) { query = it }
        workspace?.employees.orEmpty()
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .forEach { employee ->
            val subtitle = listOfNotNull(employee.role.label(), employee.phone).joinToString(" · ")
            InfoCard(employee.name, subtitle, stringResource(Res.string.common_open)) { onEmployeeClick(employee.id) }
        }
    }
}

@Composable
fun EmployeeDetailsScreen(
    employee: BusinessEmployee?,
    repository: BusinessWorkspaceRepository?,
    chatRepository: com.dphascow.app.chat.ChatRepository?,
    lang: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(false) }
    var messaging by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (employee == null) {
        PageLayout(stringResource(Res.string.employee_title_fallback), null, onBack) {
            Text(stringResource(Res.string.employee_not_found), color = T.c.dark7, style = T.t.t2Regular)
        }
        return
    }

    val contacts = listOfNotNull(employee.phone, employee.email).joinToString("\n").ifBlank { "—" }

    PageLayout(employee.name, employee.role.label(), onBack) {
        employee.avatarUrl?.let { url ->
            NetworkImage(
                url = url,
                modifier = Modifier.size(96.dp).clip(CircleShape).align(Alignment.CenterHorizontally),
            )
        }
        InfoCard(stringResource(Res.string.employee_contacts_title), contacts)
        employee.specialisationName?.let { spec ->
            InfoCard(stringResource(Res.string.employee_specialisation_label), spec)
        }
        InfoCard(
            stringResource(Res.string.employee_access_title),
            stringResource(Res.string.employee_role_value, employee.role.label()),
            stringResource(Res.string.common_edit),
            onEditClick,
        )

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        Button(
            onClick = {
                val chat = chatRepository ?: return@Button
                messaging = true
                error = null
                scope.launch {
                    runCatching { chat.startChatWith(employee.userId) }
                        .onSuccess { chatId -> messaging = false; onOpenConversation(chatId) }
                        .onFailure { messaging = false; error = it.message }
                }
            },
            enabled = !messaging && !deleting && chatRepository != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (messaging) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.onPrimary)
            } else {
                Text(stringResource(Res.string.employee_message_action))
            }
        }

        OutlinedButton(
            onClick = { confirmDelete = true },
            enabled = !deleting && repository != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (deleting) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.primary)
            } else {
                Text(stringResource(Res.string.employee_delete_action))
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_employee),
            confirmText = stringResource(Res.string.employee_delete_action),
            onConfirm = {
                confirmDelete = false
                val repo = repository ?: return@ConfirmDialog
                deleting = true
                error = null
                scope.launch {
                    runCatching { repo.deleteEmployee(employee.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { deleting = false; error = it.message ?: "Error" }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private enum class InviteStage { ENTER_PHONE, ENTER_CODE, VERIFIED }

@Composable
fun EmployeeEditScreen(
    repository: BusinessWorkspaceRepository?,
    businessId: String,
    lang: String,
    employee: BusinessEmployee?,
    currentUserId: String? = null,
    isCurrentUserEmployee: Boolean = false,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isAdd = employee == null
    // Offer "add myself" only when adding and the signed-in user is not yet an employee.
    val canAddSelf = isAdd && currentUserId != null && !isCurrentUserEmployee
    var addSelf by remember { mutableStateOf(false) }

    var specialisations by remember { mutableStateOf<List<Specialisation>>(emptyList()) }
    LaunchedEffect(repository, lang) {
        if (repository != null) {
            specialisations = runCatching { repository.loadSpecialisations(lang) }.getOrDefault(emptyList())
        }
    }

    var role by remember(employee) { mutableStateOf(employee?.role ?: EmployeeRole.SPECIALIST) }
    var specialisationId by remember(employee) { mutableStateOf(employee?.specialisationId) }
    var isActive by remember(employee) { mutableStateOf(employee?.isActive ?: true) }

    // Invite flow (add mode only)
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(InviteStage.ENTER_PHONE) }
    var invitedUserId by remember { mutableStateOf<String?>(null) }

    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val inviteFailedText = stringResource(Res.string.employee_invite_failed)
    val title = if (isAdd) stringResource(Res.string.employee_add_title) else stringResource(Res.string.employee_edit_title)

    PageLayout(title, stringResource(Res.string.employee_edit_subtitle), onBack) {
        if (canAddSelf) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
                Checkbox(checked = addSelf, onCheckedChange = { addSelf = it; error = null })
                Text(stringResource(Res.string.employee_add_self), color = T.c.onSurface, style = T.t.t3SemiBold)
            }
        }

        if (isAdd && !addSelf && stage != InviteStage.VERIFIED) {
            // Step 1-2: invite by phone + confirm code.
            when (stage) {
                InviteStage.ENTER_PHONE -> {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.auth_phone_label)) },
                        placeholder = { Text(stringResource(Res.string.auth_phone_placeholder)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                    error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }
                    SubmitButton(
                        text = stringResource(Res.string.employee_invite_action),
                        loading = submitting,
                        enabled = repository != null && phone.count { it.isDigit() } >= 9,
                    ) {
                        submitting = true
                        error = null
                        scope.launch {
                            runCatching { repository!!.requireEmployeeCode(phone) }
                                .onSuccess { sent ->
                                    submitting = false
                                    if (sent) stage = InviteStage.ENTER_CODE
                                    else error = inviteFailedText
                                }
                                .onFailure { submitting = false; error = it.message }
                        }
                    }
                }

                InviteStage.ENTER_CODE -> {
                    Text(
                        stringResource(Res.string.employee_invite_code_hint, phone),
                        color = T.c.dark7,
                        style = T.t.t3,
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { raw -> code = raw.filter(Char::isDigit).take(4); error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.employee_invite_code_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    )
                    error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }
                    ActionRow(
                        primaryText = stringResource(Res.string.employee_invite_verify_action),
                        onPrimaryClick = {
                            if (code.length != 4 || repository == null) return@ActionRow
                            submitting = true
                            error = null
                            scope.launch {
                                runCatching { repository!!.verifyEmployeePhone(phone, code) }
                                    .onSuccess { userId ->
                                        submitting = false
                                        invitedUserId = userId
                                        stage = InviteStage.VERIFIED
                                    }
                                    .onFailure { submitting = false; error = it.message }
                            }
                        },
                        secondaryText = stringResource(Res.string.common_back),
                        onSecondaryClick = { stage = InviteStage.ENTER_PHONE; code = "" },
                    )
                }

                InviteStage.VERIFIED -> Unit
            }
        } else {
            // Step 3 (add) or edit: role / specialisation / active + save.
            if (isAdd) {
                val header = if (addSelf) {
                    stringResource(Res.string.employee_add_self_hint)
                } else {
                    stringResource(Res.string.employee_invite_verified, phone)
                }
                Text(header, color = T.c.dark7, style = T.t.t3)
            }

            // Adding yourself defaults to the specialist role — no role picker needed.
            if (!addSelf) {
                RoleSelector(role) { role = it }
            }
            SpecialisationSelector(specialisations, specialisationId) { specialisationId = it }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
                Switch(checked = isActive, onCheckedChange = { isActive = it })
                Text(stringResource(Res.string.employee_active_label), color = T.c.onSurface, style = T.t.t3SemiBold)
            }

            error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

            val effectiveUserId = if (addSelf) currentUserId else invitedUserId
            SubmitButton(
                text = if (isAdd) stringResource(Res.string.employee_add_submit) else stringResource(Res.string.common_save),
                loading = submitting,
                enabled = repository != null && specialisationId != null && (!isAdd || effectiveUserId != null),
            ) {
                submitting = true
                error = null
                scope.launch {
                    val result = runCatching {
                        if (isAdd) {
                            repository!!.addEmployee(
                                businessId = businessId,
                                userId = effectiveUserId!!,
                                role = if (addSelf) EmployeeRole.SPECIALIST else role,
                                specialisationId = specialisationId,
                                isActive = isActive,
                                lang = lang,
                            )
                        } else {
                            repository!!.updateEmployee(
                                employeeId = employee!!.id,
                                role = role,
                                specialisationId = specialisationId,
                                isActive = isActive,
                                lang = lang,
                            )
                        }
                    }
                    result.onSuccess { onSaved() }
                        .onFailure { submitting = false; error = it.message }
                }
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !submitting) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    }
}

@Composable
private fun RoleSelector(selected: EmployeeRole, onSelected: (EmployeeRole) -> Unit) {
    Text(stringResource(Res.string.employee_role_label), color = T.c.dark7, style = T.t.t4SamiBold)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.d.md)) {
        EmployeeRole.entries.forEach { option ->
            if (option == selected) {
                Button(onClick = { onSelected(option) }, modifier = Modifier.weight(1f)) { Text(option.label()) }
            } else {
                OutlinedButton(onClick = { onSelected(option) }, modifier = Modifier.weight(1f)) { Text(option.label()) }
            }
        }
    }
}

@Composable
private fun SpecialisationSelector(
    options: List<Specialisation>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name
        ?: stringResource(Res.string.employee_specialisation_select)

    Text(stringResource(Res.string.employee_specialisation_label), color = T.c.dark7, style = T.t.t4SamiBold)
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onSelected(option.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SubmitButton(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.padding(vertical = 2.dp), strokeWidth = 2.dp, color = T.c.onPrimary)
        } else {
            Text(text)
        }
    }
}
