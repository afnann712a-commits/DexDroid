package com.example.dexdroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.view.Display

/**
 * Runs in the background and watches DisplayManager for a new external
 * display (HDMI/USB-C dongle, wireless cast target, etc). When one shows
 * up, it puts the DesktopPresentation on it; when it disappears, it tears
 * the presentation down.
 */
class DisplayMonitorService : Service() {

    private lateinit var displayManager: DisplayManager
    private var presentation: DesktopPresentation? = null

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            maybeShowOn(displayManager.getDisplay(displayId))
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (presentation?.display?.displayId == displayId) {
                presentation?.dismiss()
                presentation = null
            }
        }

        override fun onDisplayChanged(displayId: Int) {
            // No-op: we only care about add/remove for this simple version.
        }
    }

    override fun onCreate() {
        super.onCreate()
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        startForeground(NOTIFICATION_ID, buildNotification())
        displayManager.registerDisplayListener(listener, null)

        // Catch a display that was already connected before the service started.
        displayManager.displays
            .firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
            ?.let { maybeShowOn(it) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        displayManager.unregisterDisplayListener(listener)
        presentation?.dismiss()
        presentation = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun maybeShowOn(display: Display?) {
        if (display == null || display.displayId == Display.DEFAULT_DISPLAY) return
        if (presentation != null) return
        presentation = DesktopPresentation(applicationContext, display).also { it.show() }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Desktop mode",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("DexDroid")
            .setContentText("Watching for an external display")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "dexdroid_monitor"
        private const val NOTIFICATION_ID = 42
    }
}
