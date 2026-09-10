package com.qingyu.hermescompanion.ui.component

import com.qingyu.hermescompanion.i18n.uiText
import com.qingyu.hermescompanion.R


import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qingyu.hermescompanion.model.ImagePreview

@Composable
fun PreviewableImage(
    source: String,
    name: String,
    onOpen: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    preview: ImagePreview? = null,
) {
    val bitmap = remember(source, preview) {
        val bytes = preview?.bytes ?: source.takeIf { it.startsWith("data:image/", ignoreCase = true) }
            ?.substringAfter(',', "")
            ?.takeIf(String::isNotBlank)
            ?.let { encoded -> runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull() }
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable { onOpen(source, name) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = uiText(R.string.ui_0447, "预览 %1\$s", name),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HermesMulticolorIcon(HermesIconKind.PHOTO, contentDescription = null, iconSize = 28.dp)
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(uiText(R.string.ui_0448, "点击打开图片"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ImagePreviewDialog(image: ImagePreview, onDismiss: () -> Unit) {
    val bitmap = remember(image.source, image.bytes.size) {
        BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)?.asImageBitmap()
    }
    var scale by remember(image.source) { mutableFloatStateOf(1f) }
    var offsetX by remember(image.source) { mutableFloatStateOf(0f) }
    var offsetY by remember(image.source) { mutableFloatStateOf(0f) }
    val transformable = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale == 1f) {
            offsetX = 0f
            offsetY = 0f
        } else {
            offsetX += panChange.x
            offsetY += panChange.y
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize().background(Color(0xF20B1020))) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = image.name,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 76.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY,
                        )
                        .transformable(transformable),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(uiText(R.string.ui_0449, "无法显示这张图片"), color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(start = 14.dp, end = 8.dp, top = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(image.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(image.mimeType, color = Color.White.copy(alpha = 0.62f), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDismiss) {
                    HermesMulticolorIcon(HermesIconKind.CLOSE, contentDescription = uiText(R.string.ui_0450, "关闭预览"), tint = Color.White)
                }
            }
            if (scale > 1f) {
                Text(
                    "${(scale * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp)
                        .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
fun ImageLoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.2.dp)
                    Text(uiText(R.string.ui_0451, "正在打开图片"))
                }
            }
        }
    }
}
