# RPG Launcher

A minimalist native Android home screen with three playable classes. Each class has a different layout and keeps its own XP, daily quest progress, and unlocks. Progress is stored on the device; there are no accounts, ads, or usage-access permissions.

| Class | Starting home | Skill tree |
| --- | --- | --- |
| Warden | Amber serif Keep with a spare layout | Clock → one pinned app or a Home without XP counters |
| Ranger | Green Trail with a two-column app grid | More tiles → last-opened shortcut or daily tracking on Home |
| Mage | Violet, monospace Observatory | Home search → alphabetical drawer sections or a next-skill indicator |

Open an app through the launcher to earn **5 XP per distinct app package**, up to five packages per day. Open three different apps and claim another **10 XP** in Quest. Days follow the device's local date. The root skill unlocks at 10 XP and opens two branches at 25 and 50 XP. Unlocks are manual and don't spend XP.

## Build and install

Requires JDK 17, Gradle 8.9, and Android SDK Platform 35 / Build Tools 35.0.0. The app runs on Android 8.0 or later. Open the repo in Android Studio, or run:

```sh
gradle :app:assembleDebug :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **RPG Launcher** from your current launcher to try it without changing Home. To make it the default, use **Classes → Android default Home settings** and select **RPG Launcher**. You can switch back in Android's Default apps settings.

## Device smoke test

1. Choose a class and verify that Apps lists installed launchable apps and filters by name.
2. Open one app twice; verify you get 5 XP total. Open two other apps and claim the daily quest once; verify you have 25 XP.
3. Try unlocking a branch before the root; it should be locked even with sufficient XP. Unlock the root and then the 25 XP branch; verify both features appear.
4. Switch classes and verify the layout changes and new-class XP starts at zero. Switch back and relaunch to check persistence.
5. Set RPG Launcher as default Home and press Android Home from another app. Switch back to your previous Home app when finished.

## Personalize a class

Open **Classes → Customize [class name]** to pick accent and background colors using hue, saturation, and brightness sliders with a live preview, select a built-in font, or import a local TTF/OTF font up to 10 MB. You can also enable compact spacing and hide the Home description or XP counter. Settings follow the class when you switch, and resetting appearance doesn't reset XP or skill unlocks. Try switching between two differently styled classes and relaunching to verify that their settings persist independently.
