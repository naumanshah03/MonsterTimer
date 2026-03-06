# MonsterTimer — Future Features (v1.2+)

Detailed problem statements for planned improvements, prioritized by user impact.

---

## 🔴 F1: Per-Day Schedules

**Problem:** Parents want different screen time limits on weekdays vs weekends. A child might be allowed 10 minutes of Shorts on a school day but 30 minutes on Saturday. Currently, the app only supports a single global timer duration, forcing parents to manually change the setting every day.

**User Story:** *As a parent, I want to set different timer durations for each day of the week, so my child has appropriate limits without me having to change settings daily.*

**Acceptance Criteria:**
- [ ] Settings screen shows a per-day configuration (Mon–Sun)
- [ ] Each day has its own timer duration (slider or manual input)
- [ ] A "Same for all days" toggle for quick uniform setup
- [ ] The accessibility service reads the correct duration based on the current day
- [ ] Changing day-specific settings does not affect an already-running timer

**Technical Notes:**
- Store as a JSON map in `EncryptedSharedPreferences`: `{"mon": 10, "tue": 10, ..., "sat": 30, "sun": 30}`
- `ShortsAccessibilityService.startOrResumeTimer()` reads today's value via `Calendar.getInstance().get(Calendar.DAY_OF_WEEK)`

---

## 🔴 F2: Daily Auto-Reset

**Problem:** The timer state persists across days. If the child used all their time on Monday, the expired state carries into Tuesday. The parent must manually clear or restart the app. This is unintuitive and leads to unexpected "Time's Up!" overlays on a new day.

**User Story:** *As a parent, I want the timer to automatically reset at midnight, so my child gets a fresh allowance each day without any manual intervention.*

**Acceptance Criteria:**
- [ ] At midnight (or on first accessibility event of a new day), the persisted timer state is cleared
- [ ] The child gets a full fresh timer for the new day
- [ ] Usage stats already track per-day data; this should not affect historical stats
- [ ] Works even if the phone was off at midnight (check on next service event)

**Technical Notes:**
- `PersistentTimerState` already stores `lastUpdatedTimestamp`
- In `restoreTimerState()` and `startOrResumeTimer()`, compare `lastUpdatedTimestamp` date with today's date
- If different day → call `PersistentTimerState.clear()` before proceeding

---

## 🟡 F3: Usage History Chart

**Problem:** Parents have no visual way to track their child's Shorts consumption over time. The current stats show only today's numbers and a text-based weekly total. A visual chart would make patterns obvious (e.g., "usage spikes on weekends") and help parents make informed decisions about limits.

**User Story:** *As a parent, I want to see a bar chart of my child's daily Shorts watch time for the past 7 days, so I can identify patterns and adjust limits accordingly.*

**Acceptance Criteria:**
- [ ] A horizontal bar chart showing daily watch time for the last 7 days
- [ ] Each bar is labeled with the day name and total minutes
- [ ] Color-coded: green if under limit, red if monster was triggered
- [ ] Displayed in the Stats card or a new dedicated card
- [ ] Updates every time the app is opened

**Technical Notes:**
- `UsageStats.getWeeklyStats()` already returns 7-day data
- Use a lightweight charting library (e.g., MPAndroidChart) or custom Canvas drawing
- Consider keeping the app dependency-light by using a custom `View` with `Canvas.drawRect()`

---

## 🟡 F4: Multiple Child Profiles

**Problem:** Families with more than one child cannot use MonsterTimer effectively. All children share the same timer, PIN, and monster gallery. One child's usage counts against another's allowance.

**User Story:** *As a parent with multiple children, I want to create separate profiles with different timer durations and PINs for each child, so each child has their own independent screen time limits.*

**Acceptance Criteria:**
- [ ] A profile selector dropdown or tab in the main settings screen
- [ ] Each profile has: name, timer duration, PIN, monster gallery, usage stats
- [ ] Active profile is used by the accessibility service
- [ ] Switching profiles requires the parent PIN
- [ ] Maximum 5 profiles

**Technical Notes:**
- Store profiles as a JSON array in `EncryptedSharedPreferences`
- Add `activeProfileId` field
- Refactor `AppSettings` to `AppSettings.forProfile(profileId)`
- This is a larger refactor; consider for v1.3+

---

## 🟢 F5: Custom Warning Messages

**Problem:** The current warning messages ("2 minutes left!", "1 minute left!") and overlay message ("Too much Shorts! Time to take a break!") are hardcoded. Parents may want to personalize these to be more effective for their specific child (e.g., using the child's name, or a gentler/firmer tone).

**User Story:** *As a parent, I want to customize the warning and overlay messages, so they resonate better with my child's personality.*

**Acceptance Criteria:**
- [ ] Settings screen has editable text fields for: 2-min warning, 1-min warning, and overlay message
- [ ] Default values pre-filled
- [ ] Messages are read by the service and overlay at display time
- [ ] Character limit of 100 chars per message

**Technical Notes:**
- Add three new `String` fields to `AppSettings`
- `ShortsAccessibilityService.checkAndShowWarnings()` reads custom messages
- `MonsterOverlayService.createOverlayView()` reads custom overlay message

---

## 🟢 F6: Positive Reinforcement Notification

**Problem:** The app only punishes (shows a scary monster) but doesn't reward. Behavioral psychology shows that positive reinforcement is more effective than punishment alone. If a child voluntarily stops watching Shorts before time runs out, they should receive acknowledgment.

**User Story:** *As a parent, I want my child to receive a "Great job!" notification when they stop watching Shorts voluntarily, so they feel rewarded for good behavior.*

**Acceptance Criteria:**
- [ ] If the child leaves Shorts with ≥30% of their timer remaining, show a positive notification
- [ ] Notification includes an encouraging message and a star emoji
- [ ] Track "voluntary stops" in `UsageStats.timesStoppedEarly` (already exists)
- [ ] Optionally play a pleasant sound effect

**Technical Notes:**
- In `ShortsAccessibilityService.onAccessibilityEvent()`, when the user leaves Shorts and `currentRemainingMillis > totalTime * 0.3`, trigger a positive notification
- Reuse `showWarningNotification()` with a different channel/message
- `SoundManager.playRewardSound()` already exists

---

## 🟢 F7: App Lock

**Problem:** Tech-savvy children can open the MonsterTimer app and change settings (increase timer, change PIN, or disable monitoring via the toggle). There is no barrier preventing the child from accessing the app settings.

**User Story:** *As a parent, I want the MonsterTimer app itself to be PIN-protected, so my child cannot change the settings without my knowledge.*

**Acceptance Criteria:**
- [ ] On app launch, require PIN entry before showing settings (after EULA is accepted)
- [ ] Incorrect PIN shows error; 5 failed attempts lock for 5 minutes
- [ ] Optional: biometric unlock (fingerprint) as alternative to PIN
- [ ] The overlay PIN bypass remains separate from the app lock PIN (or reuse the same one)

**Technical Notes:**
- Add PIN check in `MainActivity.onCreate()` after EULA check
- Store failed attempt count and lockout timestamp in `EncryptedSharedPreferences`
- Consider using Android BiometricPrompt API for fingerprint support
