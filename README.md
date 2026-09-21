# DexDroid

A minimal desktop-mode app for Android, inspired by Samsung DeX. It watches
for an external display (HDMI/USB-C adapter or wireless cast) and shows a
desktop-style UI — taskbar with a clock, and an app grid — on that screen,
launching apps directly onto it.

## What it does
- Detects when an external display connects/disconnects (`DisplayMonitorService`)
- Shows a desktop UI on that display (`DesktopPresentation`), built with
  Android's `Presentation` API (the same one Samsung DeX itself is built on
  under the hood)
- Lets you tap an app icon to launch that app onto the external display via
  `ActivityOptions.setLaunchDisplayId`

## What it can't do (platform limits, not app limits)
- **Resizable floating windows for arbitrary apps**: stock Android only does
  this if freeform window mode is enabled. On most phones that means
  Settings → System → Developer options → "Force activities to be
  resizable" / "Enable freeform windows" (naming varies by OEM, and some
  manufacturers hide it entirely). Without it, apps launch full-screen on
  the external display rather than in a movable window.
- **True dual independent screens** (different content on phone vs.
  external display with full independent input) works out of the box with
  this app's `Presentation` approach, but window management on the desktop
  side is intentionally basic here — no drag/resize/snap. That layer is
  what Samsung had to build custom OS/window-manager support for.
- **Per-app desktop-optimized layouts** — that requires each app to opt in
  (large-screen/tablet layouts), same as on Samsung DeX.

## Building it
1. Open the `DexDroid/` folder in Android Studio (Hedgehog or newer).
2. Let Gradle sync (it will pull AndroidX + Material dependencies).
3. Run on a **physical device** running Android 8.0 (API 26) or higher —
   the emulator doesn't simulate external display connection well.
4. Grant the notification permission if prompted (needed for the
   foreground service on Android 13+).

## Testing without real DeX hardware
- **Cheapest**: a $10–15 USB-C-to-HDMI adapter into any monitor/TV. Most
  phones with USB-C DisplayPort Alt Mode support this.
- **Wireless**: enable "Cast" from the phone's quick settings while this
  app is running with desktop mode enabled — the presentation will appear
  on the Chromecast/smart TV/cast receiver.
- To actually test resizable freeform windows, enable Developer Options →
  freeform window support first; not all chipsets/OEM skins allow it.

## Where to go next
- Add drag-to-resize/move for launched windows (requires tracking each
  launched activity's task and using `ActivityTaskManager` reflection or
  a rooted approach — not exposed as clean public API)
- Persist a "recent/pinned apps" list in the taskbar
- Add a system tray area (battery, wifi, notifications) by reading
  `BatteryManager` / `ConnectivityManager` state
- Support drag-and-drop between phone and desktop windows using Android's
  `View.startDragAndDrop` with `ClipData`
