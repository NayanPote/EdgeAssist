# EdgeAssist

EdgeAssist is a lightweight Android application that provides quick access to essential features through a floating overlay on the screen. The app is designed to improve accessibility and usability by allowing users to control the device volume and make one-tap speed dial calls without leaving their current activity.

---

## Features

- **Floating Overlay Control**  
  A draggable and movable control that stays on top of other applications.  
- **Volume Controls**  
  Quickly increase or decrease the device volume using the floating control.  
- **Speed Dial**  
  Configure a preferred contact for instant one-tap calling.  
- **Snap to Edge**  
  Automatically aligns to the nearest screen edge for a neat appearance.  
- **Double Tap Gesture**  
  Allows secondary action execution such as speed dialing.  
- **Persistent Service**  
  Runs as a foreground service with a notification to ensure stability.  

---

## Technical Details

- **Foreground Service** with a persistent notification for reliable operation.  
- **Overlay Permission** to display floating controls on top of other apps.  
- **Gesture Detection** for tap, double-tap, and drag movements.  
- **Customizable Position** with saved preferences to restore control location.  
- **Snap Animation** using `ValueAnimator` for smooth edge docking.  

---

## How It Works

1. When the service is started, a floating control view is created and displayed.  
2. The user can drag the control to reposition it anywhere on the screen.  
3. Single tap executes the primary action (volume control).  
4. Double tap triggers the secondary action (speed dial).  
5. The control snaps to the nearest screen edge when released.  
6. User preferences, such as the control’s position, are saved for future sessions.  

---

## Requirements

- Android 6.0 (Marshmallow) or higher.  
- Overlay permissions enabled by the user.  
- Phone call permission for speed dial functionality.  

---

## Use Cases

- Quickly adjust volume during gaming or video playback without leaving the app.  
- Make fast one-tap calls to your saved contact while multitasking.  
- Maintain accessibility by keeping important controls within easy reach.  

---

## License

This project is open-source and available for personal or educational use.  

---

## Copyright

© 2025 Nayan Pote. All rights reserved.  
