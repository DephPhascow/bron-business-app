package com.dphascow.app.expects

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (PickedPhoto?) -> Unit,
): PhotoPickerLauncher = PhotoPickerLauncher(isAvailable = false) { }

