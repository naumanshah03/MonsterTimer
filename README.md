# Monster Timer - Android App

A parental control app that monitors YouTube Shorts usage and displays a "monster" overlay when the timer expires.

## Features

### Core
- ⏱️ **Configurable Timer** – 5/10/15/20/30 minute limits
- 👹 **Monster Overlay** – Fullscreen scary image/video when time's up
- 🔐 **Parent PIN** – 4-digit PIN to bypass the overlay
- 👾 **Monster Gallery** – Upload up to 5 custom scary images/videos

### Enhanced (v2)
- 💾 **Persistent Timer** – Timer survives app restarts
- ⚠️ **Progressive Warnings** – 2-min and 1-min warnings with sounds
- 🔊 **Sound Effects** – Alarm sound + vibration when monster appears
- 📊 **Usage Statistics** – Daily/weekly Shorts watch time tracking
- ⭐ **Reward System** – Tracks when child stops voluntarily

## Legal & Privacy

- **EULA:** The app requires accepting an End User License Agreement on first launch, confirming that this is a parental tool and all responsibility lies with the parent/guardian.
- **Privacy Policy:** Monster Timer collects **no data** and sends nothing to the internet. All configs are stored encrypted on-device. The Accessibility Service is used strictly to spot YouTube Shorts UI elements locally. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for details.

## Prerequisites

- **Android Studio** (Arctic Fox+)
- **Android device** running Android 8.0+ (API 26+)

## Build Steps

1. Open this folder in Android Studio
2. Wait for Gradle sync
3. Connect device with USB Debugging enabled
4. Click **Run > Run 'app'**

## First-Time Setup

### 1. Accessibility Service
Open app → "Enable Accessibility Service" → Find "Monster Timer" → Toggle ON

### 2. Overlay Permission
"Enable Overlay Permission" → Toggle "Allow display over other apps" ON

## Usage

1. **Accept the EULA** on first launch
2. Set **timer duration** (5-30 minutes, or enter a custom manual time)
3. Set **4-digit PIN**
4. Add **monster images/videos** (Up to 5)
5. Tap **Save All Settings**
6. Open YouTube → Watch Shorts → Timer starts!

## Project Structure
```
app/src/main/java/com/monstertimer/app/
├── MainActivity.kt          # Settings UI + EULA logic
├── MonsterTimerApp.kt       # App + AppSettings (EncryptedStorage)
├── data/
│   └── DataModels.kt        # UsageStats, PersistentTimer
├── service/
│   ├── ShortsAccessibilityService.kt  # Shorts detection + timer
│   └── MonsterOverlayService.kt       # Full-screen overlay
└── util/
    └── SoundManager.kt      # Sound effects + vibration
```
