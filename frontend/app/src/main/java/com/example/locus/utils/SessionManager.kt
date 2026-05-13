package com.example.locus.utils

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("locus_session", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString("token", null)
        set(value) {
            if (value != null) prefs.edit().putString("token", value).apply()
            else prefs.edit().remove("token").apply()
        }

    var username: String?
        get() = prefs.getString("username", null)
        set(value) {
            if (value != null) prefs.edit().putString("username", value).apply()
            else prefs.edit().remove("username").apply()
        }

    fun clear() = prefs.edit().clear().apply()
}
