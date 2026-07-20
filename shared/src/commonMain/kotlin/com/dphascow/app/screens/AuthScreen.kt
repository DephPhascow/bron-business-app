package com.dphascow.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dphascow.app.auth.AppUiState
import com.dphascow.app.auth.AuthError
import com.dphascow.app.auth.AuthStage
import com.dphascow.app.auth.hasPhoto
import com.dphascow.app.expects.rememberPhotoPickerLauncher
import com.dphascow.app.resources.Res
import com.dphascow.app.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import ui.theme.T

private const val CODE_LENGTH = 4
private const val RESEND_SECONDS = 40

/** The server's limit window is 5 minutes, so that is how long we keep the button locked. */
private const val RATE_LIMIT_SECONDS = 5 * 60

@Composable
fun AuthScreen(
    state: AppUiState.Auth,
    onPhoneChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onGetCodeClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    when (state.stage) {
        AuthStage.PHONE -> PhoneStage(
            state = state,
            onPhoneChanged = onPhoneChanged,
            onGetCodeClick = onGetCodeClick,
        )

        AuthStage.CODE -> CodeStage(
            state = state,
            onCodeChanged = onCodeChanged,
            onVerifyClick = onVerifyClick,
            onResendClick = onResendClick,
            onBackClick = onBackClick,
        )
    }
}

@Composable
private fun PhoneStage(
    state: AppUiState.Auth,
    onPhoneChanged: (String) -> Unit,
    onGetCodeClick: () -> Unit,
) {
    // Hitting the limit here locks the button until the server's window has passed.
    var cooldownLeft by remember { mutableStateOf(0) }
    LaunchedEffect(state.error) {
        if (state.error == AuthError.TOO_MANY_REQUESTS) cooldownLeft = RATE_LIMIT_SECONDS
    }
    LaunchedEffect(cooldownLeft) {
        if (cooldownLeft > 0) {
            delay(1000)
            cooldownLeft--
        }
    }

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
                    value = state.phone,
                    onValueChange = onPhoneChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(Res.string.auth_phone_label)) },
                    placeholder = { Text(text = stringResource(Res.string.auth_phone_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
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
                    onClick = onGetCodeClick,
                    enabled = !state.isSubmitting && cooldownLeft == 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 2.dp),
                            strokeWidth = 2.dp,
                            color = T.c.onPrimary,
                        )
                    } else if (cooldownLeft > 0) {
                        Text(text = stringResource(Res.string.auth_retry_in, formatCooldown(cooldownLeft)))
                    } else {
                        Text(text = stringResource(Res.string.auth_get_code_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeStage(
    state: AppUiState.Auth,
    onCodeChanged: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onResendClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var secondsLeft by remember { mutableStateOf(RESEND_SECONDS) }
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    val rateLimited = state.error == AuthError.TOO_MANY_REQUESTS
    // Over the limit the server refuses both resending and verifying, so the whole
    // card waits out the window rather than just the resend link.
    LaunchedEffect(state.error) {
        if (rateLimited) secondsLeft = RATE_LIMIT_SECONDS
    }

    // Auto-submit once the full code is entered — but not while we are locked out,
    // or every keystroke would spend another attempt.
    LaunchedEffect(state.code, rateLimited) {
        if (state.code.length == CODE_LENGTH && !state.isSubmitting && !rateLimited) {
            onVerifyClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(T.d.paddingMain),
        verticalArrangement = Arrangement.spacedBy(T.d.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(T.d.sm)) {
            Text(
                text = stringResource(Res.string.auth_code_title),
                color = T.c.onBackground,
                style = T.t.headingH3,
            )
            Text(
                text = stringResource(Res.string.auth_code_subtitle, CODE_LENGTH, maskPhone(state.phone)),
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
                CodeInput(
                    code = state.code,
                    enabled = !state.isSubmitting && !rateLimited,
                    onCodeChanged = onCodeChanged,
                )

                state.error?.let { error ->
                    Text(
                        text = error.asText(),
                        color = T.c.redError,
                        style = T.t.t4SamiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.isSubmitting) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 2.dp),
                            strokeWidth = 2.dp,
                            color = T.c.primary,
                        )
                    }
                }

                if (secondsLeft > 0) {
                    Text(
                        text = if (rateLimited) {
                            stringResource(Res.string.auth_retry_in, formatCooldown(secondsLeft))
                        } else {
                            stringResource(Res.string.auth_resend_in, secondsLeft)
                        },
                        color = T.c.dark7,
                        style = T.t.t4,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    TextButton(
                        onClick = {
                            onResendClick()
                            secondsLeft = RESEND_SECONDS
                        },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(Res.string.auth_resend_code))
                    }
                }

                OutlinedButton(
                    onClick = onBackClick,
                    enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.common_back))
                }
            }
        }
    }
}

@Composable
private fun CodeInput(
    code: String,
    enabled: Boolean,
    onCodeChanged: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = code,
        onValueChange = { raw ->
            onCodeChanged(raw.filter(Char::isDigit).take(CODE_LENGTH))
        },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth(),
        decorationBox = { innerTextField ->
            // The real field is invisible; it only captures focus and the keyboard.
            Box(Modifier.size(0.dp).alpha(0f)) { innerTextField() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled) { focusRequester.requestFocus() },
                horizontalArrangement = Arrangement.spacedBy(T.d.sm),
            ) {
                val shape = RoundedCornerShape(T.d.md)
                for (i in 0 until CODE_LENGTH) {
                    val char = code.getOrNull(i)?.toString().orEmpty()
                    val isFocused = i == code.length && code.length < CODE_LENGTH
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(shape)
                            .background(T.c.background)
                            .border(
                                width = 1.dp,
                                color = if (isFocused) T.c.primary else T.c.dark3,
                                shape = shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = char.ifEmpty { " " },
                            color = T.c.onBackground,
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            ),
                        )
                    }
                }
            }
        },
    )
}

/** "4:59" — a five-minute lockout reads better as mm:ss than as raw seconds. */
private fun formatCooldown(seconds: Int): String {
    val minutes = seconds / 60
    val rest = seconds % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}

/** Masks a phone number for display, e.g. "+998 ** ***-**-**". */
fun maskPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length < 6) return phone
    val prefix = digits.take(3)
    return "+$prefix ** ***-**-**"
}

@Composable
private fun AuthError.asText(): String = when (this) {
    AuthError.EMPTY_PHONE -> stringResource(Res.string.auth_phone_empty_error)
    AuthError.INVALID_PHONE -> stringResource(Res.string.auth_phone_invalid_error)
    AuthError.CODE_SEND_FAILED -> stringResource(Res.string.auth_code_send_failed_error)
    AuthError.EMPTY_CODE -> stringResource(Res.string.auth_code_empty_error)
    AuthError.INVALID_CODE -> stringResource(Res.string.auth_code_invalid_error)
    AuthError.TOO_MANY_REQUESTS -> stringResource(Res.string.auth_too_many_requests_error)
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
