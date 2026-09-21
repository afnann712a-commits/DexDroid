package com.example.dexdroid

import android.app.ActivityOptions
import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a DeX-like desktop on an external display: a taskbar with a clock,
 * and a grid of installed apps that launch straight onto that display.
 *
 * Apps launched this way only appear as resizable floating/desktop windows if
 * the device has freeform window support enabled (Settings > Developer options
 * > "Force activities to be resizable" / "Enable freeform windows", or an OEM
 * build that exposes it). Without that, apps still launch on the external
 * screen but full-screen rather than in a movable window - that limitation
 * comes from stock Android, not this app.
 */
class DesktopPresentation(
    outerContext: Context,
    display: Display
) : Presentation(outerContext, display) {

    private lateinit var clock: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("EEE, MMM d   HH:mm", Locale.getDefault())

    private val tickRunnable = object : Runnable {
        override fun run() {
            clock.text = clockFormat.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.presentation_desktop)

        clock = findViewById(R.id.taskbarClock)

        val grid: RecyclerView = findViewById(R.id.desktopAppGrid)
        grid.layoutManager = GridLayoutManager(context, 6)
        grid.adapter = AppGridAdapter(loadLaunchableApps()) { app ->
            launchOnThisDisplay(app.packageName)
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(tickRunnable)
    }

    override fun onStop() {
        handler.removeCallbacks(tickRunnable)
        super.onStop()
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolved
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun launchOnThisDisplay(packageName: String) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val options = ActivityOptions.makeBasic()
        options.launchDisplayId = display.displayId

        try {
            context.startActivity(launchIntent, options.toBundle())
        } catch (e: SecurityException) {
            // Some OEMs restrict launching on a specific display without
            // additional system permissions - fall back to a normal launch.
            context.startActivity(launchIntent)
        }
    }
}
