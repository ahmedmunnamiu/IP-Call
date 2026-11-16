package com.ahmedmunna.ipcall.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedmunna.ipcall.sip.RegStatus
import kotlinx.coroutines.delay

val GreenCall = Color(0xFF2EBD6B)
val RedEnd = Color(0xFFE5484D)

/** A glass card that holds a group of content. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    boost: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.glass(shape, boost), content = content)
}

/** Full-screen glass sheet used for overlays (call screen, about). Covers everything beneath it. */
@Composable
fun GlassScreen(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .glass(RectangleShape, boost = 0.10f, rim = false),
        content = content
    )
}

/** Tappable glass surface with a springy "liquid" press animation. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassKey(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    tint: Color = Color.White,
    boost: Float = 0f,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f),
        label = "keyScale"
    )
    val extra by animateFloatAsState(if (pressed) 0.16f else 0f, tween(160), label = "keyGlow")
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(shape, boost = boost + extra, tint = tint)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** Round glass button with an icon (and optional label underneath). */
@Composable
fun GlassRoundButton(
    icon: ImageVector,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    boost: Float = 0f,
    iconColor: Color = LocalGlassStyle.current.content,
    enabled: Boolean = true
) {
    GlassKey(
        modifier = modifier.size(size),
        shape = CircleShape,
        onClick = onClick,
        tint = tint,
        boost = boost,
        enabled = enabled
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(size * 0.42f))
    }
}

/** Fades + slides content in with a small delay, so screens build up smoothly. */
@Composable
fun StaggerIn(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(450)) + slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 5 }
    ) {
        content()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = LocalGlassStyle.current.subtle,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 6.dp, bottom = 8.dp, top = 6.dp)
    )
}

@Composable
fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
    leading: ImageVector? = null
) {
    val style = LocalGlassStyle.current
    var focused by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(260), label = "fieldGlow")
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp), boost = 0.03f + 0.10f * glow)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            Icon(leading, null, tint = style.subtle, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(label, color = style.subtle, fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = style.content, fontSize = 16.sp),
                cursorBrush = SolidColor(style.content),
                visualTransformation = if (password && !show) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
            )
        }
        if (password) {
            Icon(
                if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
                tint = style.subtle,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { show = !show }
            )
        }
    }
}

/** Small glass pill showing the SIP registration state. */
@Composable
fun StatusChip(status: RegStatus, modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    val color by animateColorAsState(
        when (status) {
            RegStatus.Registered -> GreenCall
            RegStatus.Connecting -> Color(0xFFF5A524)
            RegStatus.Failed -> RedEnd
            RegStatus.Idle -> Color(0xFF9AA5B1)
        },
        tween(400),
        label = "chipColor"
    )
    val pulse = rememberInfiniteTransition(label = "chipPulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "chipPulseV"
    )
    val label = when (status) {
        RegStatus.Registered -> "Online"
        RegStatus.Connecting -> "Connecting"
        RegStatus.Failed -> "Offline"
        RegStatus.Idle -> "Not connected"
    }
    Row(
        modifier.glass(RoundedCornerShape(50), boost = 0.04f).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .alpha(if (status == RegStatus.Connecting) pulse.value else 1f)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, color = style.content, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** DTMF tone for keypad presses. */
@Composable
fun rememberTone(): (Char) -> Unit {
    val tg = remember {
        try {
            ToneGenerator(AudioManager.STREAM_DTMF, 60)
        } catch (_: Throwable) {
            null
        }
    }
    DisposableEffect(Unit) { onDispose { tg?.release() } }
    return remember {
        { c: Char ->
            val code = when (c) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> -1
            }
            if (code >= 0) {
                try {
                    tg?.startTone(code, 110)
                } catch (_: Throwable) {
                }
            }
        }
    }
}
