package com.ahmedmunna.ipcall.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ahmedmunna.ipcall.IpCallApp
import com.ahmedmunna.ipcall.sip.CallUi

@Composable
fun IpCallRoot() {
    val ctx = LocalContext.current
    val theme = remember { ThemeState() }
    LaunchedEffect(Unit) { theme.loadSaved(ctx) }

    MaterialTheme(colorScheme = if (theme.isDark) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalThemeState provides theme) {
            GlassHost(theme) {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    val sip = IpCallApp.sip
    val loggedIn by sip.loggedIn.collectAsState()
    val call by sip.call.collectAsState()
    var showAbout by remember { mutableStateOf(false) }
    var lastCall by remember { mutableStateOf<CallUi?>(null) }
    if (call != null) lastCall = call

    // ask for microphone + notification permission on first start
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 31) perms += Manifest.permission.BLUETOOTH_CONNECT
        permLauncher.launch(perms.toTypedArray())
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = loggedIn,
            transitionSpec = {
                (fadeIn(tween(500)) + scaleIn(tween(500, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(300)) + scaleOut(tween(350), targetScale = 1.05f))
            },
            label = "route"
        ) { li ->
            if (li) MainScreen(onAbout = { showAbout = true }) else LoginScreen()
        }

        AnimatedVisibility(
            visible = showAbout && loggedIn,
            enter = fadeIn(tween(350)) + slideInVertically(tween(450, easing = FastOutSlowInEasing)) { it / 6 } +
                scaleIn(tween(450), initialScale = 0.94f),
            exit = fadeOut(tween(250)) + slideOutVertically(tween(350, easing = FastOutSlowInEasing)) { it / 8 } +
                scaleOut(tween(300), targetScale = 0.96f)
        ) {
            AboutScreen(onBack = { showAbout = false })
        }
        BackHandler(enabled = showAbout) { showAbout = false }

        AnimatedVisibility(
            visible = call != null,
            enter = fadeIn(tween(350)) + slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 3 },
            exit = fadeOut(tween(350)) + slideOutVertically(tween(450, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            lastCall?.let { CallScreen(it) }
        }
        BackHandler(enabled = call != null) { /* stay on the call screen */ }
    }
}
