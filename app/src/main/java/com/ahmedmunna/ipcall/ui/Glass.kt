package com.ahmedmunna.ipcall.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Immutable
data class GlassStyle(
    val content: Color,
    val subtle: Color,
    val dark: Boolean,
    val blur: Float,
    val opacity: Float
)

val LocalGlassStyle = staticCompositionLocalOf {
    GlassStyle(Color(0xFF14283C), Color(0x9914283C), false, 0.5f, 0.5f)
}

/** The (blurred) background, recorded once, that every glass surface samples from. */
val LocalBackdrop = staticCompositionLocalOf<GraphicsLayer?> { null }

/**
 * Draws the app background and provides the "liquid glass" backdrop to everything inside [content].
 * Real frosted blur needs Android 12+ (API 31); older versions get translucent glass without blur.
 */
@Composable
fun GlassHost(theme: ThemeState, content: @Composable () -> Unit) {
    val layer = rememberGraphicsLayer()
    val dark = theme.isDark
    val style = GlassStyle(
        content = if (dark) Color.White else Color(0xFF14283C),
        subtle = if (dark) Color(0xB3FFFFFF) else Color(0x9914283C),
        dark = dark,
        blur = theme.glassBlur,
        opacity = theme.glassOpacity
    )
    SideEffect {
        layer.renderEffect = if (Build.VERSION.SDK_INT >= 31 && style.blur > 0.01f) {
            val r = 4f + style.blur * 36f
            BlurEffect(r, r, TileMode.Clamp)
        } else null
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record(this, layoutDirection, size.toIntSize()) {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
                }
        ) {
            BackgroundLayer(theme)
        }
        CompositionLocalProvider(
            LocalBackdrop provides layer,
            LocalGlassStyle provides style
        ) {
            content()
        }
    }
}

@Composable
private fun BackgroundLayer(theme: ThemeState) {
    val img = theme.imageBitmap
    Box(Modifier.fillMaxSize()) {
        if (theme.useImage && img != null) {
            val blurMod = if (Build.VERSION.SDK_INT >= 31 && theme.imageBlur > 0.01f)
                Modifier.blur((theme.imageBlur * 25f).dp) else Modifier
            Image(
                bitmap = img,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().then(blurMod)
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = theme.dim)))
        } else {
            GradientBackground(Presets[theme.preset.coerceIn(0, Presets.lastIndex)], theme.animated)
        }
    }
}

@Composable
private fun GradientBackground(p: Preset, animated: Boolean) {
    val t: State<Float> = if (animated) {
        rememberInfiniteTransition(label = "bg").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
            label = "bgT"
        )
    } else {
        remember { mutableFloatStateOf(0.15f) }
    }
    Canvas(Modifier.fillMaxSize()) {
        val tau = (2.0 * PI).toFloat()
        val ph = t.value * tau
        drawRect(
            Brush.linearGradient(
                p.base,
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
        )
        p.blobs.forEachIndexed { i, col ->
            val f = if (i % 2 == 0) 1f else 2f
            val cx = size.width * (0.5f + 0.38f * sin(ph + i * 2.1f))
            val cy = size.height * (0.5f + 0.36f * cos(ph * f + i * 1.7f))
            val r = size.minDimension * (0.55f + 0.12f * i)
            drawCircle(
                Brush.radialGradient(
                    listOf(col.copy(alpha = 0.65f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r
                ),
                radius = r,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Liquid glass surface: blurred backdrop + soft tint + specular highlight + glossy rim.
 * [boost] makes the glass brighter (pressed / selected). [tint] colours the glass.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    boost: Float = 0f,
    tint: Color = Color.White,
    rim: Boolean = true
): Modifier {
    val layer = LocalBackdrop.current
    val style = LocalGlassStyle.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    val a = (0.08f + 0.30f * style.opacity + boost).coerceIn(0f, 0.9f)
    val rimBrush = remember {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.50f))
        )
    }
    var m = this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            if (layer != null) {
                translate(-pos.x, -pos.y) { drawLayer(layer) }
            }
            drawRect(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = (a + 0.12f).coerceAtMost(0.95f)), tint.copy(alpha = a * 0.55f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            // specular highlight (top-left light reflection)
            drawRect(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(size.width * 0.15f, 0f),
                    radius = size.maxDimension * 0.9f
                )
            )
            // soft shade at the bottom for depth
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.07f)),
                    startY = size.height * 0.45f,
                    endY = size.height
                )
            )
        }
    if (rim) m = m.border(1.dp, rimBrush, shape)
    return m
}
