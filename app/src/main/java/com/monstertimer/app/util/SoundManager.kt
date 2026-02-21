package com.monstertimer.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Handles sound effects and vibration for the monster overlay
 */
object SoundManager {
    private const val TAG = "SoundManager"
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Play the monster scare sound
     */
    fun playMonsterSound(context: Context) {
        try {
            // Release any existing player
            releaseMediaPlayer()
            
            // Try to play the custom monster sound, fallback to alarm
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                
                // Use the default alarm sound as our "scary" sound
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                
                setDataSource(context, alarmUri)
                isLooping = false
                prepare()
                start()
            }
            
            // Also vibrate for extra effect
            vibrate(context, longArrayOf(0, 500, 200, 500, 200, 500))
            
            Log.d(TAG, "Monster sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play monster sound", e)
        }
    }
    
    /**
     * Play warning beep (for 2-min and 1-min warnings)
     */
    fun playWarningBeep(context: Context) {
        try {
            releaseMediaPlayer()
            
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, notificationUri)
                prepare()
                start()
            }
            
            // Short vibration for warning
            vibrate(context, longArrayOf(0, 200, 100, 200))
            
            Log.d(TAG, "Warning beep played")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play warning beep", e)
        }
    }
    
    /**
     * Play positive reward sound
     */
    fun playRewardSound(context: Context) {
        try {
            releaseMediaPlayer()
            
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, notificationUri)
                prepare()
                start()
            }
            
            Log.d(TAG, "Reward sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play reward sound", e)
        }
    }
    
    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }
    
    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
