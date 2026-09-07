package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveUserId(userId: String) {
        prefs.edit().putString("active_user_id", userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString("active_user_id", null)
    }

    fun clearSession() {
        prefs.edit().remove("active_user_id").apply()
    }
}
