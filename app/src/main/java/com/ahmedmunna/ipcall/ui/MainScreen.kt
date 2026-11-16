package com.ahmedmunna.ipcall.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ahmedmunna.ipcall.IpCallApp

/** Returns a function that runs an action once the microphone permission is granted. */
@Composable
fun rememberMicGuard(): (() -> Unit) -> Unit {
    val ctx = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) pending?.invoke()
        pending = null
    }
    return remember<(() -> Unit) -> Unit> {
        { action ->
            val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) action() else {
                pending = action
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@Composable
fun MainScreen(onAbout: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var number by rememberSaveable { mutableStateOf("") }
    val micGuard = rememberMicGuard()
    val sip = IpCallApp.sip

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(380)) +
                        slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { dir * it / 5 }) togetherWith
                        (fadeOut(tween(220)) +
                            slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { -dir * it / 5 })
                },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> DialerScreen(
                        number = number,
                        onNumber = { number = it },
                        onCall = { micGuard { sip.placeCall(number) } }
                    )
                    1 -> RecentsScreen(onCall = { n -> micGuard { sip.placeCall(n) } })
                    else -> SettingsScreen(onAbout = onAbout)
                }
            }
        }
        GlassNavBar(selected = tab, onSelect = { tab = it })
    }
}

@Composable
private fun GlassNavBar(selected: Int, onSelect: (Int) -> Unit) {
    val style = LocalGlassStyle.current
    val items = listOf(
        Triple(Icons.Filled.Dialpad, "Keypad", 0),
        Triple(Icons.Filled.History, "Recents", 1),
        Triple(Icons.Filled.Settings, "Settings", 2)
    )
    BoxWithConstraints(
        Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(66.dp)
            .glass(RoundedCornerShape(33.dp), boost = 0.04f)
    ) {
        val itemW = maxWidth / 3
        val x by animateDpAsState(
            targetValue = itemW * selected,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "navIndicator"
        )
        Box(
            Modifier
                .offset(x = x)
                .width(itemW)
                .fillMaxHeight()
                .padding(6.dp)
                .glass(RoundedCornerShape(27.dp), boost = 0.22f)
        )
        Row(Modifier.fillMaxSize()) {
            items.forEach { (icon, label, index) ->
                val col by animateColorAsState(
                    if (selected == index) style.content else style.subtle, tween(250), label = "navColor"
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onSelect(index)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(icon, null, tint = col, modifier = Modifier.size(24.dp))
                    Text(
                        label, color = col, fontSize = 11.sp,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
