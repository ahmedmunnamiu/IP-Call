package com.ahmedmunna.ipcall.data

import android.content.Context
import android.content.SharedPreferences

/** Small SharedPreferences wrapper. Passwords are NOT stored here (Linphone keeps them in its own config). */
object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("ipcall_prefs", Context.MODE_PRIVATE)
    }

    fun getString(key: String, def: String): String = sp.getString(key, def) ?: def
    fun putString(key: String, v: String) = sp.edit().putString(key, v).apply()
    fun getInt(key: String, def: Int): Int = sp.getInt(key, def)
    fun putInt(key: String, v: Int) = sp.edit().putInt(key, v).apply()
    fun getFloat(key: String, def: Float): Float = sp.getFloat(key, def)
    fun putFloat(key: String, v: Float) = sp.edit().putFloat(key, v).apply()
    fun getBool(key: String, def: Boolean): Boolean = sp.getBoolean(key, def)
    fun putBool(key: String, v: Boolean) = sp.edit().putBoolean(key, v).apply()

    var username: String
        get() = getString("username", "")
        set(v) = putString("username", v)
    var domain: String
        get() = getString("domain", "")
        set(v) = putString("domain", v)
    var displayName: String
        get() = getString("display_name", "")
        set(v) = putString("display_name", v)
    var authUser: String
        get() = getString("auth_user", "")
        set(v) = putString("auth_user", v)
    var proxy: String
        get() = getString("proxy", "")
        set(v) = putString("proxy", v)
    var port: Int
        get() = getInt("port", 0)
        set(v) = putInt("port", v)
    var transport: String
        get() = getString("transport", "UDP")
        set(v) = putString("transport", v)
    var keepAlive: Boolean
        get() = getBool("keep_alive", true)
        set(v) = putBool("keep_alive", v)
}
