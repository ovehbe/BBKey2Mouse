# BBKey2Mouse 🖱️

Turn your BlackBerry Key2's touch-sensitive keyboard into a mouse trackpad!

## Features

- **Mouse Cursor Overlay**: Displays a customizable cursor that you control with keyboard swipes
- **Configurable Controls**:
  - Long-press any key to toggle mouse mode (default: Space)
  - Customizable left-click key (default: Enter)
  - Customizable right-click/long-press key (default: Backspace)
  - Hold Shift to switch to scroll mode
- **Pointer Customization**:
  - Adjustable cursor speed (0.5x to 5x)
  - Adjustable cursor size (16px to 128px)
  - Multiple pointer styles: Arrow, Cheese 🧀, Dot, Hand
- **Haptic Feedback**: Optional vibration on clicks
- **Auto-hide Cursor**: Cursor fades after inactivity

## How It Works

The BlackBerry Key2 has a unique touch-sensitive keyboard that normally works for scrolling. This app uses Android's Accessibility Service to:

1. Intercept keyboard touch events before they become scroll gestures
2. Display a cursor overlay on screen
3. Translate touch movements to cursor movements
4. Perform click gestures at cursor position

## Installation

1. Build the APK using Android Studio or Gradle:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install on your BB Key2:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Open the app and grant permissions:
   - **Accessibility Service**: Required to overlay cursor and perform clicks
   - **Draw Over Other Apps**: Required to display the cursor overlay

## Usage

1. **Enable the service**: Go to Settings → Accessibility → BBKey2Mouse Cursor → Enable
2. **Grant overlay permission**: Allow drawing over other apps
3. **Toggle mouse mode**: Long-press your configured toggle key (default: Space bar)
4. **Move cursor**: Swipe on the keyboard surface
5. **Click**: Press Enter (or your configured key)
6. **Right-click/Long-press**: Press Backspace (or your configured key)
7. **Scroll**: Hold Shift while swiping

## Settings

Access settings from the gear icon in the app:

### Pointer Settings
- **Speed**: How fast the cursor moves relative to finger movement
- **Size**: Cursor size in pixels
- **Image**: Choose from Arrow, Cheese, Dot, or Hand pointer

### Key Bindings
- **Toggle Key**: Long-press to enable/disable mouse mode
- **Click Key**: Press to left-click at cursor position
- **Right Click Key**: Press to perform long-press (context menu)
- **Scroll Mode Key**: Hold while swiping to scroll instead of moving cursor

### Behavior
- **Vibrate on Click**: Haptic feedback when clicking
- **Auto-hide Cursor**: Fade cursor after 3 seconds of inactivity

## Technical Details

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- Uses `AccessibilityService` for gesture dispatch
- Uses `TYPE_ACCESSIBILITY_OVERLAY` for cursor display
- Intercepts `MotionEvent` from keyboard touchpad input device

## Permissions Explained

- **SYSTEM_ALERT_WINDOW**: Draw cursor overlay on screen
- **BIND_ACCESSIBILITY_SERVICE**: Intercept key events and perform click gestures
- **VIBRATE**: Haptic feedback on clicks
- **FOREGROUND_SERVICE**: Keep service running in background

## Troubleshooting

### Keyboard not detected
- Make sure you're using a BlackBerry Key2 or Key2 LE
- The app looks for input devices with touchpad capabilities

### Cursor not appearing
- Check that overlay permission is granted
- Check that accessibility service is enabled
- Try toggling mouse mode with long-press on your toggle key

### Mouse mode not activating
- Hold the toggle key for at least 500ms (half a second)
- Check your configured toggle key in settings

## Building from Source

Requirements:
- Android Studio Hedgehog or newer
- JDK 17
- Gradle 8.2

```bash
git clone <repo>
cd BBKey2Mouse
./gradlew build
```

## License

MIT License - See LICENSE file for details

## Credits

Made for the BlackBerry Key2 community! 🫐

