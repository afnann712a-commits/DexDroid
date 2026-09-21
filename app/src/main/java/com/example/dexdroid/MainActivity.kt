package com.example.dexdroid

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var displayManager: DisplayManager

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshStatus()
        override fun onDisplayRemoved(displayId: Int) = refreshStatus()
        override fun onDisplayChanged(displayId: Int) = refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.enableButton).setOnClickListener {
            val intent = Intent(this, DisplayMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            refreshStatus()
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        displayManager.registerDisplayListener(listener, null)
        refreshStatus()
    }

    override fun onPause() {
        displayManager.unregisterDisplayListener(listener)
        super.onPause()
    }

    private fun refreshStatus() {
        val external = displayManager.displays.any { it.displayId != Display.DEFAULT_DISPLAY }
        statusText.text = if (external) {
            "External display detected.\nDesktop mode should be showing there once you tap \"Enable Desktop Mode\"."
        } else {
            "No external display connected yet.\nConnect over HDMI/USB-C, or start a wireless cast, then tap the button below."
        }
    }
}
