package com.ahmedmunna.ipcall

import android.app.Application
import com.ahmedmunna.ipcall.data.Prefs
import com.ahmedmunna.ipcall.sip.Notifier
import com.ahmedmunna.ipcall.sip.SipManager

class IpCallApp : Application() {
    companion object {
        lateinit var sip: SipManager
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Notifier.createChannels(this)
        sip = SipManager(this)
        sip.init()
    }
}
