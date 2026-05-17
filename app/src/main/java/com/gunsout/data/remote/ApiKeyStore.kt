package com.gunsout.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Stores the optional CalorieNinjas API key in encrypted shared preferences. */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getCalorieNinjasKey(): String? = prefs.getString(KEY_CN, null)?.takeIf { it.isNotBlank() }

    fun setCalorieNinjasKey(key: String?) {
        prefs.edit().apply {
            if (key.isNullOrBlank()) remove(KEY_CN) else putString(KEY_CN, key.trim())
            apply()
        }
    }

    companion object {
        private const val KEY_CN = "calorie_ninjas_key"
    }
}
