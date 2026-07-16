package com.dphascow.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.dphascow.BuildKonfig
import com.dphascow.app.utils.resolveFullUrl

/** Loads and displays a remote image (avatars, logo, gallery, chat media). */
@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = resolveFullUrl(BuildKonfig.API_HOST, url),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
