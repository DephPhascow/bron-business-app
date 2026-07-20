package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dphascow.app.expects.PickedPhoto
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.profile.MeProfile
import com.dphascow.app.profile.ProfileRepository
import com.dphascow.app.ui.AppButton
import com.dphascow.app.ui.AppOutlinedButton
import com.dphascow.app.ui.AppTextField
import com.dphascow.app.ui.NetworkImage
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import settings.ThemeMode
import ui.theme.T

private val LANGUAGES = listOf("ru" to "Русский", "uz" to "O‘zbek", "en" to "English")

/**
 * Navigation drawer: links to the Account and Settings pages, plus logout pinned to the
 * bottom. It only navigates — the pages themselves hold their content.
 */
@Composable
fun AccountDrawer(
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: (allDevices: Boolean) -> Unit,
) {
    // Non-null while a logout is pending confirmation; true = every device.
    var confirmLogoutAllDevices by remember { mutableStateOf<Boolean?>(null) }

    ModalDrawerSheet {
        Column(
            modifier = Modifier.fillMaxHeight().padding(T.d.md),
            verticalArrangement = Arrangement.spacedBy(T.d.xs),
        ) {
            Text(
                stringResource(Res.string.account_menu_title),
                color = T.c.dark10,
                style = T.t.headingH3,
                modifier = Modifier.padding(T.d.sm),
            )
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.account_title)) },
                icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                selected = false,
                onClick = onOpenAccount,
            )
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.settings_title)) },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = false,
                onClick = onOpenSettings,
            )

            Spacer(modifier = Modifier.weight(1f))

            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.home_logout), color = T.c.redError) },
                icon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = T.c.redError,
                    )
                },
                selected = false,
                onClick = { confirmLogoutAllDevices = false },
            )
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.logout_all_devices), color = T.c.redError) },
                icon = {
                    Icon(
                        Icons.Outlined.DevicesOther,
                        contentDescription = null,
                        tint = T.c.redError,
                    )
                },
                selected = false,
                onClick = { confirmLogoutAllDevices = true },
            )
        }
    }

    confirmLogoutAllDevices?.let { allDevices ->
        ConfirmDialog(
            title = if (allDevices) {
                stringResource(Res.string.confirm_logout_all_devices)
            } else {
                stringResource(Res.string.confirm_logout)
            },
            confirmText = if (allDevices) {
                stringResource(Res.string.logout_all_devices)
            } else {
                stringResource(Res.string.home_logout)
            },
            onConfirm = { confirmLogoutAllDevices = null; onLogout(allDevices) },
            onDismiss = { confirmLogoutAllDevices = null },
        )
    }
}

/** Account page: the signed-in user's profile. */
@Composable
fun AccountScreen(
    profileRepository: ProfileRepository?,
    onBack: () -> Unit,
) {
    PageLayout(stringResource(Res.string.account_title), stringResource(Res.string.account_subtitle), onBack) {
        val scope = rememberCoroutineScope()
        var profile by remember { mutableStateOf<MeProfile?>(null) }
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var patronymic by remember { mutableStateOf("") }
        var notifications by remember { mutableStateOf(true) }
        var avatar by remember { mutableStateOf<PickedPhoto?>(null) }
        var loading by remember { mutableStateOf(true) }
        var saving by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val avatarPicker = rememberPhotoPickerLauncher(onPhotoPicked = { avatar = it })

        LaunchedEffect(profileRepository) {
            if (profileRepository == null) {
                loading = false
                return@LaunchedEffect
            }
            loading = true
            runCatching { profileRepository.loadMe() }
                .onSuccess { me ->
                    profile = me
                    firstName = me.firstName.orEmpty()
                    lastName = me.lastName.orEmpty()
                    patronymic = me.patronymic.orEmpty()
                    notifications = me.confirmNotifications
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }

        if (loading) {
            CircularProgressIndicator(color = T.c.primary)
        } else {
            profile?.imageUrl?.let { url ->
                NetworkImage(
                    url = url,
                    modifier = Modifier.size(96.dp).clip(CircleShape).align(Alignment.CenterHorizontally),
                )
            }
            AppTextField(firstName, { firstName = it }, stringResource(Res.string.account_first_name), enabled = !saving)
            AppTextField(lastName, { lastName = it }, stringResource(Res.string.account_last_name), enabled = !saving)
            AppTextField(patronymic, { patronymic = it }, stringResource(Res.string.account_patronymic), enabled = !saving)

            profile?.phone?.let {
                AppTextField(
                    value = it,
                    onValueChange = {},
                    label = stringResource(Res.string.business_settings_phone_label),
                    enabled = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }

            AppOutlinedButton(
                text = avatar?.fileName ?: stringResource(Res.string.account_avatar_action),
                onClick = avatarPicker::launch,
                enabled = avatarPicker.isAvailable && !saving,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
                Switch(checked = notifications, onCheckedChange = { notifications = it })
                Text(stringResource(Res.string.account_notifications), color = T.c.dark10, style = T.t.t3SemiBold)
            }

            error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

            AppButton(
                text = stringResource(Res.string.common_save),
                loading = saving,
                enabled = profileRepository != null,
                onClick = {
                    if (profileRepository == null) return@AppButton
                    saving = true
                    error = null
                    scope.launch {
                        runCatching {
                            profileRepository.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                patronymic = patronymic,
                                confirmNotifications = notifications,
                                avatar = avatar,
                            )
                        }.onSuccess {
                            profile = it
                            avatar = null
                            saving = false
                        }.onFailure { saving = false; error = it.message }
                    }
                },
            )
        }
    }
}

/** Settings page: language and theme. Logging out lives at the bottom of the drawer. */
@Composable
fun SettingsScreen(
    lang: String,
    theme: ThemeMode,
    onLangChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
    PageLayout(stringResource(Res.string.settings_title), stringResource(Res.string.settings_subtitle), onBack) {
        Text(stringResource(Res.string.account_language), color = T.c.dark5, style = T.t.t4SamiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
            LANGUAGES.forEach { (code, title) ->
                if (code == lang) {
                    AppButton(text = title, onClick = { onLangChange(code) }, modifier = Modifier.weight(1f))
                } else {
                    AppOutlinedButton(text = title, onClick = { onLangChange(code) }, modifier = Modifier.weight(1f))
                }
            }
        }

        Text(stringResource(Res.string.account_theme), color = T.c.dark5, style = T.t.t4SamiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
            ThemeMode.entries.forEach { mode ->
                if (mode == theme) {
                    AppButton(text = mode.label(), onClick = { onThemeChange(mode) }, modifier = Modifier.weight(1f))
                } else {
                    AppOutlinedButton(text = mode.label(), onClick = { onThemeChange(mode) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
    ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
    ThemeMode.DARK -> stringResource(Res.string.theme_dark)
}
