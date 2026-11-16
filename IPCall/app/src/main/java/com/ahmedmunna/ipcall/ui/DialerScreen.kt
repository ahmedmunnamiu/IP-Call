package com.ahmedmunna.ipcall.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedmunna.ipcall.IpCallApp

private val KeyRows = listOf(
    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
    listOf("*" to "", "0" to "+", "#" to "")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(number: String, onNumber: (String) -> Unit, onCall: () -> Unit) {
    val style = LocalGlassStyle.current
    val reg by IpCallApp.sip.regStatus.collectAsState()
    val haptic = LocalHapticFeedback.current
    val tone = rememberTone()
    val clipboard = LocalClipboardManager.current

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))
        StaggerIn(0) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("KEYPAD", color = style.subtle, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("Make a call", color = style.content, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                StatusChip(reg)
            }
        }
        Spacer(Modifier.height(20.dp))

        // number display (long press = paste)
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = {
                        val pasted = clipboard.getText()?.text.orEmpty().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                        if (pasted.isNotEmpty()) onNumber(pasted)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val size = when {
                number.length > 18 -> 20.sp
                number.length > 13 -> 26.sp
                else -> 32.sp
            }
            if (number.isEmpty()) {
                Text("000 000 000", color = style.subtle.copy(alpha = 0.5f), fontSize = 32.sp, fontWeight = FontWeight.Normal)
            } else {
                Text(number, color = style.content, fontSize = size, fontWeight = FontWeight.Normal, maxLines = 1)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(style.subtle.copy(alpha = 0.45f)))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
            val gap = 14.dp
            val kw = (maxWidth - gap * 2) / 3
            val kh = minOf(kw * 0.72f, (maxHeight - gap * 4) / 5)
            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(gap)) {
                KeyRows.forEachIndexed { rowIdx, row ->
                    StaggerIn(rowIdx + 1) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            row.forEach { (d, letters) ->
                                DialKey(
                                    digit = d,
                                    letters = letters,
                                    modifier = Modifier.size(kw, kh),
                                    onPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        tone(d[0])
                                        onNumber(number + d)
                                    },
                                    onLong = if (d == "0") {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNumber(number + "+")
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
                StaggerIn(5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        GlassKey(
                            modifier = Modifier.size(kw * 2 + gap, kh),
                            shape = RoundedCornerShape(26.dp),
                            onClick = { if (number.isNotEmpty()) onCall() },
                            boost = 0.06f
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.alpha(if (number.isEmpty()) 0.55f else 1f)
                            ) {
                                Icon(Icons.Filled.Call, null, tint = GreenCall, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("CALL", color = style.content, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                            }
                        }
                        GlassKey(
                            modifier = Modifier.size(kw, kh),
                            shape = RoundedCornerShape(26.dp),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (number.isNotEmpty()) onNumber(number.dropLast(1))
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNumber("")
                            }
                        ) {
                            Icon(Icons.Filled.Backspace, null, tint = style.content, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialKey(
    digit: String,
    letters: String,
    modifier: Modifier,
    onPress: () -> Unit,
    onLong: (() -> Unit)?
) {
    val style = LocalGlassStyle.current
    GlassKey(modifier = modifier, shape = RoundedCornerShape(26.dp), onClick = onPress, onLongClick = onLong) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digit, color = style.content, fontSize = 28.sp, fontWeight = FontWeight.Normal)
            if (letters.isNotEmpty()) {
                Text(letters, color = style.subtle, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
