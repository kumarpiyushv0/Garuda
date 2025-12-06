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

## 💻 Insights for Developers

Garuda is built natively for Android using modern development practices, adhering to **Clean Architecture** principles and the **MVVM (Model-View-ViewModel)** pattern.

### 🏗️ Project Structure

The project is organized into the following distinct layers:

*   **`ui/`**: Contains all Jetpack Compose screens, components, and ViewModels. Handles user interaction and data presentation.
*   **`domain/`**: The core business logic layer. Contains UseCases, Repository interfaces, and Data models. This layer is pure Kotlin and independent of the Android framework.
*   **`data/`**: Handles data retrieval and storage. Implements Repository interfaces and manages sources like Room (local DB), DataStore, and Network APIs (Firebase/Retrofit).
*   **`di/`**: Hilt modules for Dependency Injection, providing dependencies across the app.
*   **`service/`**: Foreground services for long-running tasks like Location Tracking, Voice Activation, and Shake detection.

### 🛠️ Tech Stack & Libraries

*   **Language**: [Kotlin](https://kotlinlang.org/) (100%)
*   **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material3 Design)
*   **Dependency Injection**: [Hilt](https://dagger.dev/hilt/)
*   **Asynchronous Programming**: [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
*   **Local Storage**:
    *   [Room Database](https://developer.android.com/training/data-storage/room) for contact management.
    *   [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for user preferences.
*   **Cloud & Backend**:
    *   **Firebase Authentication**: Secure user login.
    *   **Firebase Firestore**: Storing user profiles and contacts.
    *   **Firebase Storage**: Uploading evidence (audio/images).
*   **AI & ML**:
    *   **Gemini AI**: Used for intelligent analysis of situations (via `GeminiAnalyzer`).
*   **Mapping**: [Google Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview)
*   **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable background execution.

### ⚙️ Setup & Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/garuda.git
    cd garuda
    ```

2.  **Firebase Setup**:
    *   Create a project in the [Firebase Console](https://console.firebase.google.com/).
    *   Enable **Authentication** (Email/Password), **Firestore**, and **Storage**.
    *   Download `google-services.json` and place it in the `app/` directory.

3.  **API Keys Configuration**:
    *   Obtain a **Google Maps API Key** from Google Cloud Console.
    *   Obtain a **Gemini API Key** from Google AI Studio.
    *   Add them to your `local.properties` file (do NOT commit this file):
        ```properties
        MAPS_API_KEY=your_actual_maps_key
        GEMINI_API_KEY=your_actual_gemini_key
        ```

4.  **Build and Run**:
    *   Open the project in Android Studio (Koala or later recommended).
    *   Sync Gradle files.
    *   Select an emulator or physical device.
    *   Run the app (`Shift + F10`).

### 📝 Permissions Explained

*   **`ACCESS_FINE_LOCATION`**: Critical for sending precise location in SOS alerts.
*   **`SEND_SMS`**: Used as a fallback to send SOS alerts via SMS when internet is unavailable.
*   **`RECORD_AUDIO`**: Allows the app to record ambient sound during an emergency.
*   **`FOREGROUND_SERVICE`**: Required to keep the app active and tracking location even when the screen is locked.
