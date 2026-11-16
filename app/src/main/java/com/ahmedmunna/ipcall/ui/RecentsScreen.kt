package com.ahmedmunna.ipcall.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedmunna.ipcall.IpCallApp
import com.ahmedmunna.ipcall.sip.LogItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsScreen(onCall: (String) -> Unit) {
    val style = LocalGlassStyle.current
    val logs by IpCallApp.sip.logs.collectAsState()
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("HISTORY", color = style.subtle, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Recent calls", color = style.content, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            if (logs.isNotEmpty()) {
                GlassRoundButton(
                    icon = Icons.Filled.DeleteSweep,
                    size = 46.dp,
                    onClick = { IpCallApp.sip.clearLogs() }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                StaggerIn(0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(84.dp).glass(CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.History, null, tint = style.subtle, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("No calls yet", color = style.subtle, fontSize = 15.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(logs, key = { i, it -> "${it.startSec}_${it.number}_${it.durationSec}_$i" }) { _, item ->
                    LogRow(item, fmt, onCall, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun LogRow(item: LogItem, fmt: SimpleDateFormat, onCall: (String) -> Unit, modifier: Modifier) {
    val style = LocalGlassStyle.current
    val (icon, color) = when {
        item.missed -> Icons.Filled.CallMissed to RedEnd
        item.incoming -> Icons.Filled.CallReceived to GreenCall
        else -> Icons.Filled.CallMade to Color(0xFF4F8CFF)
    }
    val dur = if (item.durationSec > 0) " · %d:%02d".format(item.durationSec / 60, item.durationSec % 60) else ""
    Row(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), boost = 0.02f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onCall(item.number)
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name, color = if (item.missed) RedEnd else style.content,
                fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                fmt.format(Date(item.startSec * 1000L)) + dur,
                color = style.subtle, fontSize = 12.sp
            )
        }
        Icon(Icons.Filled.Call, null, tint = style.subtle, modifier = Modifier.size(20.dp))
    }
}
