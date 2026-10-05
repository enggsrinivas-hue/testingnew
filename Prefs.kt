package com.smsbuddy.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Settings are stored encrypted on the device (the app password never leaves it except to Gmail). */
object Prefs {
    private fun sp(c: Context): SharedPreferences {
        val key = MasterKey.Builder(c.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            c.applicationContext, "secure_prefs", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun gmail(c: Context) = sp(c).getString("gmail", "") ?: ""
    fun password(c: Context) = sp(c).getString("password", "") ?: ""
    fun to(c: Context) = sp(c).getString("to", "") ?: ""
    fun isEnabled(c: Context) = sp(c).getBoolean("enabled", false)
    fun count(c: Context) = sp(c).getInt("count", 0)

    fun isConfigured(c: Context) = gmail(c).isNotBlank() && password(c).isNotBlank() && to(c).isNotBlank()

    fun save(c: Context, gmail: String, password: String, to: String) {
        sp(c).edit().putString("gmail", gmail).putString("password", password).putString("to", to).apply()
    }

    fun setEnabled(c: Context, on: Boolean) = sp(c).edit().putBoolean("enabled", on).apply()

    @Synchronized
    fun increment(c: Context) {
        val s = sp(c)
        s.edit().putInt("count", s.getInt("count", 0) + 1).apply()
    }
}
