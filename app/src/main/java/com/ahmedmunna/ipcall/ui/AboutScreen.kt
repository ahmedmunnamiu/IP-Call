package com.ahmedmunna.ipcall.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val style = LocalGlassStyle.current
    val ctx = LocalContext.current
    val version = remember(ctx) {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (_: Throwable) {
            "1.0"
        }
    }

    GlassScreen {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp)) {
            GlassRoundButton(icon = Icons.Filled.ArrowBack, size = 48.dp, onClick = onBack)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                StaggerIn(0) {
                    Box(Modifier.size(104.dp).glass(CircleShape, boost = 0.10f), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Call, null, tint = style.content, modifier = Modifier.size(46.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                StaggerIn(1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("IP CALL", color = style.content, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Version $version", color = style.subtle, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(30.dp))
                StaggerIn(2) {
                    GlassCard(Modifier.fillMaxWidth(), boost = 0.04f) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("DEVELOPED BY", color = style.subtle, fontSize = 12.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "AHMED MUNNA", color = style.content, fontSize = 26.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            ContactLine(Icons.Filled.Phone, "Mobile", "+8801625104847") {
                                openLink(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:+8801625104847")))
                            }
                            Spacer(Modifier.height(10.dp))
                            ContactLine(Icons.Filled.Public, "Facebook", "facebook.com/ahmedmunna0") {
                                openLink(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/ahmedmunna0")))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                StaggerIn(3) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("A simple SIP dialer", color = style.content, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Sign in with any SIP account to make and receive calls, wrapped in a liquid glass interface.",
                                color = style.subtle, fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                StaggerIn(4) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Open-source credits", color = style.content, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "SIP engine: Linphone SDK (GPLv3) by Belledonne Communications.",
                                color = style.subtle, fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

private fun openLink(ctx: Context, intent: Intent) {
    try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Throwable) {
    }
}

@Composable
private fun ContactLine(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    val style = LocalGlassStyle.current
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(16.dp), boost = 0.02f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = style.content, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = style.subtle, fontSize = 11.sp)
            Text(value, color = style.content, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
