# Brain Smooch

Android app that blocks access to distracting websites and apps for a set duration. Like SelfControl for macOS, but for Android.

## Features

- **Website Blocking**: Local VPN DNS sinkhole blocks domains (no external servers)
- **App Blocking**: Blocks apps via AccessibilityService - sends you home when you try to open them
- **Popular Apps Database**: Block apps even if not installed (Instagram, TikTok, games, etc.)
- **Hardcore Mode**: Prevents uninstalling the app or stopping the block
- **Unlimited Mode**: Block with no timer - only emergency password can release
- **Survives Reboots**: Block persists across device restarts
- **Emergency Password**: Optional password to release block early

## Privacy & Security

- 100% offline - no data leaves your device
- No analytics, no Firebase, no third-party SDKs
- Open source

## Installation

### 1. Build & Install

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open in Android Studio and run.

### 2. Grant Permissions

The app will prompt you to enable:

1. **Device Admin** - Prevents uninstalling during a block
2. **Accessibility Service** - Required for app blocking
3. **Usage Stats** - Required to detect which app is in foreground
4. **VPN** - Required for website blocking (prompted when starting block)

### 3. (Optional) Enable Device Owner for Maximum Protection

For the strongest protection (prevents VPN disconnection, date/time changes, etc.):

```bash
adb shell dpm set-device-owner com.brainsmooch/.receiver.BrainSmoochDeviceAdminReceiver
```

**Prerequisites for Device Owner**:
- No Google account on device (remove all accounts first)
- No other Device Owner/Admin apps

### 4. Usage

1. Select **Websites** or **Apps** tab
2. Add domains (e.g., twitter.com) or search for apps to block
3. Set duration (days, hours, minutes) or enable **Unlimited**
4. Optionally set an **Emergency Password**
5. Press **Smooch me brain** 🧠
6. Confirm and smooch the brain to activate

## Architecture

```
com.brainsmooch/
├── data/
│   ├── BlockState.kt       # Data models
│   └── BlockRepository.kt  # DataStore persistence
├── domain/
│   ├── BlockSessionManager.kt # Block lifecycle
│   └── Sfx.kt              # Sound effects
├── service/
│   ├── BlockerVpnService.kt           # Local VPN DNS sinkhole
│   ├── BlockGuardAccessibilityService.kt # App blocking
│   ├── AppBlockerManager.kt           # Installed/popular apps
│   └── MdmManager.kt                  # Device Policy Manager
├── receiver/
│   ├── BrainSmoochDeviceAdminReceiver.kt
│   ├── BootReceiver.kt
│   └── BlockEndReceiver.kt
├── viewmodel/
│   └── MainViewModel.kt
└── ui/
    ├── theme/
    └── screens/MainScreen.kt
```

## How It Works

### Website Blocking (DNS Sinkhole)
1. App creates a local VPN interface
2. All DNS queries (UDP port 53) are intercepted
3. Blocked domains return 0.0.0.0
4. Allowed domains forward to real DNS

### App Blocking (Accessibility Service)
1. AccessibilityService monitors window changes
2. When a blocked app opens, it triggers GLOBAL_ACTION_HOME
3. User is sent back to the home screen

## Requirements

- Android 8.0+ (API 26)
- ADB access for Device Owner setup (optional)

## License

MIT
