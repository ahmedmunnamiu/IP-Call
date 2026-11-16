package com.ahmedmunna.ipcall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedmunna.ipcall.IpCallApp
import com.ahmedmunna.ipcall.data.Prefs
import com.ahmedmunna.ipcall.sip.SipConfig

@Composable
fun LoginScreen() {
    val style = LocalGlassStyle.current
    val sip = IpCallApp.sip
    val busy by sip.loginBusy.collectAsState()
    val error by sip.loginError.collectAsState()
    val focus = LocalFocusManager.current

    var displayName by rememberSaveable { mutableStateOf(Prefs.displayName) }
    var username by rememberSaveable { mutableStateOf(Prefs.username) }
    var password by rememberSaveable { mutableStateOf("") }
    var domain by rememberSaveable { mutableStateOf(Prefs.domain) }
    var advanced by rememberSaveable { mutableStateOf(false) }
    var authUser by rememberSaveable { mutableStateOf(Prefs.authUser) }
    var proxy by rememberSaveable { mutableStateOf(Prefs.proxy) }
    var port by rememberSaveable { mutableStateOf(if (Prefs.port > 0) Prefs.port.toString() else "") }
    var transport by rememberSaveable { mutableStateOf(Prefs.transport) }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        focus.clearFocus()
        if (username.isBlank() || password.isBlank() || domain.isBlank()) {
            localError = "Enter username, password and server"
            return
        }
        localError = null
        sip.login(
            SipConfig(
                displayName = displayName.trim(),
                username = username.trim(),
                authUser = authUser.trim(),
                password = password,
                domain = domain.trim(),
                proxy = proxy.trim(),
                port = port.trim().toIntOrNull() ?: 0,
                transport = transport
            )
        )
    }

    val float = rememberInfiniteTransition(label = "logo").animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoY"
    )

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(36.dp))
        StaggerIn(0) {
            Box(
                Modifier
                    .graphicsLayer { translationY = float.value }
                    .size(92.dp)
                    .glass(CircleShape, boost = 0.08f),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Call, null, tint = style.content, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        StaggerIn(1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("IP CALL", color = style.content, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text("Sign in with your SIP account", color = style.subtle, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(28.dp))

        StaggerIn(2) {
            GlassField(displayName, { displayName = it }, "Display name (optional)", leading = Icons.Filled.Badge)
        }
        Spacer(Modifier.height(12.dp))
        StaggerIn(3) {
            GlassField(username, { username = it }, "Username / Extension", leading = Icons.Filled.Person)
        }
        Spacer(Modifier.height(12.dp))
        StaggerIn(4) {
            GlassField(password, { password = it }, "Password", password = true, leading = Icons.Filled.Lock)
        }
        Spacer(Modifier.height(12.dp))
        StaggerIn(5) {
            GlassField(
                domain, { domain = it }, "Server / Domain (sip.example.com)",
                keyboardType = KeyboardType.Uri,
                imeAction = if (advanced) ImeAction.Next else ImeAction.Done,
                onDone = { submit() },
                leading = Icons.Filled.Dns
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    advanced = !advanced
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Advanced settings", color = style.subtle, fontSize = 13.sp)
            Icon(
                if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                null, tint = style.subtle, modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = advanced,
            enter = expandVertically(tween(420, easing = FastOutSlowInEasing)) + fadeIn(tween(420)),
            exit = shrinkVertically(tween(320, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
        ) {
            Column {
                Spacer(Modifier.height(4.dp))
                GlassField(authUser, { authUser = it }, "Auth username (if different)", leading = Icons.Filled.Person)
                Spacer(Modifier.height(12.dp))
                GlassField(proxy, { proxy = it }, "Outbound proxy (optional)", keyboardType = KeyboardType.Uri, leading = Icons.Filled.Dns)
                Spacer(Modifier.height(12.dp))
                GlassField(port, { port = it.filter { c -> c.isDigit() }.take(5) }, "Port (default)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("UDP", "TCP", "TLS").forEach { t ->
                        val sel = transport == t
                        GlassKey(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { transport = t },
                            boost = if (sel) 0.22f else 0f
                        ) {
                            Text(
                                t, color = style.content, fontSize = 14.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        val shownError = localError ?: error
        AnimatedVisibility(
            visible = shownError != null,
            enter = expandVertically(tween(300)) + fadeIn(tween(300)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
        ) {
            Text(
                shownError ?: "",
                color = RedEnd, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(18.dp))
        StaggerIn(6) {
            GlassKey(
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(24.dp),
                onClick = { if (!busy) submit() },
                tint = GreenCall,
                boost = 0.10f
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = style.content
                    )
                } else {
                    Text("SIGN IN", color = style.content, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
