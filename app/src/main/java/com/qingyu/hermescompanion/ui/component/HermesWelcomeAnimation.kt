package com.qingyu.hermescompanion.ui.component

import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.qingyu.hermescompanion.R

/**
 * Lightweight welcome/idle mascot used only on empty and guide surfaces.
 * Animated WebP is available through ImageDecoder on Android 9+; older
 * devices receive the matching transparent first frame.
 */
@Composable
fun HermesWelcomeAnimation(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        AnimatedWelcomeImage(modifier, contentDescription)
    } else {
        Image(
            painter = painterResource(R.drawable.hermes_welcome_fallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun AnimatedWelcomeImage(
    modifier: Modifier,
    contentDescription: String?,
) {
    val context = LocalContext.current
    val drawable = remember(context) {
        runCatching {
            ImageDecoder.decodeDrawable(
                ImageDecoder.createSource(context.resources, R.drawable.hermes_welcome_animation),
            ).also { decoded ->
                if (decoded is AnimatedImageDrawable) {
                    decoded.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                }
            }
        }.getOrNull()
    }

    if (drawable == null) {
        Image(
            painter = painterResource(R.drawable.hermes_welcome_fallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
        return
    }

    DisposableEffect(drawable) {
        (drawable as? Animatable)?.start()
        onDispose { (drawable as? Animatable)?.stop() }
    }
    AndroidView(
        modifier = modifier,
        factory = { imageContext ->
            ImageView(imageContext).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                setImageDrawable(drawable)
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            if (imageView.drawable !== drawable) imageView.setImageDrawable(drawable)
            imageView.contentDescription = contentDescription
            (drawable as? Animatable)?.start()
        },
    )
}
