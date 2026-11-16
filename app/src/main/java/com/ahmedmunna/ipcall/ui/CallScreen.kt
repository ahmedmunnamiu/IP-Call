package com.ahmedmunna.ipcall.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedmunna.ipcall.IpCallApp
import com.ahmedmunna.ipcall.sip.CallPhase
import com.ahmedmunna.ipcall.sip.CallUi
import kotlinx.coroutines.delay

private fun mmss(totalSec: Long): String {
    val s = if (totalSec < 0) 0 else totalSec
    return "%02d:%02d".format(s / 60, s % 60)
}

@Composable
fun CallScreen(c: CallUi) {
    val style = LocalGlassStyle.current
    val sip = IpCallApp.sip
    val tone = rememberTone()
    var showPad by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(c.phase) {
        while (c.phase == CallPhase.Active || c.phase == CallPhase.Held) {
            now = SystemClock.elapsedRealtime()
            delay(500)
        }
    }

    val ringing = c.phase == CallPhase.Incoming || c.phase == CallPhase.Dialing || c.phase == CallPhase.Ringing

    GlassScreen {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = c.phase,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "status"
            ) { ph ->
                val text = when (ph) {
                    CallPhase.Incoming -> "Incoming call"
                    CallPhase.Dialing -> "Calling…"
                    CallPhase.Ringing -> "Ringing…"
                    CallPhase.Active -> mmss((now - c.connectedAtMs) / 1000)
                    CallPhase.Held -> "On hold · " + mmss((now - c.connectedAtMs) / 1000)
                    CallPhase.Ended -> c.note.ifBlank { "Call ended" }
                }
                Text(text, color = style.subtle, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                c.name, color = style.content, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (c.name != c.number) {
                Spacer(Modifier.height(4.dp))
                Text(c.number, color = style.subtle, fontSize = 15.sp)
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = showPad,
                    transitionSpec = {
                        (fadeIn(tween(350)) + scaleIn(tween(350, easing = FastOutSlowInEasing), initialScale = 0.9f)) togetherWith
                            (fadeOut(tween(200)) + scaleOut(tween(250), targetScale = 0.95f))
                    },
                    label = "padSwitch"
                ) { pad ->
                    if (pad) InCallPad(onKey = { ch -> tone(ch); sip.sendDtmf(ch) }) else Avatar(c, ringing)
                }
            }

            if (c.phase == CallPhase.Incoming) {
                val pulse = rememberInfiniteTransition(label = "acceptPulse").animateFloat(
                    initialValue = 1f, targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "acceptScale"
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LabeledRound(Icons.Filled.CallEnd, "Decline", 76.dp, RedEnd, 0.12f, onClick = { sip.hangup() })
                    Box(Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }) {
                        LabeledRound(Icons.Filled.Call, "Accept", 76.dp, GreenCall, 0.12f, onClick = { sip.answer() })
                    }
                }
                Spacer(Modifier.height(24.dp))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LabeledRound(
                        if (c.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        "Mute", 64.dp, boost = if (c.muted) 0.28f else 0f, onClick = { sip.toggleMute() }
                    )
                    LabeledRound(
                        Icons.Filled.Dialpad, "Keypad", 64.dp,
                        boost = if (showPad) 0.28f else 0f, onClick = { showPad = !showPad }
                    )
                    LabeledRound(
                        Icons.Filled.VolumeUp, "Speaker", 64.dp,
                        boost = if (c.speaker) 0.28f else 0f, onClick = { sip.toggleSpeaker() }
                    )
                    LabeledRound(
                        if (c.phase == CallPhase.Held) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        if (c.phase == CallPhase.Held) "Resume" else "Hold", 64.dp,
                        boost = if (c.phase == CallPhase.Held) 0.28f else 0f,
                        onClick = { sip.toggleHold() }
                    )
                }
                Spacer(Modifier.height(26.dp))
                LabeledRound(
                    Icons.Filled.CallEnd, "", 76.dp, RedEnd, 0.16f,
                    enabled = c.phase != CallPhase.Ended,
                    onClick = { sip.hangup() }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun LabeledRound(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    size: Dp,
    tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    boost: Float = 0f,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val style = LocalGlassStyle.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GlassRoundButton(icon = icon, size = size, onClick = onClick, tint = tint, boost = boost, enabled = enabled)
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(label, color = style.subtle, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Avatar(c: CallUi, ringing: Boolean) {
    val style = LocalGlassStyle.current
    val inf = rememberInfiniteTransition(label = "rings")
    val p1 = inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing)),
        label = "p1"
    )
    val p2 = inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearOutSlowInEasing), initialStartOffset = StartOffset(1200)),
        label = "p2"
    )
    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        if (ringing) {
            Canvas(Modifier.fillMaxSize()) {
                listOf(p1.value, p2.value).forEach { p ->
                    drawCircle(
                        color = style.content.copy(alpha = (1f - p) * 0.30f),
                        radius = size.minDimension / 2f * (0.55f + 0.45f * p),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        Box(Modifier.size(150.dp).glass(CircleShape, boost = 0.10f), contentAlignment = Alignment.Center) {
            val initial = c.name.firstOrNull { it.isLetter() }?.uppercase()
            if (initial != null) {
                Text(initial, color = style.content, fontSize = 60.sp, fontWeight = FontWeight.Light)
            } else {
                Icon(Icons.Filled.Person, null, tint = style.content, modifier = Modifier.size(70.dp))
            }
        }
    }
}

@Composable
private fun InCallPad(onKey: (Char) -> Unit) {
    val style = LocalGlassStyle.current
    val rows = listOf("123", "456", "789", "*0#")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                r.forEach { ch ->
                    GlassKey(
                        modifier = Modifier.size(width = 80.dp, height = 58.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = { onKey(ch) }
                    ) {
                        Text(ch.toString(), color = style.content, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}
