package com.dphascow.app.expects

import androidx.compose.runtime.Composable

@Composable
expect fun BackPressHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)

