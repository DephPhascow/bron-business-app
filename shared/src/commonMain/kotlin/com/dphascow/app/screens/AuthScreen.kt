package com.dphascow.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.auth.AuthError
import com.dphascow.app.auth.hasPhoto
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import com.dphascow.app.resources.auth_email_empty_error
import com.dphascow.app.resources.auth_email_label
import com.dphascow.app.resources.auth_email_invalid_error
import com.dphascow.app.resources.auth_invalid_credentials_error
import com.dphascow.app.resources.auth_login_action
import com.dphascow.app.resources.auth_password_empty_error
import com.dphascow.app.resources.auth_password_label
import com.dphascow.app.resources.auth_subtitle
import com.dphascow.app.resources.auth_title
import com.dphascow.app.resources.auth_unknown_error
import com.dphascow.app.resources.business_remember_choice
import com.dphascow.app.resources.business_remember_choice_hint
import com.dphascow.app.resources.business_create_action
import com.dphascow.app.resources.business_create_name_label
import com.dphascow.app.resources.business_create_photo_action
import com.dphascow.app.resources.business_create_photo_added
import com.dphascow.app.resources.business_create_photo_empty
import com.dphascow.app.resources.business_create_submit_action
import com.dphascow.app.resources.business_create_subtitle
import com.dphascow.app.resources.business_create_title
import com.dphascow.app.resources.business_select_action
import com.dphascow.app.resources.business_selection_subtitle
import com.dphascow.app.resources.business_selection_title
import com.dphascow.app.resources.common_cancel
import com.dphascow.app.resources.home_logout
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

@Composable
fun AuthScreen(
    state: AppUiState.Auth,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegistrationClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(T.d.sm)) {
            Text(
                text = stringResource(Res.string.auth_title),
                color = T.c.onBackground,
                style = T.t.headingH3,
            )
            Text(
                text = stringResource(Res.string.auth_subtitle),
                color = T.c.dark7,
                style = T.t.t2Regular,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = T.c.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(T.d.lg),
                verticalArrangement = Arrangement.spacedBy(T.d.md),
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.auth_email_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.auth_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )

                state.error?.let { error ->
                    Text(
                        text = error.asText(),
                        color = T.c.redError,
                        style = T.t.t4SamiBold,
                    )
                }

                Button(
                    onClick = onLoginClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 2.dp),
                            strokeWidth = 2.dp,
                            color = T.c.onPrimary,
                        )
                    } else {
                        Text(text = stringResource(Res.string.auth_login_action))
                    }
                }

                OutlinedButton(
                    onClick = onRegistrationClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.registration_action))
                }
            }
        }

    }
}

@Composable
fun RegistrationScreen(
    state: AppUiState.Registration,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(T.d.sm)) {
            Text(
                text = stringResource(Res.string.registration_title),
                color = T.c.onBackground,
                style = T.t.headingH3,
            )
            Text(
                text = stringResource(Res.string.registration_subtitle),
                color = T.c.dark7,
                style = T.t.t2Regular,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = T.c.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(T.d.lg),
                verticalArrangement = Arrangement.spacedBy(T.d.md),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.registration_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.auth_email_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.auth_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )

                state.error?.let { error ->
                    Text(
                        text = error.asText(),
                        color = T.c.redError,
                        style = T.t.t4SamiBold,
                    )
                }

                Button(
                    onClick = onRegisterClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 2.dp),
                            strokeWidth = 2.dp,
                            color = T.c.onPrimary,
                        )
                    } else {
                        Text(text = stringResource(Res.string.registration_submit_action))
                    }
                }

                OutlinedButton(
                    onClick = onBackClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.common_cancel))
                }
            }
        }
    }
}

@Composable
private fun AuthError.asText(): String = when (this) {
    AuthError.EMPTY_EMAIL -> stringResource(Res.string.auth_email_empty_error)
    AuthError.INVALID_EMAIL -> stringResource(Res.string.auth_email_invalid_error)
    AuthError.EMPTY_PASSWORD -> stringResource(Res.string.auth_password_empty_error)
    AuthError.INVALID_CREDENTIALS -> stringResource(Res.string.auth_invalid_credentials_error)
    AuthError.UNKNOWN -> stringResource(Res.string.auth_unknown_error)
}

