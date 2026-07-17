package com.dphascow.app.screens.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.dphascow.app.ui.NetworkImage
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import settings.ThemeMode
import ui.theme.T

private val LANGUAGES = listOf("ru" to "Русский", "uz" to "O‘zbek", "en" to "English")

/**
 * Navigation drawer: links to the Account and Settings pages. It only navigates —
 * the pages themselves hold their content.
 */
@Composable
fun AccountDrawer(
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(T.d.md),
            verticalArrangement = Arrangement.spacedBy(T.d.xs),
        ) {
            Text(
                stringResource(Res.string.account_menu_title),
                color = T.c.onBackground,
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
        }
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
            OutlinedTextField(firstName, { firstName = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.account_first_name)) }, singleLine = true)
            OutlinedTextField(lastName, { lastName = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.account_last_name)) }, singleLine = true)
            OutlinedTextField(patronymic, { patronymic = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(Res.string.account_patronymic)) }, singleLine = true)

            profile?.phone?.let {
                OutlinedTextField(it, {}, Modifier.fillMaxWidth(), enabled = false, label = { Text(stringResource(Res.string.business_settings_phone_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            }

            OutlinedButton(onClick = avatarPicker::launch, enabled = avatarPicker.isAvailable && !saving, modifier = Modifier.fillMaxWidth()) {
                Text(avatar?.fileName ?: stringResource(Res.string.account_avatar_action))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
                Switch(checked = notifications, onCheckedChange = { notifications = it })
                Text(stringResource(Res.string.account_notifications), color = T.c.onSurface, style = T.t.t3SemiBold)
            }

            error?.let { Text(it, color = T.c.redError, style = T.t.t4SamiBold) }

            Button(
                onClick = {
                    if (profileRepository == null) return@Button
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
                enabled = !saving && profileRepository != null,
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
}

/** Settings page: language, theme and logout. */
@Composable
fun SettingsScreen(
    lang: String,
    theme: ThemeMode,
    onLangChange: (String) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    PageLayout(stringResource(Res.string.settings_title), stringResource(Res.string.settings_subtitle), onBack) {
        Text(stringResource(Res.string.account_language), color = T.c.dark7, style = T.t.t4SamiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
            LANGUAGES.forEach { (code, title) ->
                if (code == lang) {
                    Button(onClick = { onLangChange(code) }, modifier = Modifier.weight(1f)) { Text(title) }
                } else {
                    OutlinedButton(onClick = { onLangChange(code) }, modifier = Modifier.weight(1f)) { Text(title) }
                }
            }
        }

        Text(stringResource(Res.string.account_theme), color = T.c.dark7, style = T.t.t4SamiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.d.sm)) {
            ThemeMode.entries.forEach { mode ->
                if (mode == theme) {
                    Button(onClick = { onThemeChange(mode) }, modifier = Modifier.weight(1f)) { Text(mode.label()) }
                } else {
                    OutlinedButton(onClick = { onThemeChange(mode) }, modifier = Modifier.weight(1f)) { Text(mode.label()) }
                }
            }
        }

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.home_logout))
        }
    }
}

@Composable
private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(Res.string.theme_system)
    ThemeMode.LIGHT -> stringResource(Res.string.theme_light)
    ThemeMode.DARK -> stringResource(Res.string.theme_dark)
}
