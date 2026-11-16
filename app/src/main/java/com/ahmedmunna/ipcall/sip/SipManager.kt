package com.ahmedmunna.ipcall.sip

import android.app.Application
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.PowerManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.ahmedmunna.ipcall.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.Reason
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

enum class RegStatus { Idle, Connecting, Registered, Failed }
enum class CallPhase { Incoming, Dialing, Ringing, Active, Held, Ended }

data class SipConfig(
    val displayName: String,
    val username: String,
    val authUser: String,
    val password: String,
    val domain: String,
    val proxy: String,
    val port: Int,
    val transport: String
)

data class CallUi(
    val phase: CallPhase,
    val name: String,
    val number: String,
    val incoming: Boolean,
    val connectedAtMs: Long = 0L,
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val note: String = ""
)

data class LogItem(
    val number: String,
    val name: String,
    val incoming: Boolean,
    val missed: Boolean,
    val startSec: Long,
    val durationSec: Int
)

class SipManager(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var core: Core
        private set

    val regStatus = MutableStateFlow(RegStatus.Idle)
    val regMessage = MutableStateFlow("")
    val loggedIn = MutableStateFlow(false)
    val loginBusy = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val account = MutableStateFlow("")
    val call = MutableStateFlow<CallUi?>(null)
    val logs = MutableStateFlow<List<LogItem>>(emptyList())

    private var loginAttempt = false
    private var timeoutJob: Job? = null
    private var endJob: Job? = null
    private var ringtone: Ringtone? = null
    private var ringtoneKey: String? = null
    private var proxLock: PowerManager.WakeLock? = null

    // ------------------------------------------------------------------ init

    fun init() {
        val factory = Factory.instance()
        factory.setDebugMode(false, "IPCALL")
        core = factory.createCore(null, null, app)
        core.addListener(listener)
        core.start()
        // turn the display off when the phone is held to the ear during a call
        scope.launch { call.collect { updateProximity() } }

        val acc = core.defaultAccount ?: core.accountList.firstOrNull()
        if (acc != null) {
            loggedIn.value = true
            val id = acc.params.identityAddress
            account.value = if (id != null) "${id.username}@${id.domain}" else ""
            regStatus.value = mapReg(acc.state)
            startKeepAlive()
        }
        refreshLogs()
    }

    // ------------------------------------------------------------------ account

    fun login(cfg: SipConfig) {
        if (loginBusy.value) return
        loginError.value = null
        clearAccounts()
        try {
            val f = Factory.instance()
            val auth = f.createAuthInfo(
                cfg.username,
                cfg.authUser.ifBlank { null },
                cfg.password,
                null,
                null,
                cfg.domain
            )
            val params = core.createAccountParams()
            val identity = f.createAddress("sip:${cfg.username}@${cfg.domain}")
            val server = f.createAddress("sip:${cfg.proxy.ifBlank { cfg.domain }}")
            if (identity == null || server == null) {
                loginError.value = "Invalid username or server address"
                return
            }
            if (cfg.displayName.isNotBlank()) identity.displayName = cfg.displayName
            server.transport = when (cfg.transport) {
                "TCP" -> TransportType.Tcp
                "TLS" -> TransportType.Tls
                else -> TransportType.Udp
            }
            if (cfg.port > 0) server.port = cfg.port
            params.identityAddress = identity
            params.serverAddress = server
            params.isRegisterEnabled = true

            val acc = core.createAccount(params)
            loginAttempt = true
            loginBusy.value = true
            core.addAuthInfo(auth)
            core.addAccount(acc)
            core.defaultAccount = acc

            Prefs.username = cfg.username
            Prefs.domain = cfg.domain
            Prefs.displayName = cfg.displayName
            Prefs.authUser = cfg.authUser
            Prefs.proxy = cfg.proxy
            Prefs.port = cfg.port
            Prefs.transport = cfg.transport
            account.value = "${cfg.username}@${cfg.domain}"

            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(25_000)
                if (loginAttempt) failLogin("Connection timed out. Check server address and network.")
            }
        } catch (t: Throwable) {
            loginAttempt = false
            loginBusy.value = false
            loginError.value = t.message ?: "Login failed"
        }
    }

    private fun failLogin(msg: String) {
        loginAttempt = false
        loginBusy.value = false
        timeoutJob?.cancel()
        loginError.value = msg
        clearAccounts()
        regStatus.value = RegStatus.Idle
    }

    fun logout() {
        timeoutJob?.cancel()
        loginAttempt = false
        loginBusy.value = false
        clearAccounts()
        regStatus.value = RegStatus.Idle
        account.value = ""
        loggedIn.value = false
        app.stopService(Intent(app, CallService::class.java))
    }

    private fun clearAccounts() {
        for (a in core.accountList) core.removeAccount(a)
        core.clearAllAuthInfo()
    }

    fun startKeepAlive() {
        if (!Prefs.keepAlive) return
        try {
            ContextCompat.startForegroundService(app, Intent(app, CallService::class.java))
        } catch (_: Throwable) {
        }
    }

    fun setKeepAlive(on: Boolean) {
        Prefs.keepAlive = on
        if (on && loggedIn.value) startKeepAlive()
        else app.stopService(Intent(app, CallService::class.java))
    }

    private fun mapReg(s: RegistrationState?): RegStatus = when (s) {
        RegistrationState.Ok -> RegStatus.Registered
        RegistrationState.Progress -> RegStatus.Connecting
        RegistrationState.Failed -> RegStatus.Failed
        else -> RegStatus.Idle
    }

    // ------------------------------------------------------------------ calls

    fun placeCall(raw: String) {
        val n = raw.trim()
        if (n.isEmpty() || core.callsNb > 0) return
        val domain = Prefs.domain.ifBlank { account.value.substringAfter('@', "") }
        val safe = n.replace("#", "%23")
        val uri = if (safe.contains('@')) "sip:$safe" else "sip:$safe@$domain"
        val addr = Factory.instance().createAddress(uri) ?: return
        core.isMicEnabled = true
        core.inviteAddress(addr)
    }

    private fun incomingCall(): Call? = core.calls.firstOrNull {
        it.state == Call.State.IncomingReceived || it.state == Call.State.IncomingEarlyMedia
    }

    fun answer() {
        incomingCall()?.accept()
    }

    /** Hang up the active call, or decline it if it is still ringing. */
    fun hangup() {
        val inc = incomingCall()
        if (inc != null) {
            inc.decline(Reason.Declined)
            return
        }
        (core.currentCall ?: core.calls.firstOrNull())?.terminate()
    }

    fun toggleMute() {
        core.isMicEnabled = !core.isMicEnabled
        publishFlags()
    }

    fun toggleSpeaker() {
        try {
            val wantSpeaker = core.outputAudioDevice?.type != AudioDevice.Type.Speaker
            val wanted = if (wantSpeaker) AudioDevice.Type.Speaker else AudioDevice.Type.Earpiece
            val target = core.audioDevices.firstOrNull { it.type == wanted }
            if (target != null) {
                core.outputAudioDevice = target
                core.currentCall?.outputAudioDevice = target
            }
        } catch (_: Throwable) {
        }
        publishFlags()
    }

    fun toggleHold() {
        val c = core.currentCall ?: core.calls.firstOrNull() ?: return
        if (c.state == Call.State.Paused || c.state == Call.State.Pausing) c.resume() else c.pause()
    }

    fun sendDtmf(ch: Char) {
        try {
            (core.currentCall ?: core.calls.firstOrNull())?.sendDtmf(ch)
        } catch (_: Throwable) {
        }
    }

    private fun publishFlags() {
        val cur = call.value ?: return
        call.value = cur.copy(
            muted = !core.isMicEnabled,
            speaker = core.outputAudioDevice?.type == AudioDevice.Type.Speaker
        )
    }

    fun refreshLogs() {
        try {
            logs.value = core.callLogs.take(100).map { l ->
                val a = l.remoteAddress
                val num = a.username ?: a.asStringUriOnly()
                LogItem(
                    number = num,
                    name = a.displayName?.takeIf { it.isNotBlank() } ?: num,
                    incoming = l.dir == Call.Dir.Incoming,
                    missed = l.status == Call.Status.Missed,
                    startSec = l.startDate,
                    durationSec = l.duration
                )
            }
        } catch (_: Throwable) {
        }
    }

    fun clearLogs() {
        core.clearCallLogs()
        refreshLogs()
    }

    // ------------------------------------------------------------------ ringing

    private fun updateProximity() {
        try {
            val c = call.value
            val want = c != null && c.phase != CallPhase.Incoming && c.phase != CallPhase.Ended && !c.speaker
            val pm = app.getSystemService(PowerManager::class.java) ?: return
            if (want) {
                if (proxLock == null && pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                    proxLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "ipcall:proximity")
                        .also { it.setReferenceCounted(false) }
                }
                if (proxLock?.isHeld == false) proxLock?.acquire(4 * 60 * 60 * 1000L)
            } else if (proxLock?.isHeld == true) {
                proxLock?.release()
            }
        } catch (_: Throwable) {
        }
    }

    private fun startRinging() {
        try {
            val pref = Prefs.ringtone
            if (pref != "silent") {
                if (ringtone == null || ringtoneKey != pref) {
                    ringtone?.stop()
                    val def = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    val uri = if (pref.isEmpty()) def else Uri.parse(pref)
                    ringtone = (RingtoneManager.getRingtone(app, uri) ?: RingtoneManager.getRingtone(app, def))?.also {
                        if (Build.VERSION.SDK_INT >= 28) it.isLooping = true
                    }
                    ringtoneKey = pref
                }
                ringtone?.takeIf { !it.isPlaying }?.play()
            }
            app.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
        } catch (_: Throwable) {
        }
    }

    private fun stopRinging() {
        try {
            ringtone?.stop()
            app.getSystemService(Vibrator::class.java)?.cancel()
        } catch (_: Throwable) {
        }
    }

    // ------------------------------------------------------------------ listener

    private val listener = object : CoreListenerStub() {

        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            val st = mapReg(state)
            regStatus.value = st
            regMessage.value = message
            if (!loginAttempt) return
            when (st) {
                RegStatus.Registered -> {
                    loginAttempt = false
                    loginBusy.value = false
                    timeoutJob?.cancel()
                    loginError.value = null
                    loggedIn.value = true
                    startKeepAlive()
                }
                RegStatus.Failed -> scope.launch {
                    failLogin(
                        when {
                            message.contains("Unauthorized", true) || message.contains("Forbidden", true) ->
                                "Wrong username or password ($message)"
                            message.isBlank() -> "Registration failed"
                            else -> "Registration failed: $message"
                        }
                    )
                }
                else -> {}
            }
        }

        override fun onCallStateChanged(core: Core, c: Call, state: Call.State, message: String) {
            val remote = c.remoteAddress
            val number = remote.username ?: remote.asStringUriOnly()
            val name = remote.displayName?.takeIf { it.isNotBlank() } ?: number
            val muted = !core.isMicEnabled
            val speaker = core.outputAudioDevice?.type == AudioDevice.Type.Speaker
            val prev = call.value

            when (state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                    endJob?.cancel()
                    call.value = CallUi(CallPhase.Incoming, name, number, true, muted = muted, speaker = speaker)
                    startRinging()
                    Notifier.showIncoming(app, name, number)
                }
                Call.State.OutgoingInit, Call.State.OutgoingProgress -> {
                    endJob?.cancel()
                    call.value = CallUi(CallPhase.Dialing, name, number, false, muted = muted, speaker = speaker)
                }
                Call.State.OutgoingRinging, Call.State.OutgoingEarlyMedia -> {
                    call.value = CallUi(CallPhase.Ringing, name, number, false, muted = muted, speaker = speaker)
                }
                Call.State.Connected, Call.State.StreamsRunning -> {
                    stopRinging()
                    Notifier.cancelIncoming(app)
                    val started = if (prev != null && prev.connectedAtMs > 0L) prev.connectedAtMs
                    else SystemClock.elapsedRealtime() - c.duration * 1000L
                    call.value = CallUi(
                        CallPhase.Active, name, number, prev?.incoming ?: false,
                        connectedAtMs = started, muted = muted, speaker = speaker
                    )
                }
                Call.State.Pausing, Call.State.Paused, Call.State.PausedByRemote -> {
                    call.value = (prev ?: CallUi(CallPhase.Held, name, number, false))
                        .copy(phase = CallPhase.Held, muted = muted, speaker = speaker)
                }
                Call.State.End, Call.State.Error -> {
                    stopRinging()
                    Notifier.cancelIncoming(app)
                    val note = if (state == Call.State.Error) message else ""
                    call.value = (prev ?: CallUi(CallPhase.Ended, name, number, false))
                        .copy(phase = CallPhase.Ended, note = note)
                    this@SipManager.core.isMicEnabled = true
                    refreshLogs()
                    endJob?.cancel()
                    endJob = scope.launch {
                        delay(1400)
                        if (this@SipManager.core.callsNb == 0) call.value = null
                    }
                }
                Call.State.Released -> {
                    refreshLogs()
                    if (core.callsNb == 0 && call.value?.phase != CallPhase.Ended) call.value = null
                }
                else -> {}
            }
        }
    }
}
