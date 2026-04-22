package com.example.locus.utils

import android.util.Base64
import org.json.JSONObject

fun decodeUserIdFromToken(token: String): Int? {
    return try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING))
        JSONObject(payload).getInt("user_id")
    } catch (e: Exception) {
        null
    }
}