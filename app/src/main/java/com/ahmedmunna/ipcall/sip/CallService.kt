package com.ahmedmunna.ipcall.sip

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.ahmedmunna.ipcall.IpCallApp

/** Keeps the process (and the SIP registration) alive so incoming calls can arrive in the background. */
class CallService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = Notifier.serviceNotification(this)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(Notifier.ID_SERVICE, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(Notifier.ID_SERVICE, n)
            }
        } catch (_: Throwable) {
            stopSelf()
        }
        return START_STICKY
    }
}

/** Handles the "Decline" button of the incoming-call notification. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        IpCallApp.sip.hangup()
    }
}
