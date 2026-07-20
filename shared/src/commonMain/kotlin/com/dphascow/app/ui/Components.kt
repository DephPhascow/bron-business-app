package com.dphascow.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.theme.T

/**
 * Shared look of the client app (`bron/app`): flat content on `dark1`, accent panels
 * in `graniteGreen7` with `dark1` content, 15 dp fields and 52 dp buttons.
 */

/** Corner radius the client app uses for inputs and small surfaces. */
private val FieldShape = RoundedCornerShape(15.dp)

/** Accent panels (the "coming soon" card, the salon action row) are rounder. */
private val PanelShape = RoundedCornerShape(20.dp)

/** Primary buttons in the client app are a fixed 52 dp tall. */
private val ButtonHeight = 52.dp

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
        shape = MaterialTheme.shapes.large,
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = T.c.onPrimary, modifier = Modifier.height(20.dp))
        } else {
            Text(text, style = T.t.b14)
        }
    }
}

@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(ButtonHeight),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = T.c.dark10),
    ) {
        if (loading) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = T.c.primary, modifier = Modifier.height(20.dp))
        } else {
            Text(text, style = T.t.b14)
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: ImageVector? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        label = { Text(label, color = T.c.dark10, style = T.t.b14) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = T.c.dark5) } },
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedLabelColor = T.c.dark10,
            unfocusedLabelColor = T.c.dark10,
            focusedBorderColor = T.c.borderColor,
            unfocusedLeadingIconColor = T.c.borderColor,
            focusedPlaceholderColor = T.c.dark5,
            unfocusedPlaceholderColor = T.c.dark5,
        ),
    )
}

/**
 * The client app's accent block — a filled `graniteGreen7` panel whose content is
 * drawn in `dark1`. Reserved for the one call to action on a screen; a page full of
 * them would flatten the hierarchy it exists to create.
 */
@Composable
fun AccentPanel(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(T.c.graniteGreen7, PanelShape)
            .padding(T.d.xl),
        verticalArrangement = Arrangement.spacedBy(T.d.sm),
    ) {
        Text(title, style = T.t.t1, color = T.c.dark1)
        if (subtitle != null) {
            Text(subtitle, style = T.t.t4, color = T.c.dark1)
        }
        if (actionText != null && onClick != null) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().height(ButtonHeight),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = T.c.dark1,
                    contentColor = T.c.dark10,
                ),
            ) {
                Text(actionText, style = T.t.b14)
            }
        }
    }
}

/** A plain content block on the page surface — the client app does not use elevated cards. */
@Composable
fun AppSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(T.d.sm)) {
        Text(title, style = T.t.t14, color = T.c.dark10)
        content()
    }
}

/** Title on the left, an optional text affordance on the right — the client app's list header. */
@Composable
fun AppRowItem(
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = T.d.md),
        horizontalArrangement = Arrangement.spacedBy(T.d.md),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(T.d.xs),
        ) {
            Text(title, style = T.t.t2, color = T.c.dark10)
            if (subtitle != null) {
                Text(subtitle, style = T.t.t4, color = T.c.dark5)
            }
        }
        if (actionText != null) {
            Text(actionText, style = T.t.t4, color = T.c.dark10)
        }
    }
}
