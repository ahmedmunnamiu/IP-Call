package com.ahmedmunna.ipcall.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedmunna.ipcall.IpCallApp
import com.ahmedmunna.ipcall.data.Prefs
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onAbout: () -> Unit) {
    val style = LocalGlassStyle.current
    val theme = LocalThemeState.current
    val sip = IpCallApp.sip
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by sip.account.collectAsState()
    val reg by sip.regStatus.collectAsState()
    var keepAlive by remember { mutableStateOf(Prefs.keepAlive) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { theme.importImage(ctx, uri) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        StaggerIn(0) {
            Column {
                Text("SETTINGS", color = style.subtle, fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text("Preferences", color = style.content, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---------------- Account
        StaggerIn(1) {
            Column {
                SectionLabel("Account")
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(account.ifBlank { "—" }, color = style.content, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(2.dp))
                            Text("SIP account", color = style.subtle, fontSize = 12.sp)
                        }
                        StatusChip(reg)
                    }
                    GlassKey(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        tint = RedEnd,
                        boost = 0.06f,
                        onClick = { sip.logout() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Logout, null, tint = style.content, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Sign out", color = style.content, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // ---------------- Appearance
        Spacer(Modifier.height(14.dp))
        StaggerIn(2) {
            Column {
                SectionLabel("Appearance")
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Background", color = style.content, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Presets.forEachIndexed { i, p ->
                            BackgroundTile(
                                selected = !theme.useImage && theme.preset == i,
                                onClick = { theme.selectPreset(i) },
                                label = p.name,
                                brush = Brush.linearGradient(p.base + p.blobs.take(1))
                            )
                        }
                        // own photo tile
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val sel = theme.useImage
                            val bc by animateColorAsState(if (sel) style.content else Color.Transparent, tween(250), label = "tileBorder")
                            Box(
                                Modifier
                                    .size(62.dp, 82.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .glass(RoundedCornerShape(18.dp), boost = 0.05f)
                                    .border(2.dp, bc, RoundedCornerShape(18.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        if (theme.imageBitmap != null && !theme.useImage) theme.selectImage()
                                        else picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, null, tint = style.content, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Photo", color = style.subtle, fontSize = 11.sp)
                        }
                    }

                    AnimatedVisibility(
                        visible = theme.useImage,
                        enter = expandVertically(tween(350)) + fadeIn(tween(350)),
                        exit = shrinkVertically(tween(300)) + fadeOut(tween(200))
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            GlassKey(
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                            ) { Text("Choose another photo", color = style.content, fontSize = 14.sp) }
                            SliderRow("Dim photo", theme.dim, 0f..0.8f) { theme.updateDim(it) }
                            SliderRow("Blur photo (Android 12+)", theme.imageBlur, 0f..1f) { theme.updateImageBlur(it) }
                        }
                    }

                    Column(Modifier.padding(horizontal = 16.dp)) {
                        SliderRow("Glass blur (Android 12+)", theme.glassBlur, 0f..1f) { theme.updateGlassBlur(it) }
                        SliderRow("Glass brightness", theme.glassOpacity, 0f..1f) { theme.updateGlassOpacity(it) }
                    }
                    ToggleRow("Animated background", theme.animated) { theme.updateAnimated(it) }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // ---------------- Calls
        Spacer(Modifier.height(14.dp))
        StaggerIn(3) {
            Column {
                SectionLabel("Calls")
                GlassCard(Modifier.fillMaxWidth()) {
                    ToggleRow("Receive calls in background", keepAlive) {
                        keepAlive = it
                        sip.setKeepAlive(it)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                try {
                                    ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                } catch (_: Throwable) {
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.BatteryChargingFull, null, tint = style.subtle, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Battery optimization", color = style.content, fontWeight = FontWeight.Medium)
                            Text("Allow IP CALL to run unrestricted for reliable incoming calls", color = style.subtle, fontSize = 12.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = style.subtle)
                    }
                }
            }
        }

        // ---------------- About
        Spacer(Modifier.height(14.dp))
        StaggerIn(4) {
            Column {
                SectionLabel("About")
                GlassCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onAbout() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, tint = style.subtle, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text("About IP CALL", color = style.content, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null, tint = style.subtle)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BackgroundTile(selected: Boolean, onClick: () -> Unit, label: String, brush: Brush) {
    val style = LocalGlassStyle.current
    val borderColor by animateColorAsState(if (selected) style.content else Color.Transparent, tween(250), label = "tileBorder")
    val pad by animateDpAsState(if (selected) 0.dp else 2.dp, tween(250), label = "tilePad")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(62.dp, 82.dp)
                .padding(pad)
                .clip(RoundedCornerShape(18.dp))
                .background(brush)
                .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(18.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = style.subtle, fontSize = 11.sp)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val style = LocalGlassStyle.current
    Column(Modifier.padding(top = 10.dp)) {
        Text(label, color = style.content, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.9f),
                inactiveTrackColor = style.subtle.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val style = LocalGlassStyle.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = style.content, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GreenCall,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = style.subtle.copy(alpha = 0.35f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
