package com.dphascow.app.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dphascow.app.business.BusinessEmployee
import com.dphascow.app.business.BusinessService
import com.dphascow.app.business.BusinessWorkspace
import com.dphascow.app.business.BusinessWorkspaceRepository
import com.dphascow.app.business.EmployeeRole
import com.dphascow.app.business.SERVICE_NAME_LANGS
import com.dphascow.app.business.ServiceCategory
import com.dphascow.app.business.Specialisation
import com.dphascow.app.business.specialisationSummary
import com.dphascow.app.ui.AccentPanel
import com.dphascow.app.ui.AppButton
import com.dphascow.app.ui.AppOutlinedButton
import com.dphascow.app.ui.AppTextField
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
        AccentPanel(
            stringResource(Res.string.employees_add_title),
            stringResource(Res.string.employees_add_subtitle),
            stringResource(Res.string.common_add),
            onAddEmployeeClick,
        )
        SearchField(query) { query = it }
        workspace?.employees.orEmpty()
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .forEach { employee ->
            val dismissed = stringResource(Res.string.employee_dismissed_label).takeIf { !employee.isActive }
            val subtitle = listOfNotNull(employee.role.label(), employee.phone, dismissed).joinToString(" · ")
            InfoCard(employee.name, subtitle, stringResource(Res.string.common_open)) { onEmployeeClick(employee.id) }
        }
    }
}

