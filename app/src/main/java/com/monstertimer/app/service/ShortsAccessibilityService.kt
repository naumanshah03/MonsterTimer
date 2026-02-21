package com.monstertimer.app.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.monstertimer.app.AppSettings
import com.monstertimer.app.MainActivity
import com.monstertimer.app.R
import com.monstertimer.app.data.PersistentTimerState
import com.monstertimer.app.data.UsageStats
import com.monstertimer.app.util.SoundManager
import java.util.LinkedList

class ShortsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ShortsAccessibility"
        private const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val WARNING_CHANNEL_ID = "monster_warning_channel"
        private const val TIMER_STATUS_CHANNEL_ID = "monster_timer_status_channel"
        private const val WARNING_NOTIFICATION_ID = 2001
        private const val TIMER_STATUS_NOTIFICATION_ID = 2002

        private val SHORTS_INDICATORS = listOf(
            "Shorts",
            "shorts_pivot_button",
            "reel_player_page_container",
            "shorts_video_view",
            "ShortsFragment"
        )

        private const val TWO_MINUTE_WARNING = 2 * 60 * 1000L
        private const val ONE_MINUTE_WARNING = 1 * 60 * 1000L

        var isServiceRunning = false
            private set

        var isParentBypassed = false
    }

    private var countdownTimer: CountDownTimer? = null
    private var isTimerRunning = false
    private var isShortsDetected = false
    private var lastCheckedTimestamp = 0L
    private var sessionStartTime = 0L
    private var currentRemainingMillis = 0L

    private var twoMinuteWarningShown = false
    private var oneMinuteWarningShown = false

    override fun onCreate() {
        super.onCreate()
        createWarningNotificationChannel()
        createTimerStatusNotificationChannel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.d(TAG, "Accessibility Service connected")
        restoreTimerState()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (isParentBypassed) return

        if (event.packageName?.toString() != YOUTUBE_PACKAGE) {
            if (isShortsDetected) {
                Log.d(TAG, "Left YouTube - Saving timer state")
                saveTimerState()
                pauseTimer()
                isShortsDetected = false

                if (sessionStartTime > 0) {
                    val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000
                    updateUsageStats(sessionDuration)
                    sessionStartTime = 0
                }
            }
            hideMonsterOverlay()
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastCheckedTimestamp < 500) return
        lastCheckedTimestamp = now

        val rootNode = rootInActiveWindow ?: return
        val shortsDetected = detectShorts(rootNode)
        // No need to recycle rootNode here, detectShorts handles all recycling

        if (shortsDetected && !isShortsDetected) {
            Log.d(TAG, "Shorts DETECTED - Starting/Resuming timer")
            isShortsDetected = true
            sessionStartTime = System.currentTimeMillis()
            startOrResumeTimer()
        } else if (!shortsDetected && isShortsDetected) {
            Log.d(TAG, "Left Shorts section - Pausing timer")
            saveTimerState()
            pauseTimer()
            isShortsDetected = false

            if (sessionStartTime > 0) {
                val sessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000
                updateUsageStats(sessionDuration)
                sessionStartTime = 0
            }
            hideMonsterOverlay()
        }
    }

    private fun detectShorts(rootNode: AccessibilityNodeInfo): Boolean {
        val queue = LinkedList<AccessibilityNodeInfo>()
        val visitedNodes = mutableListOf<AccessibilityNodeInfo>()
        var shortsFound = false

        try {
            queue.add(rootNode)
            visitedNodes.add(rootNode)

            while (queue.isNotEmpty()) {
                val node = queue.poll() ?: continue

                val viewId = node.viewIdResourceName ?: ""
                if (SHORTS_INDICATORS.any { viewId.contains(it, ignoreCase = true) }) {
                    shortsFound = true
                    break
                }
                val contentDesc = node.contentDescription?.toString() ?: ""
                if (contentDesc.contains("Shorts", ignoreCase = true)) {
                    shortsFound = true
                    break
                }
                val text = node.text?.toString() ?: ""
                if (text == "Shorts") {
                    shortsFound = true
                    break
                }

                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        visitedNodes.add(child)
                        queue.add(child)
                    }
                }
            }
        } finally {
            // Safely recycle all nodes that were touched during the traversal.
            for (node in visitedNodes) {
                node.recycle()
            }
        }
        return shortsFound
    }

    private fun restoreTimerState() {
        val state = PersistentTimerState.load(this) ?: return
        currentRemainingMillis = state.getAdjustedRemainingMillis()
        Log.d(TAG, "Restored timer state: ${currentRemainingMillis / 1000}s remaining")

        if (currentRemainingMillis <= 0 && state.isActive) {
            Log.d(TAG, "Timer expired while app was closed - showing monster")
            showMonsterOverlay()
        }
    }

    private fun startOrResumeTimer() {
        if (isTimerRunning) return

        val savedState = PersistentTimerState.load(this)
        val adjustedRemaining = savedState?.getAdjustedRemainingMillis() ?: 0L

        if (savedState != null && adjustedRemaining <= 0 && savedState.isActive) {
            Log.d(TAG, "Timer already expired. Showing monster overlay.")
            showMonsterOverlay()
            // Clear the expired state so re-entry gets a fresh timer
            PersistentTimerState.clear(this)
            return
        }

        currentRemainingMillis = if (adjustedRemaining > 0) {
            adjustedRemaining
        } else {
            val settings = AppSettings.load(this)
            settings.timerMinutes * 60 * 1000L
        }

        twoMinuteWarningShown = false
        oneMinuteWarningShown = false
        showTimerStatusNotification(currentRemainingMillis)

        countdownTimer = object : CountDownTimer(currentRemainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                currentRemainingMillis = millisUntilFinished
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                Log.d(TAG, "Timer: $minutes:${String.format("%02d", seconds)} remaining")

                showTimerStatusNotification(millisUntilFinished)
                checkAndShowWarnings(millisUntilFinished)

                if (seconds.toInt() % 10 == 0) {
                    saveTimerState()
                }
            }

            override fun onFinish() {
                Log.d(TAG, "Timer EXPIRED - Showing monster!")
                isTimerRunning = false
                currentRemainingMillis = 0
                hideTimerStatusNotification()
                saveTimerState(isFinished = true)

                val stats = UsageStats.getToday(this@ShortsAccessibilityService).incrementMonsterShown()
                UsageStats.save(this@ShortsAccessibilityService, stats)

                showMonsterOverlay()
            }
        }.start()

        isTimerRunning = true
    }

    private fun checkAndShowWarnings(millisRemaining: Long) {
        if (!twoMinuteWarningShown && millisRemaining <= TWO_MINUTE_WARNING && millisRemaining > ONE_MINUTE_WARNING) {
            twoMinuteWarningShown = true
            showWarningNotification("2 minutes left!")
            SoundManager.playWarningBeep(this)
            Log.d(TAG, "2-minute warning shown")
        }

        if (!oneMinuteWarningShown && millisRemaining <= ONE_MINUTE_WARNING) {
            oneMinuteWarningShown = true
            showWarningNotification("1 minute left! The monster is coming! 👹")
            SoundManager.playWarningBeep(this)
            Log.d(TAG, "1-minute warning shown")
        }
    }

    private fun showWarningNotification(message: String) {
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monster)
            .setContentTitle("Monster Timer Warning")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(WARNING_NOTIFICATION_ID, notification)
    }

    private fun createWarningNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(WARNING_CHANNEL_ID, "Timer Warnings", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Warnings before the monster appears"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createTimerStatusNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(TIMER_STATUS_CHANNEL_ID, "Timer Status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the remaining time for the monster timer"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showTimerStatusNotification(millisRemaining: Long) {
        val contentText = if (millisRemaining < TWO_MINUTE_WARNING) {
            val seconds = (millisRemaining / 1000) + 1
            "Time remaining: $seconds seconds"
        } else {
            val minutes = (millisRemaining / 60000) + 1
            "Time remaining: $minutes minutes"
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, TIMER_STATUS_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monster)
            .setContentTitle("Monster Timer Active")
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(TIMER_STATUS_NOTIFICATION_ID, notification)
    }

    private fun hideTimerStatusNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(TIMER_STATUS_NOTIFICATION_ID)
    }

    private fun saveTimerState(isFinished: Boolean = false) {
        val state = if (isFinished) {
            PersistentTimerState(0, System.currentTimeMillis(), isActive = true)
        } else {
            PersistentTimerState(currentRemainingMillis, System.currentTimeMillis(), isTimerRunning)
        }
        PersistentTimerState.save(this, state)
    }

    private fun pauseTimer() {
        countdownTimer?.cancel()
        isTimerRunning = false
        hideTimerStatusNotification()
    }

    private fun cancelTimer() {
        countdownTimer?.cancel()
        countdownTimer = null
        isTimerRunning = false
        currentRemainingMillis = 0
        PersistentTimerState.clear(this)
        hideTimerStatusNotification()
    }

    private fun updateUsageStats(sessionSeconds: Long) {
        val stats = UsageStats.getToday(this).addWatchTime(sessionSeconds)
        UsageStats.save(this, stats)
        Log.d(TAG, "Updated usage: ${stats.formattedWatchTime} total today")
    }

    private fun showMonsterOverlay() {
        val intent = Intent(this, MonsterOverlayService::class.java)
        startForegroundService(intent)
    }

    private fun hideMonsterOverlay() {
        stopService(Intent(this, MonsterOverlayService::class.java))
    }

    fun onStoppedEarly() {
        val stats = UsageStats.getToday(this).incrementStoppedEarly()
        UsageStats.save(this, stats)
        cancelTimer()
        SoundManager.playRewardSound(this)
        Log.d(TAG, "Child stopped early - reward tracked!")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
        saveTimerState()
        cancelTimer()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        saveTimerState()
        cancelTimer()
        SoundManager.releaseMediaPlayer()
        Log.d(TAG, "Service destroyed")
    }
}
