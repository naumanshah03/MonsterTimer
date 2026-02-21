package com.monstertimer.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.monstertimer.app.AppSettings
import com.monstertimer.app.MainActivity
import com.monstertimer.app.R
import com.monstertimer.app.util.SoundManager
import java.io.File

/**
 * Service that displays a fullscreen "Monster" overlay when the timer expires.
 * Enhanced with:
 * - Sound effects
 * - Reward messages for good behavior
 */
class MonsterOverlayService : Service() {

    companion object {
        private const val TAG = "MonsterOverlay"
        private const val CHANNEL_ID = "monster_timer_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        showOverlay()
        
        // Play scary sound!
        SoundManager.playMonsterSound(this)
        
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monster Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Parental control overlay notification"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = "Monster Timer Active"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Screen time limit reached")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = createOverlayView()
        
        try {
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay displayed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay", e)
            stopSelf()
        }
    }

    private fun createOverlayView(): View {
        val settings = AppSettings.load(this)
        
        // Root container
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resources.getColor(R.color.monster_overlay_bg, null))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Title text
        val titleText = TextView(this).apply {
            text = getString(R.string.times_up)
            textSize = 36f
            setTextColor(resources.getColor(R.color.white, null))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        root.addView(titleText)

        // Monster image or video
        val monsterPaths = settings.monsterPaths
        if (monsterPaths.isNotEmpty()) {
            val randomPath = monsterPaths.random()
            val file = File(randomPath)
            
            if (randomPath.endsWith(".mp4", true) || randomPath.endsWith(".webm", true)) {
                // Video
                val videoView = VideoView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 900
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    setVideoURI(Uri.fromFile(file))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
                root.addView(videoView)
            } else {
                // Image
                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 900
                    ).apply {
                        gravity = Gravity.CENTER
                    }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                Glide.with(this).load(file).into(imageView)
                root.addView(imageView)
            }
        } else {
            // Default scary emoji with pulsing effect
            val emojiView = TextView(this).apply {
                text = "👹"
                textSize = 150f
                gravity = Gravity.CENTER
            }
            root.addView(emojiView)
        }

        // Scary message
        val messageText = TextView(this).apply {
            text = "Too much Shorts! Time to take a break!"
            textSize = 20f
            setTextColor(resources.getColor(R.color.white, null))
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        root.addView(messageText)

        // Parent bypass button
        val bypassButton = Button(this).apply {
            text = getString(R.string.parent_unlock)
            setBackgroundColor(resources.getColor(R.color.purple_500, null))
            setTextColor(resources.getColor(R.color.white, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 64
            }
        }
        
        bypassButton.setOnClickListener {
            showPinDialog(settings.parentPin)
        }
        
        root.addView(bypassButton)

        return root
    }

    private fun showPinDialog(correctPin: String) {
        // Update overlay to be focusable for PIN input
        val params = overlayView?.layoutParams as? WindowManager.LayoutParams
        params?.flags = params?.flags?.and(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()) ?: 0
        try {
            windowManager?.updateViewLayout(overlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to make overlay focusable", e)
        }

        // Add PIN input container
        val pinContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(resources.getColor(R.color.purple_700, null))
        }

        val pinLabel = TextView(this).apply {
            text = "Enter Parent PIN"
            textSize = 18f
            setTextColor(resources.getColor(R.color.white, null))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        pinContainer.addView(pinLabel)

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val pinInput = EditText(this).apply {
            hint = "4-digit PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or 
                       android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            layoutParams = LinearLayout.LayoutParams(300, LinearLayout.LayoutParams.WRAP_CONTENT)
            setTextColor(resources.getColor(R.color.white, null))
            setHintTextColor(resources.getColor(R.color.purple_200, null))
        }
        inputRow.addView(pinInput)

        val confirmButton = Button(this).apply {
            text = "OK"
            setBackgroundColor(resources.getColor(R.color.teal_700, null))
            setTextColor(resources.getColor(R.color.white, null))
            setOnClickListener {
                if (pinInput.text.toString() == correctPin) {
                    Log.d(TAG, "PIN correct - Parent bypass activated")
                    ShortsAccessibilityService.isParentBypassed = true
                    SoundManager.releaseMediaPlayer()
                    dismissOverlay()
                } else {
                    pinInput.error = "Wrong PIN"
                    pinInput.text.clear()
                }
            }
        }
        inputRow.addView(confirmButton)
        pinContainer.addView(inputRow)

        // Find root and add PIN container
        (overlayView as? LinearLayout)?.addView(pinContainer)
    }

    private fun dismissOverlay() {
        try {
            windowManager?.removeView(overlayView)
            overlayView = null
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.releaseMediaPlayer()
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onDestroy", e)
            }
        }
    }
}
