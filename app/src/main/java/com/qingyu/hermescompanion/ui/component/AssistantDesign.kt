package com.qingyu.hermescompanion.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.*
import com.qingyu.hermescompanion.R
import com.qingyu.hermescompanion.ui.AppRoute

val AssistantBlue: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < .5f) Color(0xFF69D4FF) else Color(0xFF0078AD)
val NavigationSelected: Color
    @Composable get() = if(MaterialTheme.colorScheme.background.luminance()<.5f) Color(0xFFB1C5FF) else Color(0xFF586CCA)
val NavigationIdle: Color
    @Composable get() = if(MaterialTheme.colorScheme.background.luminance()<.5f) Color(0xFF9CA7B5) else Color(0xFF6C7585)

val NavigationGradientStart: Color
    @Composable get() = if(MaterialTheme.colorScheme.background.luminance()<.5f) Color(0xFF8EDFFF) else Color(0xFF168DD0)
val NavigationGradientEnd: Color
    @Composable get() = if(MaterialTheme.colorScheme.background.luminance()<.5f) Color(0xFFC6ACFF) else Color(0xFF8860DA)

@Composable
fun FixedRegionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier.fillMaxWidth().testTag("fixed_region_divider"), thickness = 1.dp,
        color = if (MaterialTheme.colorScheme.background.luminance()<.5f) Color(0xFF344452) else Color(0xFFDCE6F0))
}

val AssistantAccent = Color(0xFF009BDE)
val AssistantMint = Color(0xFF06B9A9)
val AssistantPurple = Color(0xFF8260E7)

@Composable
fun AssistantPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)),
        shadowElevation = 0.dp, content = content)
}

/** Draw the original artwork viewport: no re-generated face, pose or avatar substitution.
 * Coordinates are in the supplied 853 x 1844 design reference; source bytes are unchanged. */
@Composable
fun ReferenceHermesGirl(modifier: Modifier = Modifier) {
    val artwork = ImageBitmap.imageResource(R.drawable.hermes_home_reference)
    val backdrop = MaterialTheme.colorScheme.background
    // Shift the reference backdrop to the theme without changing its source or pose.
    val palette = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        1f,0f,0f,0f,backdrop.red * 255f - 245f,
        0f,1f,0f,0f,backdrop.green * 255f - 246f,
        0f,0f,1f,0f,backdrop.blue * 255f - 250f,
        0f,0f,0f,1f,0f)))
    Canvas(modifier) {
        drawImage(artwork, srcOffset = IntOffset(493, 176), srcSize = IntSize(312, 267),
            dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.High,
            colorFilter = if (backdrop.luminance() > .5f) palette else null)
        // The reference's search button overlaps the top-right six pixels of the
        // viewport. Cover only that UI fragment; it does not intersect the artwork.
        drawRect(if (backdrop.luminance() > .5f) backdrop else Color(0xFFF5F6FA), topLeft = Offset(size.width * 242f / 312f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width * 70f / 312f, size.height * 6f / 267f))
    }
}

