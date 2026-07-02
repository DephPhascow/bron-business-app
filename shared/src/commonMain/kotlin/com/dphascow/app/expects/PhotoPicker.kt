package com.dphascow.app.expects

import androidx.compose.runtime.Composable

data class PickedPhoto(
    val bytes: ByteArray,
    val fileName: String? = null,
    val mimeType: String? = null,
)

class PhotoPickerLauncher(
    val isAvailable: Boolean,
    private val onLaunch: () -> Unit,
) {
    fun launch() = onLaunch()
}

@Composable
expect fun rememberPhotoPickerLauncher(
    onPhotoPicked: (PickedPhoto?) -> Unit,
): PhotoPickerLauncher

