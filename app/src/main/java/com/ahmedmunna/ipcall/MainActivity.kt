package com.ahmedmunna.ipcall

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.ahmedmunna.ipcall.ui.IpCallRoot
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ANSWER = "ipcall_answer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Show over the lock screen only while a call is going on.
        lifecycleScope.launch {
            IpCallApp.sip.call.collect { c ->
                val on = c != null
                if (Build.VERSION.SDK_INT >= 27) {
                    setShowWhenLocked(on)
                    setTurnScreenOn(on)
                }
            }
        }

        setContent { IpCallRoot() }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_ANSWER, false) == true) {
            i.removeExtra(EXTRA_ANSWER)
            IpCallApp.sip.answer()
        }
    }
}
