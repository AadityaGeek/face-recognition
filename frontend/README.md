# Face Recognition & Liveness Verification - Android Application

A modern Android application built with **Jetpack Compose**, **CameraX**, **ML Kit**, and **Retrofit** for real-time face recognition, liveness verification, and secure backend user authentication.

---

## 📋 Features

- 📸 **Live Camera Feed & Face Detection**: Real-time face framing and quality checks powered by CameraX and ML Kit.
- 👁️ **Liveness Detection**: Anti-spoofing checks (blink detection, smile detection, head movement verification).
- 🧭 **Structured Jetpack Navigation**: Modular navigation routing between Registration, Verification, QR Code Scanner, and Dynamic Settings.
- 🔐 **User Registration & Biometric Verification**: Multi-step registration flow with face capture, vector matching, and remote backend sync.
- 📱 **Universal & ABI Split APK Builds**: Flexible Gradle build outputs (universal APK for testing, ABI splits for optimized binary sizes).
- 🌐 **Dynamic API Base URL**: Easily switch between local development backend and live production servers directly in app settings.

---

## 🛠 Prerequisites

Before building or running the project, ensure you have the following installed:

1. **Java Development Kit (JDK 17 or higher, JDK 23 supported)**
   - Verify installation: `java -version`
   - Ensure the `JAVA_HOME` environment variable points to your JDK directory.
