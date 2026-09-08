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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
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

val AssistantAccent = Color(0xFF009BDE)
val AssistantMint = Color(0xFF06B9A9)
val AssistantPurple = Color(0xFF8260E7)

@Composable
fun AssistantPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)),
        shadowElevation = 0.dp, content = content)
}

/** A native contour clip removes the rectangular backdrop without tinting the face.
 * Coordinates follow the unmodified 1254px portrait; holes between curls stay transparent. */
private val hermesPortraitContour = """
    M 145 646 C 65 649 26 720 43 779 C 54 810 80 824 109 829
    C 60 859 33 891 44 938 C 53 985 86 1010 123 1015
    C 133 1050 163 1076 199 1072 C 191 1091 171 1100 151 1092
    C 159 1107 174 1111 186 1113 L 183 1119
    C 411 1253 815 1267 1093 1103 L 1077 1092
    C 1118 1085 1136 1057 1131 1024 C 1101 1054 1074 1057 1049 1040
    C 1063 1024 1078 1014 1080 990 C 1094 1005 1091 1008 1088 1008
    C 1144 1007 1175 951 1180 901 C 1185 877 1179 852 1170 846
    C 1154 890 1110 907 1051 878 C 1116 882 1152 837 1138 793
    C 1126 753 1100 735 1077 735 C 1110 760 1101 807 1068 809
    C 1005 811 969 689 959 545 C 999 531 1017 483 1007 424
    C 1002 328 960 232 888 183 C 842 133 769 100 681 89
    C 508 58 375 135 307 286 C 268 374 256 491 220 597
    C 191 682 164 733 137 743 C 95 749 91 684 145 646 Z
    M 124 913 C 103 943 99 973 114 998 C 104 994 95 966 103 943 Z
    M 183 1007 C 178 1029 186 1049 198 1058 C 184 1052 176 1038 179 1020 Z
    M 1070 965 C 1088 980 1092 995 1085 1005 C 1084 987 1076 978 1070 965 Z
""".trimIndent()

@Composable
fun ReferenceHermesGirl(modifier: Modifier = Modifier) {
    val artwork = ImageBitmap.imageResource(R.drawable.hermes_home_portrait)
    val contour = remember {
        androidx.compose.ui.graphics.vector.PathParser().parsePathString(hermesPortraitContour).toPath().apply {
            fillType = PathFillType.EvenOdd
        }
    }
    Canvas(modifier.testTag("home_hermes_portrait")) {
        // The portrait has alpha outside its native clip on both themes, including the curls.
        // End above the source artwork's curved lower edge; the canvas ends at this flat cut.
        scale(size.width / 1254f, size.height / 1080f, pivot = Offset.Zero) {
            clipRect(left = 0f, top = 0f, right = 1254f, bottom = 1080f) { clipPath(contour) {
                drawImage(artwork, dstSize = IntSize(1254, 1254), filterQuality = FilterQuality.High)
            } }
        }
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
                "tasks" -> {
                    val body = Path().apply {
                        moveTo(7f,3f); lineTo(17f,3f); quadraticTo(20f,3f,20f,6f)
                        lineTo(20f,19f); quadraticTo(20f,22f,17f,22f); lineTo(7f,22f)
                        quadraticTo(4f,22f,4f,19f); lineTo(4f,6f); quadraticTo(4f,3f,7f,3f); close()
                    }
                    drawPath(body,ink,style=if(filled) androidx.compose.ui.graphics.drawscope.Fill else stroke)
                    val detail = if(filled) cutout else tint
                    fun mark(x:Float,y:Float,x2:Float,y2:Float) = drawLine(detail,Offset(x,y),Offset(x2,y2),1.65f,StrokeCap.Round)
                    mark(7f,9f,8.5f,10.5f); mark(8.5f,10.5f,11f,7.5f); mark(13.5f,9f,17f,9f)
                    mark(7f,16f,8.5f,17.5f); mark(8.5f,17.5f,11f,14.5f); mark(13.5f,16f,17f,16f)
                }
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

/** Five tabs share one opaque lower backing, with no selected icon background. */
@Composable
fun ReferenceBottomDock(selected: AppRoute, hasUnread: Boolean, onSelect: (AppRoute) -> Unit,
    navigationInsets: WindowInsets = WindowInsets.navigationBars) {
    Column(Modifier.fillMaxWidth().padding(top=8.dp).background(MaterialTheme.colorScheme.background)
        .testTag("bottom_dock_occlusion").windowInsetsPadding(navigationInsets).padding(bottom=8.dp)) {
    Surface(Modifier.fillMaxWidth().padding(horizontal=14.dp).testTag("floating_bottom_dock"),
        shape=RoundedCornerShape(26.dp), color=MaterialTheme.colorScheme.surface,
        shadowElevation=5.dp) {
        Row(Modifier.fillMaxWidth().heightIn(min=64.dp).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
            listOf(Triple(AppRoute.HOME,"助理","home"),Triple(AppRoute.SESSIONS,"回看","history"),Triple(AppRoute.TASKS,"任务","tasks"),Triple(AppRoute.WORKSPACE,"文件","folder"),Triple(AppRoute.PROFILE,"我的","user")).forEach { (route,label,icon) ->
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
