package com.qingyu.hermescompanion.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.storage.resolvePrivateAvatarFile
import java.io.File

/** Displays only copied private avatar files; expired external content URIs are ignored. */
@Composable
fun UserAvatar(
    uri: String,
    displayName: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
    hermesFallback: Boolean = false,
    shape: Shape = RoundedCornerShape(size / 3.2f),
) {
    val context = LocalContext.current
    val framedModifier = modifier.size(size).clip(shape)
        .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    val privateBitmap = remember(uri) {
        resolvePrivateAvatarFile(uri, File(context.filesDir, "avatars"))
            ?.let { file -> runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull() }
    }

    if (privateBitmap != null) {
        Image(
            bitmap = privateBitmap,
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = framedModifier,
        )
    } else if (hermesFallback) {
        Box(modifier = framedModifier, contentAlignment = Alignment.Center) {
            HermesMark(compact = true, requestedSize = size)
        }
    } else {
        Image(
            painter = painterResource(R.drawable.fixed_user_avatar),
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = framedModifier,
        )
    }
}

/** Renders the same stored user image without forcing avatar dimensions or a circular crop. */
@Composable
fun UserPhoto(
    uri: String,
    displayName: String,
    modifier: Modifier = Modifier,
    shape: Shape,
) {
    val context = LocalContext.current
    val privateBitmap = remember(uri) {
        resolvePrivateAvatarFile(uri, File(context.filesDir, "avatars"))
            ?.let { file -> runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull() }
    }
    val photoModifier = modifier.clip(shape)
    if (privateBitmap != null) {
        Image(
            bitmap = privateBitmap,
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = photoModifier,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.fixed_user_avatar),
            contentDescription = displayName,
            contentScale = ContentScale.Crop,
            modifier = photoModifier,
        )
    }
}
