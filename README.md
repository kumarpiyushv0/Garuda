# Garuda
**Women’s Safety App with Live Tracking**

Garuda is a personal safety application designed to provide peace of mind and immediate assistance in emergencies. Whether you're commuting late at night, traveling alone, or just want to ensure your loved ones know you're safe, Garuda acts as your digital guardian.

With a focus on speed and reliability, Garuda allows you to alert your trusted contacts instantly, sharing your live location and situation details without needing to unlock your phone or navigate through complex menus.

![Platform](https://img.shields.io/badge/Platform-Android-brightgreen)
![Language](https://img.shields.io/badge/Language-Kotlin-purple)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26-orange)

## Key Features

### 🆘 Instant SOS Alerts
In an emergency, every second counts. With a single tap or a quick command, Garuda sends an SOS message to your pre-selected emergency contacts. This message includes a "Help!" alert and a link to your live location on Google Maps.

### 📍 Real-Time Location Sharing
Keep your trusted circle informed. When SOS is active, Garuda shares your live location updates, allowing your contacts to track your movement in real-time until you are safe.

### 🎙️ Discreet Audio Recording
Sometimes, evidence is necessary. Garuda can automatically start recording audio in the background when SOS is triggered, helping to capture important conversations or environmental sounds without drawing attention.

### 🗣️ Voice Activation
Hands tied? Just say the safety phrase. Garuda listens for specific voice commands to trigger the SOS mode, ensuring you can call for help even if you can't reach your phone.

### 🔘 Power Button Trigger
For situations where looking at your screen isn't possible, you can trigger an SOS alert by pressing your phone's power button multiple times in rapid succession. This hardware trigger is designed to be discreet and effective.

### 👥 Trusted Contacts Management
You are in control. Easily add family members, friends, or guardians from your phone's contact list. You decide who receives your alerts and location data.

---

## For Developers

Garuda is built natively for Android using modern development practices.

### Tech Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material3)
*   **Architecture**: MVVM with Clean Architecture
*   **Dependency Injection**: Hilt
*   **Services**: Firebase (Firestore, Auth, Storage), Google Maps SDK, Gemini AI

### Setup & Installation
1.  **Clone the repository**: `git clone https://github.com/yourusername/garuda.git`
2.  **Configuration**: Add `google-services.json` to `app/` and API keys (Maps, Gemini) to `local.properties`.
3.  **Build**: Open in Android Studio, sync Gradle, and run.

### Permissions Required
*   Location (Fine/Coarse) - For tracking.
*   SMS - To send alerts.
*   Microphone - For audio evidence/voice format.
*   Contacts - To select guardians.
