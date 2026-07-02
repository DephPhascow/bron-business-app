package com.dphascow.app.expects

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPhotoPickerLauncher(
    onPhotoPicked: (PickedPhoto?) -> Unit,
): PhotoPickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            onPhotoPicked(null)
            return@rememberLauncherForActivityResult
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: run {
            onPhotoPicked(null)
            return@rememberLauncherForActivityResult
        }

        val fileName = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        }

        onPhotoPicked(
            PickedPhoto(
                bytes = bytes,
                fileName = fileName,
                mimeType = context.contentResolver.getType(uri),
            )
        )
    }

    return PhotoPickerLauncher(isAvailable = true) {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
}