@Composable
fun BusinessSelectionScreen(
    state: AppUiState.BusinessSelection,
    onRememberChoiceChanged: (Boolean) -> Unit,
    onBusinessClick: (String) -> Unit,
    onCreateBusinessClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        Text(
            text = stringResource(Res.string.business_selection_title),
            color = T.c.onBackground,
            style = T.t.headingH3,
        )
        Text(
            text = stringResource(Res.string.business_selection_subtitle),
            color = T.c.dark7,
            style = T.t.t2Regular,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(T.d.sm),
        ) {
            Checkbox(
                checked = state.rememberChoice,
                onCheckedChange = { checked -> onRememberChoiceChanged(checked) },
            )
            Column(verticalArrangement = Arrangement.spacedBy(T.d.xs)) {
                Text(
                    text = stringResource(Res.string.business_remember_choice),
                    color = T.c.onSurface,
                    style = T.t.t3SemiBold,
                )
                Text(
                    text = stringResource(Res.string.business_remember_choice_hint),
                    color = T.c.dark7,
                    style = T.t.t4,
                )
            }
        }

        if (state.businesses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = T.c.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(T.d.lg),
                    verticalArrangement = Arrangement.spacedBy(T.d.sm),
                ) {
                    Text(
                        text = stringResource(Res.string.business_empty_title),
                        color = T.c.onSurface,
                        style = T.t.t2Bold,
                    )
                    Text(
                        text = stringResource(Res.string.business_empty_subtitle),
                        color = T.c.dark7,
                        style = T.t.t2Regular,
                    )
                }
            }
        }

        state.businesses.forEach { business ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = T.c.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(T.d.lg),
                    verticalArrangement = Arrangement.spacedBy(T.d.md),
                ) {
                    Text(
                        text = business.name,
                        color = T.c.onSurface,
                        style = T.t.t2Bold,
                    )
                    Text(
                        text = business.role,
                        color = T.c.dark7,
                        style = T.t.t2Regular,
                    )
                    Button(
                        onClick = { onBusinessClick(business.id) },
                        enabled = !state.isSelecting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(Res.string.business_select_action))
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onCreateBusinessClick,
            enabled = !state.isSelecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.business_create_action))
        }

        Button(
            onClick = onLogoutClick,
            enabled = !state.isSelecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(Res.string.home_logout))
        }
    }
}

@Composable
fun BusinessCreationScreen(
    state: AppUiState.BusinessCreation,
    onNameChanged: (String) -> Unit,
    onPhotoPicked: (com.dphascow.app.expects.PickedPhoto?) -> Unit,
    onCreateClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val photoPicker = rememberPhotoPickerLauncher(onPhotoPicked = onPhotoPicked)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        Text(
            text = stringResource(Res.string.business_create_title),
            color = T.c.onBackground,
            style = T.t.headingH3,
        )
        Text(
            text = stringResource(Res.string.business_create_subtitle),
            color = T.c.dark7,
            style = T.t.t2Regular,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = T.c.surface),
            shape = RoundedCornerShape(T.d.lg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(T.d.lg),
                verticalArrangement = Arrangement.spacedBy(T.d.md),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(T.d.lg))
                        .background(if (state.draft.hasPhoto) T.c.graniteGreen7 else T.c.dark3),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(T.d.sm),
                    ) {
                        Icon(
                            imageVector = if (state.draft.hasPhoto) Icons.Outlined.Apartment else Icons.Outlined.AddAPhoto,
                            contentDescription = null,
                            tint = T.c.dark1,
                        )
                        Text(
                            text = state.draft.photo?.fileName ?: stringResource(
                                if (state.draft.hasPhoto) {
                                    Res.string.business_create_photo_added
                                } else {
                                    Res.string.business_create_photo_empty
                                }
                            ),
                            color = T.c.dark1,
                            style = T.t.t3SemiBold,
                        )
                    }
                }

                OutlinedButton(
                    onClick = photoPicker::launch,
                    enabled = !state.isSubmitting && photoPicker.isAvailable,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.business_create_photo_action))
                }

                OutlinedTextField(
                    value = state.draft.name,
                    onValueChange = onNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.business_create_name_label)) },
                    singleLine = true,
                )

                Button(
                    onClick = onCreateClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 2.dp),
                            strokeWidth = 2.dp,
                            color = T.c.onPrimary,
                        )
                    } else {
                        Text(text = stringResource(Res.string.business_create_submit_action))
                    }
                }

                OutlinedButton(
                    onClick = onCancelClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.common_cancel))
                }
            }
        }
    }
}