@Composable
fun AssistantGlyph(kind: String, modifier: Modifier = Modifier.size(24.dp), tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, filled: Boolean = false, fillBrush: Brush? = null) {
    val cutout = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val ink = fillBrush ?: SolidColor(tint)
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(1.65f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(tint, Offset(x,y), Offset(x2,y2), 1.65f, StrokeCap.Round)
            when (kind) {
                "home" -> { val p = Path().apply { moveTo(3f,11f); lineTo(12f,3f); lineTo(21f,11f); lineTo(21f,21f); lineTo(15f,21f); lineTo(15f,15f); lineTo(9f,15f); lineTo(9f,21f); lineTo(3f,21f); close() }; drawPath(p,ink,style=if(filled) androidx.compose.ui.graphics.drawscope.Fill else stroke) }
                "history" -> { if(filled) { drawCircle(ink,9f,Offset(12f,12f)); drawLine(cutout,Offset(12f,6f),Offset(12f,12f),1.8f,StrokeCap.Round); drawLine(cutout,Offset(12f,12f),Offset(17f,12f),1.8f,StrokeCap.Round) } else { drawCircle(tint,9f,Offset(12f,12f),style=stroke); line(12f,6f,12f,12f);line(12f,12f,17f,12f) } }
                "user" -> { drawCircle(ink,4.1f,Offset(12f,6.5f),style=if(filled) androidx.compose.ui.graphics.drawscope.Fill else stroke); val p=Path().apply{moveTo(3f,22f);cubicTo(3f,10f,21f,10f,21f,22f);close()};drawPath(p,ink,style=if(filled) androidx.compose.ui.graphics.drawscope.Fill else stroke) }
                "plus" -> {line(12f,4f,12f,20f);line(4f,12f,20f,12f)}
                "wave" -> listOf(8f,16f,22f,14f,5f).forEachIndexed { i,h -> line(3f+i*4.5f,12f-h/2,3f+i*4.5f,12f+h/2) }
                "search" -> { drawCircle(tint,7f,Offset(10f,10f),style=stroke);line(15f,15f,21f,21f) }
                "arrow" -> {line(5f,12f,20f,12f);line(14f,6f,20f,12f);line(20f,12f,14f,18f)}
                "chevron" -> {line(9f,6f,15f,12f);line(15f,12f,9f,18f)}
                "more" -> listOf(5f,12f,19f).forEach {drawCircle(tint,1.8f,Offset(it,12f))}
                "check" -> {line(5f,12f,10f,17f);line(10f,17f,19f,7f)}
                "stop" -> {drawRoundRect(tint,topLeft=Offset(6f,6f),size=androidx.compose.ui.geometry.Size(12f,12f),cornerRadius=androidx.compose.ui.geometry.CornerRadius(2f))}
                "folder" -> { val p=Path().apply { moveTo(3f,6f); quadraticTo(3f,4f,5f,4f); lineTo(10f,4f); lineTo(12f,7f); lineTo(19f,7f); quadraticTo(21f,7f,21f,9f); lineTo(21f,19f); quadraticTo(21f,21f,19f,21f); lineTo(5f,21f); quadraticTo(3f,21f,3f,19f); close() }; drawPath(p,ink,style=if(filled) androidx.compose.ui.graphics.drawscope.Fill else stroke) }
                "compose" -> { val p=Path().apply { moveTo(14f,4f);lineTo(5f,4f);quadraticTo(3f,4f,3f,6f);lineTo(3f,19f);quadraticTo(3f,21f,5f,21f);lineTo(18f,21f);quadraticTo(20f,21f,20f,19f);lineTo(20f,12f) };drawPath(p,tint,style=stroke); val pen=Path().apply{moveTo(10f,14f);lineTo(11f,10f);lineTo(19f,2f);lineTo(22f,5f);lineTo(14f,13f);close()};drawPath(pen,tint,style=stroke) }
                "file" -> {val p=Path().apply{moveTo(5f,2f);lineTo(15f,2f);lineTo(20f,7f);lineTo(20f,22f);lineTo(5f,22f);close()};drawPath(p,tint,style=stroke);line(8f,10f,16f,10f);line(8f,14f,16f,14f);line(8f,18f,13f,18f)}
                "bulb" -> {drawCircle(tint,6f,Offset(12f,9f),style=stroke);line(9f,15f,9f,19f);line(15f,15f,15f,19f);line(9f,19f,15f,19f);line(10f,22f,14f,22f)}
                "tune" -> {listOf(6f,12f,18f).forEachIndexed {i,y->line(3f,y,21f,y);drawCircle(tint,2f,Offset(if(i%2==0)8f else 16f,y))}}
                else -> {drawCircle(tint,8f,Offset(12f,12f),style=stroke);drawCircle(tint.copy(alpha=.2f),11f,Offset(12f,12f),style=Stroke(2f))}
            }
        }
    }
}

@Composable
fun AssistantIconWell(kind: String, color: Color, modifier: Modifier = Modifier.size(36.dp)) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = .10f)) {
        Box(contentAlignment = Alignment.Center) { AssistantGlyph(kind, Modifier.size(21.dp), color) }
    }
}

/** Native paths derived from the supplied silhouette remain crisp at navigation size. */
@Composable
private fun HermesNavigationPortrait(brush:Brush,modifier:Modifier=Modifier) {
    androidx.compose.foundation.Image(
        painter=androidx.compose.ui.res.painterResource(R.drawable.hermes_nav_portrait_vector),
        contentDescription=null,modifier=modifier.graphicsLayer { compositingStrategy=CompositingStrategy.Offscreen }.drawWithCache {
            onDrawWithContent { drawContent();drawRect(brush,blendMode=BlendMode.SrcIn) }
        },colorFilter=ColorFilter.tint(Color.White))
}

