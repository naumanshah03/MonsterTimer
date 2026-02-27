package com.monstertimer.app

import android.app.Application
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class MonsterTimerApp : Application() {

    companion object {
        private const val PREFS_FILE = "monster_timer_prefs"
        
        private lateinit var instance: MonsterTimerApp
        
        fun getSecurePrefs(context: Context) = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}

/**
 * Data class representing app settings
 */
data class AppSettings(
    val timerMinutes: Int = 10,
    val parentPin: String = "1234",
    val monsterPaths: List<String> = emptyList(),
    val isManualTimer: Boolean = false,
    val eulaAccepted: Boolean = false,
    val monitoringEnabled: Boolean = true
) {
    companion object {
        private const val KEY_TIMER_MINUTES = "timer_minutes"
        private const val KEY_PIN = "parent_pin"
        private const val KEY_MONSTER_PATHS = "monster_paths"
        private const val KEY_IS_MANUAL_TIMER = "is_manual_timer"
        private const val KEY_EULA_ACCEPTED = "eula_accepted"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        
        fun load(context: Context): AppSettings {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            return AppSettings(
                timerMinutes = prefs.getInt(KEY_TIMER_MINUTES, 10),
                parentPin = prefs.getString(KEY_PIN, "1234") ?: "1234",
                monsterPaths = prefs.getString(KEY_MONSTER_PATHS, "")
                    ?.split(":::")
                    ?.filter { it.isNotBlank() }
                    ?: emptyList(),
                isManualTimer = prefs.getBoolean(KEY_IS_MANUAL_TIMER, false),
                eulaAccepted = prefs.getBoolean(KEY_EULA_ACCEPTED, false),
                monitoringEnabled = prefs.getBoolean(KEY_MONITORING_ENABLED, true)
            )
        }
        
        fun save(context: Context, settings: AppSettings) {
            val prefs = MonsterTimerApp.getSecurePrefs(context)
            prefs.edit()
                .putInt(KEY_TIMER_MINUTES, settings.timerMinutes)
                .putString(KEY_PIN, settings.parentPin)
                .putString(KEY_MONSTER_PATHS, settings.monsterPaths.joinToString(":::"))
                .putBoolean(KEY_IS_MANUAL_TIMER, settings.isManualTimer)
                .putBoolean(KEY_EULA_ACCEPTED, settings.eulaAccepted)
                .putBoolean(KEY_MONITORING_ENABLED, settings.monitoringEnabled)
                .apply()
        }
    }
}