2. **Android SDK (API Level 24 minimum, target/compile SDK 36)**
   - Installed via [Android Studio](https://developer.android.com/studio) or command-line tools.
   - Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` environment variable is set.
3. **Gradle (9.3.1 or higher)**
   - Managed automatically via the included Gradle Wrapper (`gradlew` / `gradlew.bat`).
4. **Android Device or Emulator**
   - Running Android 7.0 (API Level 24) or higher with camera support.

---

## ⚙️ Environment & API Configuration

### Step 1: Environment File (`.env`)
Create a `.env` file in the `frontend` root directory by copying `.env.example`:

```cmd
copy .env.example .env
```

If using Gemini AI features, set your API key inside `.env`:
```env
GEMINI_API_KEY=your_actual_gemini_api_key_here
```

### Step 2: Local SDK Path (`local.properties`)
If building via command line without `ANDROID_HOME` set globally, create a `local.properties` file in `frontend`:
```properties
sdk.dir=C\:\\Users\\<Your-Username>\\AppData\\Local\\Android\\Sdk
```
*(Adjust the path to match your Android SDK location).*

### Step 3: Backend Endpoint Setup
By default, the app is configured in `app/src/main/java/com/example/data/ApiConfig.kt` to connect to the backend server.

For **Local Development**:
- **Android Emulator**: Use `http://10.0.2.2:8000/` (Android emulator routes `10.0.2.2` to the host machine's `localhost:8000`).
- **Physical Device (ADB Reverse)**: Connect via USB, run `adb reverse tcp:8000 tcp:8000`, and set URL to `http://localhost:8000/`.
- **Physical Device (Local Network)**: Use host machine's LAN IP address, e.g., `http://192.168.1.100:8000/`.
*(Note: You can also update the backend base URL dynamically from within the app settings screen).*

---

## 📱 Installation & Build Guide

### Option A: Using Android Studio (Recommended GUI)

1. Open **Android Studio**.
2. Click **Open** and select the `frontend` folder (`frontend`).
3. Allow Gradle to download dependencies and sync (`File` ➔ `Sync Project with Gradle Files`).
4. Connect an Android device via USB (with USB Debugging enabled) or start an Android Virtual Device (AVD) emulator.
5. Select `app` in the run configuration dropdown.
6. Click **Run** (`Shift + F10`) or **Debug** (`Shift + F9`).

---

### Option B: Using Command Line (`cmd` / PowerShell / Terminal)

#### 1. Navigate to Frontend Directory
```cmd
cd frontend
```

#### 2. Verify Gradle Wrapper
If `gradlew.bat` (Windows) or `gradlew` (Linux/macOS) is missing, generate it:
```cmd
gradle wrapper --gradle-version 9.3.1
```

#### 3. Build Debug APK
Clean and build debug APKs:
```cmd
.\gradlew clean assembleDebug
```
Upon completion (`BUILD SUCCESSFUL`), the output APKs will be located at:
```text
app\build\outputs\apk\debug\app-universal-debug.apk
app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
app\build\outputs\apk\debug\app-armeabi-v7a-debug.apk
```

#### 4. Install APK onto Connected Device / Emulator
Ensure your device or emulator is visible (`adb devices`), then install the universal APK:
```cmd
adb install app\build\outputs\apk\debug\app-universal-debug.apk
```
Alternatively, build and install in a single step using Gradle:
```cmd
.\gradlew installDebug
```

#### 5. Build Release APK (Optional)
To generate a production release APK:
```cmd
.\gradlew assembleRelease
```
*Note: Release builds require setting environment variables for signing (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`).*

---

## 📂 Project Structure Overview

```text
frontend/
├── app/
│   ├── src/main/java/com/example/
│   │   ├── data/                  # Retrofit API interface, DTOs, and ApiConfig
│   │   ├── ui/                    # Jetpack Compose UI screens, theme, ViewModels
│   │   │   ├── components/        # CameraView, QrScannerView, LivenessOverlay
│   │   │   └── theme/             # App typography, icons, colors
│   │   ├── MainNavigation.kt      # Navigation Host routing screens
│   │   ├── RegistrationScreen.kt  # User registration UI & state logic
│   │   ├── VerificationScreen.kt  # Real-time face verification UI
│   │   └── MainActivity.kt        # Entry point & runtime camera permission handler
│   └── build.gradle.kts           # Application build dependencies & ABI split configs
├── gradle/                        # Gradle wrapper files & dependencies catalog
├── .env.example                   # Environment variable template
├── build.gradle.kts               # Root project build file
├── settings.gradle.kts            # Project settings & repository definitions
└── README.md                      # Project documentation
```

---

## 🔍 Step-by-Step Debugging Guide

### 1. Real-Time Logcat Monitoring
To view logs from the running application on your connected device:

- **Via Android Studio**: Open the **Logcat** tab at the bottom and filter by `package:mine` or tag `MainActivity`.
- **Via Terminal**:
  ```cmd
  adb logcat -s MainActivity LivenessViewModel FaceRecognitionApi OkHttp
  ```
- **Filter Only Error Logs**:
  ```cmd
  adb logcat *:E
  ```

### 2. Network & API Request Inspection
Network traffic (HTTP requests/responses) can be inspected using the built-in OkHttp logging interceptor.
- Search Logcat for `OkHttp` to view request headers, request bodies (Base64 payloads), status codes, and server response times.
- If requests fail with `java.net.ConnectException` or `Timeout`, verify backend accessibility using `curl`:
  ```cmd
  curl -I http://10.0.2.2:8000/health
  ```

### 3. Camera & Hardware Debugging
- Ensure Camera permission is granted on the device (`Settings` ➔ `Apps` ➔ `Face Recognition` ➔ `Permissions`).
- **Emulator Camera Configuration**:
  1. Open AVD Manager in Android Studio.
  2. Edit your virtual device.
  3. Under **Advanced Settings**, ensure **Front Camera** is set to `Webcam0` (or `Emulated`).
  4. Restart the emulator.

---

## 🚨 Troubleshooting & Common Issues

### Issue 1: `Minimum supported Gradle version is 9.3.1. Current version is 8.7`
- **Cause**: The Android Gradle Plugin (AGP) version requires Gradle 9.3.1 or higher.
- **Fix**: Open `gradle/wrapper/gradle-wrapper.properties` and update `distributionUrl`:
  ```properties
  distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
  ```

### Issue 2: `Keystore file '<project-root>\debug.keystore' not found`
- **Cause**: Missing custom debug signing key specified in `app/build.gradle.kts`.
- **Fix**: Update `app/build.gradle.kts` `debug` build type to use standard debug fallback.

### Issue 3: `CLEARTEXT communication to <IP> not permitted`
- **Cause**: Android 9+ (API 28+) blocks non-HTTPS (HTTP) traffic by default.
- **Fix**: For local HTTP development (`http://10.0.2.2:8000` or local IP), ensure `android:usesCleartextTraffic="true"` is configured in `app/src/main/AndroidManifest.xml` under `<application>`.

---

## 🔄 Verification & Liveness Flow (Detection & Capture)

The face detection, gesture liveness verification, and biometric capture sequence operates as follows:

```text
┌────────────────────┐    1. Camera Frame Stream     ┌───────────────────────┐
│  CameraX Preview   │ ────────────────────────────> │  ML Kit Face Detector │
└────────────────────┘                               └───────────────────────┘
                                                                 │
                                                                 ▼
                                                     ┌───────────────────────┐
                                                     │ Face Framing & Quality│
                                                     │  Validation (1 Face)  │
                                                     └───────────────────────┘
                                                                 │
                                                                 ▼
┌────────────────────┐    3. Real-Time Analysis      ┌───────────────────────┐
│  Gesture Feedback  │ <──────────────────────────── │ MotionLivenessChecker │
│  (Warning/Alerts)  │                               │ (Blink, Smile, Turns) │
└────────────────────┘                               └───────────────────────┘
                                                                 │
                                                                 │ 4. All 4 Challenges Passed
                                                                 ▼
┌────────────────────┐    5. Multipart API Request   ┌───────────────────────┐
│ Backend Verification│ <──────────────────────────── │ Biometric Capture &   │
│   (/verify Endpoint)│                              │ High-Res Face Crop    │
└────────────────────┘                               └───────────────────────┘
```

### Detailed Sequence Steps:

1. **Live Frame Streaming & ML Kit Analyzer**:
   - CameraX streams `YUV_420_888` camera frames via `ImageAnalysis` analyzer.
   - Each frame is converted to an `InputImage` and analyzed in real-time by Google ML Kit Face Detection.

2. **Face Framing & Quality Validation**:
   - ML Kit evaluates bounding box bounds, face area ratios, eye opening probabilities, smiling probabilities, and head orientation angles (`Euler X` pitch, `Euler Y` yaw).
   - Validation checks confirm that exactly one face is framed, centered inside the circular UI guide, and sufficiently visible.

3. **Interactive Motion Liveness Challenges**:
   - The user is presented with a random sequence of **4 liveness challenges** (e.g. *Blink Both Eyes*, *Smile*, *Turn Head Left*, *Turn Head Right*, *Nod Head Up*, or *Nod Head Down*).
   - `MotionLivenessChecker` tracks live landmark metrics against target thresholds:
     - **Blink**: Both eye opening probabilities drop below `0.40`.
     - **Smile**: Smiling probability exceeds `0.45` facing forward.
     - **Head Turn Left**: Yaw exceeds `+12.0°`.
     - **Head Turn Right**: Yaw drops below `-12.0°`.
     - **Nod Up / Down**: Pitch angle exceeds `+10.0°` (Up) or `-10.0°` (Down).

4. **Automatic Biometric Capture & Remote Verification**:
   - Upon completing all **4 challenges** successfully, the app captures a high-resolution frame via `ImageCapture`, crops the face region, and submits a `MultipartBody` request to `/verify`.

