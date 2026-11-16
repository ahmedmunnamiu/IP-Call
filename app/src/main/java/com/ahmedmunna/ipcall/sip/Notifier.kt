package com.ahmedmunna.ipcall.sip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ahmedmunna.ipcall.MainActivity
import com.ahmedmunna.ipcall.R

object Notifier {
    const val CH_SERVICE = "ipcall_service"
    const val CH_CALL = "ipcall_incoming"
    const val ID_SERVICE = 11
    const val ID_CALL = 12

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Connection status", NotificationManager.IMPORTANCE_LOW)
        )
        val call = NotificationChannel(CH_CALL, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null) // the app plays the ringtone itself
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(call)
    }

    private fun flags() = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun openIntent(ctx: Context, extraAnswer: Boolean, code: Int): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (extraAnswer) i.putExtra(MainActivity.EXTRA_ANSWER, true)
        return PendingIntent.getActivity(ctx, code, i, flags())
    }

    fun serviceNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle("IP CALL")
            .setContentText("Ready to receive calls")
            .setOngoing(true)
            .setContentIntent(openIntent(ctx, false, 1))
            .build()

    fun showIncoming(ctx: Context, name: String, number: String) {
        val decline = PendingIntent.getBroadcast(
            ctx, 3, Intent(ctx, CallActionReceiver::class.java), flags()
        )
        val open = openIntent(ctx, false, 1)
        val n = NotificationCompat.Builder(ctx, CH_CALL)
            .setSmallIcon(R.drawable.ic_stat_call)
            .setContentTitle(name)
            .setContentText(if (name == number) "Incoming call" else "Incoming call · $number")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(open, true)
            .setContentIntent(open)
            .addAction(0, "Decline", decline)
            .addAction(0, "Answer", openIntent(ctx, true, 2))
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java).notify(ID_CALL, n)
        } catch (_: Throwable) {
        }
    }

    fun cancelIncoming(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID_CALL)
    }
}
