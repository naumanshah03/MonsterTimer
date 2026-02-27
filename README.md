# Monster Timer - Android App

A parental control app that monitors YouTube Shorts usage and displays a "monster" overlay when the timer expires.

## Features

### Core
- ⏱️ **Configurable Timer** – 5/10/15/20/30 minute limits or custom manual entry
- 👹 **Monster Overlay** – Fullscreen scary image/video when time's up
- 🔐 **Parent PIN** – 4-digit PIN to bypass the overlay
- 👾 **Monster Gallery** – Upload up to 5 custom scary images/videos
- 🛡️ **Monitoring Toggle** – Enable/disable monitoring instantly (v1.1)

### Enhanced
- 💾 **Persistent Timer** – Timer survives app restarts
- ⚠️ **Progressive Warnings** – 2-min and 1-min warnings with sounds
- 🔊 **Sound Effects** – Alarm sound + vibration when monster appears
- 📊 **Usage Statistics** – Daily/weekly Shorts watch time tracking
- ⭐ **Reward System** – Tracks when child stops voluntarily

### Overlay Actions (v1.1)
After entering the parent PIN on the overlay, you get:
- **✓ Close** – Dismiss the overlay
- **⏸ Disable Monitoring** – Turn off monitoring from the overlay
- **⚙ Open Settings** – Jump to app settings

## Legal & Privacy

- **EULA:** The app requires accepting an End User License Agreement on first launch.
- **Privacy Policy:** Monster Timer collects **no data**. All configs are stored encrypted on-device. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md).

## Prerequisites

- **Android Studio** (Arctic Fox+)
- **Android device** running Android 8.0+ (API 26+)

## Build

```bash
bash gradlew assembleRelease
```

## First-Time Setup

1. Open app → Accept EULA
2. "Enable Accessibility Service" → Find "Monster Timer" → Toggle ON
3. "Enable Overlay Permission" → Toggle ON

## Usage

1. **Toggle monitoring** ON/OFF as needed
2. Set **timer duration** (5-30 minutes or custom)
3. Set **4-digit PIN**
4. Add **monster images/videos** (Up to 5)
5. Tap **Save All Settings**
6. Open YouTube → Watch Shorts → Timer starts!

## Project Structure
```
app/src/main/java/com/monstertimer/app/
├── MainActivity.kt          # Settings UI + EULA
├── MonsterTimerApp.kt       # App + AppSettings
├── data/
│   └── DataModels.kt        # UsageStats, PersistentTimer
├── service/
│   ├── ShortsAccessibilityService.kt  # Shorts detection + timer
│   └── MonsterOverlayService.kt       # Overlay + post-PIN actions
└── util/
    └── SoundManager.kt      # Sound effects + vibration
```

## Changelog

### v1.1 (February 2026)
- Added **Monitoring Toggle** to enable/disable Shorts monitoring
- Added **Post-PIN Action Panel** with Close, Disable Monitoring, and Open Settings
- Created **FUTURE_FEATURES.md** with planned improvements
- Bug fixes: EULA persistence, timer state, crash on EULA accept

### v1.0 (February 2026)
- Initial release