@Composable
fun EmployeeDetailsScreen(
    employee: BusinessEmployee?,
    repository: BusinessWorkspaceRepository?,
    chatRepository: com.dphascow.app.chat.ChatRepository?,
    businessId: String,
    lang: String,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onAddServiceClick: () -> Unit,
    onEditServiceClick: (String) -> Unit,
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
            Text(stringResource(Res.string.employee_not_found), color = T.c.dark5, style = T.t.t2Regular)
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
        employee.specialisationSummary?.let { spec ->
            InfoCard(stringResource(Res.string.employee_specialisation_label), spec)
        }
        InfoCard(
            stringResource(Res.string.employee_access_title),
            stringResource(Res.string.employee_role_value, employee.role.label()),
            stringResource(Res.string.common_edit),
            onEditClick,
        )

        Text(stringResource(Res.string.services_title), color = T.c.dark5, style = T.t.t4SamiBold)
        AccentPanel(
            stringResource(Res.string.employee_service_add_title),
            stringResource(Res.string.employee_service_add_subtitle),
            stringResource(Res.string.common_add),
            onAddServiceClick,
        )
        if (employee.services.isEmpty()) {
            EmptyStateCard(stringResource(Res.string.services_empty))
        }
        employee.services.forEach { service ->
            InfoCard(
                service.name,
                "${service.duration} · ${service.cost}",
                stringResource(Res.string.common_edit),
            ) { onEditServiceClick(service.id) }
        }

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        AppButton(
            text = stringResource(Res.string.employee_message_action),
            loading = messaging,
            enabled = !deleting && chatRepository != null,
            onClick = {
                val chat = chatRepository ?: return@AppButton
                messaging = true
                error = null
                scope.launch {
                    runCatching { chat.startChatWith(employee.userId) }
                        .onSuccess { chatId -> messaging = false; onOpenConversation(chatId) }
                        .onFailure { messaging = false; error = it.message }
                }
            },
        )

        if (employee.isActive) {
            AppOutlinedButton(
                text = stringResource(Res.string.employee_delete_action),
                loading = deleting,
                enabled = repository != null,
                onClick = { confirmDelete = true },
            )
        } else {
            // Dismissal only deactivates, and hiring the same phone again reinstates
            // the very same record instead of creating a duplicate.
            AppOutlinedButton(
                text = stringResource(Res.string.employee_rehire_action),
                loading = deleting,
                enabled = repository != null && employee.phone != null,
                onClick = {
                    val repo = repository ?: return@AppOutlinedButton
                    val phone = employee.phone ?: return@AppOutlinedButton
                    deleting = true
                    error = null
                    scope.launch {
                        runCatching {
                            repo.hireEmployee(
                                businessId = businessId,
                                phone = phone,
                                role = employee.role,
                                specialisationIds = employee.specialisations.map { it.id },
                                lang = lang,
                            )
                        }
                            .onSuccess { onDeleted() }
                            .onFailure { deleting = false; error = it.message }
                    }
                },
            )
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
                    runCatching { repo.deleteEmployee(businessId, employee.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { deleting = false; error = it.message ?: "Error" }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun EmployeeEditScreen(
    repository: BusinessWorkspaceRepository?,
    businessId: String,
    lang: String,
    employee: BusinessEmployee?,
    currentUserPhone: String? = null,
    isCurrentUserEmployee: Boolean = false,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isAdd = employee == null
    // Offer "add myself" only when adding and the signed-in user is not yet an employee.
    val canAddSelf = isAdd && !currentUserPhone.isNullOrBlank() && !isCurrentUserEmployee
    var addSelf by remember { mutableStateOf(false) }

    var specialisations by remember { mutableStateOf<List<Specialisation>>(emptyList()) }
    LaunchedEffect(repository, lang) {
        if (repository != null) {
            specialisations = runCatching { repository.loadSpecialisations(lang) }.getOrDefault(emptyList())
        }
    }

    var role by remember(employee) { mutableStateOf(employee?.role ?: EmployeeRole.SPECIALIST) }
    // Prefilled with what the employee already has, so editing re-picks rather than starts empty.
    var specialisationIds by remember(employee) {
        mutableStateOf(employee?.specialisations.orEmpty().map { it.id }.toSet())
    }
    // Hiring takes a phone number: the account is created server-side if unknown.
    var phone by remember { mutableStateOf("") }

    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = if (isAdd) stringResource(Res.string.employee_add_title) else stringResource(Res.string.employee_edit_title)
    val effectivePhone = if (addSelf) currentUserPhone.orEmpty() else phone

    PageLayout(title, stringResource(Res.string.employee_edit_subtitle), onBack) {
        if (isAdd) {
            if (canAddSelf) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
                    Checkbox(checked = addSelf, onCheckedChange = { addSelf = it; error = null })
                    Text(stringResource(Res.string.employee_add_self), color = T.c.dark10, style = T.t.t3SemiBold)
                }
            }

            if (addSelf) {
                Text(stringResource(Res.string.employee_add_self_hint), color = T.c.dark5, style = T.t.t3)
            } else {
                Text(stringResource(Res.string.employee_hire_hint), color = T.c.dark5, style = T.t.t3)
                AppTextField(
                    value = phone,
                    onValueChange = { phone = it; error = null },
                    label = stringResource(Res.string.auth_phone_label),
                    placeholder = stringResource(Res.string.auth_phone_placeholder),
                    enabled = !submitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        } else {
            Text(employee!!.phone.orEmpty(), color = T.c.dark5, style = T.t.t3)
        }

        RoleSelector(role) { role = it }
        SpecialisationSelector(specialisations, specialisationIds) { id ->
            specialisationIds = if (id in specialisationIds) specialisationIds - id else specialisationIds + id
        }

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        SubmitButton(
            text = if (isAdd) stringResource(Res.string.employee_add_submit) else stringResource(Res.string.common_save),
            loading = submitting,
            enabled = repository != null && specialisationIds.isNotEmpty() &&
                (!isAdd || effectivePhone.count { it.isDigit() } >= MIN_PHONE_DIGITS),
        ) {
            submitting = true
            error = null
            scope.launch {
                val result = runCatching {
                    if (isAdd) {
                        repository!!.hireEmployee(
                            businessId = businessId,
                            phone = effectivePhone,
                            role = role,
                            specialisationIds = specialisationIds.toList(),
                            lang = lang,
                        )
                    } else {
                        // Role and specialisations are separate mutations server-side.
                        if (role != employee!!.role) {
                            repository!!.updateEmployeeRole(
                                businessId = businessId,
                                employeeId = employee.id,
                                role = role,
                                lang = lang,
                            )
                        }
                        if (specialisationIds != employee.specialisations.map { it.id }.toSet()) {
                            repository!!.setEmployeeSpecialisations(
                                businessId = businessId,
                                employeeId = employee.id,
                                specialisationIds = specialisationIds.toList(),
                                lang = lang,
                            )
                        }
                    }
                }
                result.onSuccess { onSaved() }
                    .onFailure { submitting = false; error = it.message }
            }
        }
        AppOutlinedButton(
            text = stringResource(Res.string.common_cancel),
            onClick = onBack,
            enabled = !submitting,
        )
    }
}

private const val MIN_PHONE_DIGITS = 9

@Composable
fun EmployeeServiceEditScreen(
    repository: BusinessWorkspaceRepository?,
    businessId: String,
    employeeId: String,
    service: BusinessService?,
    lang: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val isAdd = service == null

    var categories by remember { mutableStateOf<List<ServiceCategory>>(emptyList()) }
    LaunchedEffect(repository, lang) {
        if (repository != null) {
            categories = runCatching { repository.loadCategories(lang) }.getOrDefault(emptyList())
        }
    }

    // One field per language: the API stores the name as a language-keyed map.
    var names by remember(service) {
        mutableStateOf(SERVICE_NAME_LANGS.associateWith { code -> service?.nameByLang?.get(code).orEmpty() })
    }
    var categoryId by remember(service) { mutableStateOf(service?.categoryId) }
    var cost by remember(service) { mutableStateOf(service?.cost?.toString().orEmpty()) }
    var duration by remember(service) { mutableStateOf(service?.duration ?: DEFAULT_SERVICE_DURATION) }
    var isActive by remember(service) { mutableStateOf(service?.isActive ?: true) }

    var submitting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val title = if (isAdd) {
        stringResource(Res.string.employee_service_add_title)
    } else {
        stringResource(Res.string.employee_service_edit_title)
    }

    PageLayout(title, stringResource(Res.string.employee_service_edit_subtitle), onBack) {
        SERVICE_NAME_LANGS.forEach { code ->
            AppTextField(
                value = names[code].orEmpty(),
                onValueChange = { names = names + (code to it); error = null },
                label = stringResource(Res.string.employee_service_name_label, code.uppercase()),
                enabled = !submitting,
            )
        }

        CategorySelector(categories, categoryId) { categoryId = it }

        AppTextField(
            value = cost,
            onValueChange = { raw -> cost = raw.filter(Char::isDigit); error = null },
            label = stringResource(Res.string.service_price_title),
            enabled = !submitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        AppTextField(
            value = duration,
            onValueChange = { duration = it; error = null },
            label = stringResource(Res.string.service_duration_title),
            placeholder = DEFAULT_SERVICE_DURATION,
            enabled = !submitting,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
            Switch(checked = isActive, onCheckedChange = { isActive = it })
            Text(stringResource(Res.string.employee_active_label), color = T.c.dark10, style = T.t.t3SemiBold)
        }

        error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

        val filledNames = names.filterValues { it.isNotBlank() }
        SubmitButton(
            text = stringResource(Res.string.common_save),
            loading = submitting,
            enabled = repository != null && filledNames.isNotEmpty() && cost.toIntOrNull() != null &&
                duration.isNotBlank(),
        ) {
            submitting = true
            error = null
            scope.launch {
                val result = runCatching {
                    if (isAdd) {
                        repository!!.addEmployeeService(
                            businessId = businessId,
                            employeeId = employeeId,
                            name = filledNames,
                            cost = cost.toInt(),
                            duration = duration.trim(),
                            categoryId = categoryId,
                            isActive = isActive,
                            lang = lang,
                        )
                    } else {
                        repository!!.updateEmployeeService(
                            businessId = businessId,
                            serviceId = service!!.id,
                            name = filledNames,
                            cost = cost.toInt(),
                            duration = duration.trim(),
                            categoryId = categoryId,
                            isActive = isActive,
                            lang = lang,
                        )
                    }
                }
                result.onSuccess { onSaved() }
                    .onFailure { submitting = false; error = it.message }
            }
        }

        if (!isAdd) {
            AppOutlinedButton(
                text = stringResource(Res.string.employee_service_delete_action),
                onClick = { confirmDelete = true },
                enabled = !submitting && repository != null,
            )
        }

        AppOutlinedButton(
            text = stringResource(Res.string.common_cancel),
            onClick = onBack,
            enabled = !submitting,
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(Res.string.confirm_delete_service),
            confirmText = stringResource(Res.string.employee_service_delete_action),
            onConfirm = {
                confirmDelete = false
                val repo = repository ?: return@ConfirmDialog
                submitting = true
                error = null
                scope.launch {
                    runCatching { repo.deleteEmployeeService(businessId, service!!.id) }
                        .onSuccess { onSaved() }
                        .onFailure { submitting = false; error = it.message }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** The `Time` scalar the API expects for a service duration. */
private const val DEFAULT_SERVICE_DURATION = "01:00:00"

@Composable
private fun CategorySelector(
    options: List<ServiceCategory>,
    selectedId: String?,
    onSelected: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.id == selectedId }?.name
        ?: stringResource(Res.string.employee_service_category_select)

    Text(stringResource(Res.string.employee_service_category_label), color = T.c.dark5, style = T.t.t4SamiBold)
    Box(modifier = Modifier.fillMaxWidth()) {
        AppOutlinedButton(text = selectedName, onClick = { expanded = true })
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
private fun RoleSelector(selected: EmployeeRole, onSelected: (EmployeeRole) -> Unit) {
    Text(stringResource(Res.string.employee_role_label), color = T.c.dark5, style = T.t.t4SamiBold)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.d.md)) {
        EmployeeRole.entries.forEach { option ->
            if (option == selected) {
                AppButton(text = option.label(), onClick = { onSelected(option) }, modifier = Modifier.weight(1f))
            } else {
                AppOutlinedButton(text = option.label(), onClick = { onSelected(option) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** An employee can hold several specialisations, so each option is an independent checkbox. */
@Composable
private fun SpecialisationSelector(
    options: List<Specialisation>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Text(stringResource(Res.string.employee_specialisation_label), color = T.c.dark5, style = T.t.t4SamiBold)
    if (options.isEmpty()) {
        Text(stringResource(Res.string.employee_specialisation_empty), color = T.c.dark5, style = T.t.t3)
        return
    }
    Text(stringResource(Res.string.employee_specialisation_select), color = T.c.dark5, style = T.t.t3)
    Column(modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(option.id) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(T.d.sm),
            ) {
                Checkbox(checked = option.id in selectedIds, onCheckedChange = { onToggle(option.id) })
                Text(option.name, color = T.c.dark10, style = T.t.t3)
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
    AppButton(text = text, onClick = onClick, enabled = enabled, loading = loading)
}