/** Persistent four-tab navigation: no selected icon background. */
@Composable
fun ReferenceBottomDock(selected: AppRoute, hasUnread: Boolean, onSelect: (AppRoute) -> Unit, showDivider: Boolean = selected != AppRoute.HOME) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
    if (showDivider) FixedRegionDivider()
    AssistantPanel(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=14.dp,vertical=8.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            listOf(Triple(AppRoute.HOME,"助理","home"),Triple(AppRoute.SESSIONS,"回看","history"),Triple(AppRoute.WORKSPACE,"文件","folder"),Triple(AppRoute.PROFILE,"我的","user")).forEach { (route,label,icon) ->
                val active=selected==route
                val progress by animateFloatAsState(if(active) 1f else 0f,spring(dampingRatio=.78f,stiffness=Spring.StiffnessLow),label="navSelection")
                val brush = Brush.verticalGradient(listOf(lerp(NavigationIdle,NavigationGradientStart,progress.coerceIn(0f,1f)),
                    lerp(NavigationIdle,NavigationGradientEnd,progress.coerceIn(0f,1f))))
                val color by animateColorAsState(if(active) NavigationSelected else NavigationIdle,tween(220),label="navColor")
                Column(Modifier.weight(1f).heightIn(min=48.dp).testTag("nav_${route.name}").selectable(selected=active,role=Role.Tab,interactionSource=remember { MutableInteractionSource() },indication=null,onClick={if(!active)onSelect(route)}),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(25.dp).graphicsLayer { scaleX=1f+.10f*progress;scaleY=scaleX;translationY=-1.dp.toPx()*progress },contentAlignment=Alignment.Center) {
                        if(route==AppRoute.HOME) HermesNavigationPortrait(brush,Modifier.size(29.dp,28.dp))
                        else {
                            AssistantGlyph(icon,Modifier.fillMaxSize().graphicsLayer { alpha=1f-progress.coerceIn(0f,1f) },color)
                            AssistantGlyph(icon,Modifier.fillMaxSize().graphicsLayer { alpha=progress.coerceIn(0f,1f) },color,filled=true,fillBrush=brush)
                        }
                        if(route==AppRoute.SESSIONS && hasUnread) Surface(Modifier.align(Alignment.TopEnd).size(5.dp),shape=CircleShape,color=AssistantMint) {}
                    }
                    Text(label,style=MaterialTheme.typography.labelMedium,color=color)
                }
            }
        }
    }
    }
}

/** One action style for header creation and floating creation, with a white symbol. */
@Composable
fun AssistantCreateButton(label:String,onClick:()->Unit,modifier:Modifier=Modifier,large:Boolean=false) {
    val interaction=remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) .93f else 1f,spring(dampingRatio=.7f,stiffness=Spring.StiffnessMedium),label="actionPress")
    if(large) {
        // 56dp -> 45dp visible size; retain a comfortable 48dp touch target.
        Box(modifier.size(48.dp).semantics { contentDescription=label }
            .clickable(interactionSource=interaction,indication=null,onClick=onClick),contentAlignment=Alignment.Center) {
            Surface(Modifier.size(45.dp).graphicsLayer { scaleX=scale;scaleY=scale }.testTag("conversation_create_visual"),
                shape=RoundedCornerShape(16.dp),color=AssistantAccent,contentColor=Color.White,shadowElevation=3.dp) {
                Box(contentAlignment=Alignment.Center) { AssistantGlyph("compose",Modifier.size(21.dp),Color.White) }
            }
        }
    } else {
        // A compact 24dp visible control retains a 48dp accessible touch target.
        Box(modifier.size(48.dp).semantics { contentDescription=label }.clickable(interactionSource=interaction,indication=null,onClick=onClick),contentAlignment=Alignment.Center) {
            Surface(Modifier.size(24.dp).graphicsLayer { scaleX=scale;scaleY=scale }.testTag("compact_create_visual"),shape=RoundedCornerShape(8.dp),color=AssistantAccent) {
                Box(contentAlignment=Alignment.Center) { AssistantGlyph("plus",Modifier.size(17.dp),Color.White) }
            }
        }
    }
}

/** Animate the current page in place; outgoing screens are never kept alive. */
@Composable
fun AssistantPageTransition(route:AppRoute,content:@Composable ()->Unit) {
    val progress=remember { Animatable(1f) }
    LaunchedEffect(route) { progress.snapTo(0f);progress.animateTo(1f,tween(240,easing=FastOutSlowInEasing)) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha=.6f+.4f*progress.value;translationY=10.dp.toPx()*(1f-progress.value) }) { content() }
}
