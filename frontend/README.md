<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run, build, and debug your app locally.

View your app in AI Studio: https://ai.studio/apps/0ca17a42-0efc-4dfc-b76e-69c775fed356

---

## 🛠 Prerequisites

1. **Java Development Kit (JDK 17 or later)**: Ensure `JAVA_HOME` environment variable is set.
2. **Android SDK**: Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` is set, or [Android Studio](https://developer.android.com/studio) is installed.
3. **Environment File (`.env`)**:
   Create a `.env` file in the project root folder (see `.env.example`):
   ```cmd
   copy .env.example .env
   ```

---

## 📱 Building the APK via Command Line (`cmd` / PowerShell)

### Step 1: Open Terminal in Project Directory
```cmd
cd /d <path-to-project-root>
```

### Step 2: Generate Gradle Wrapper (if missing)
If `gradlew.bat` is not present in the project directory, generate it using global Gradle:
```cmd
gradle wrapper
```
*(Ensure `gradle/wrapper/gradle-wrapper.properties` uses Gradle `9.3.1` or higher).*

### Step 3: Build the Debug APK
Run the assemble command:
```cmd
gradlew assembleDebug
```

### Step 4: Locate Built APK
Once `BUILD SUCCESSFUL` appears, your APK is generated at:
```text
app\build\outputs\apk\debug\app-debug.apk
```

### Step 5: Install APK to Device or Emulator
Connect your device or start an emulator, then run:
```cmd
adb install app\build\outputs\apk\debug\app-debug.apk
```
Or directly install via Gradle:
```cmd
gradlew installDebug
```

---

## 🔍 Troubleshooting & Common Issues

### 1. `Minimum supported Gradle version is 9.3.1. Current version is 8.7.`
* **Cause**: Android Gradle Plugin (AGP) 9.1.1 requires Gradle 9.3.1+.
* **Fix**: Open `gradle/wrapper/gradle-wrapper.properties` and update the `distributionUrl`:
  ```properties
  distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
  ```

---

### 2. `Keystore file '<project-root>\debug.keystore' not found for signing config 'debugConfig'.`
* **Cause**: The `app/build.gradle.kts` configuration tried to use a missing custom `debug.keystore` file in the root directory.
* **Fix**: Open `app/build.gradle.kts` and update the `debug` build type to empty so it defaults to the built-in Android debug key:
  ```kotlin
  buildTypes {
      release {
          ...
      }
      debug { }
  }
  ```

---

### 3. `Secrets Gradle Plugin error` / `.env` File Missing
* **Cause**: The build requires a `.env` file for property resolution.
* **Fix**: Duplicate `.env.example` as `.env` in the root folder:
  ```cmd
  copy .env.example .env
  ```
  Set your `GEMINI_API_KEY` inside `.env`.

---

### 4. `JAVA_HOME is not set` or `SDK location not found`
* **Cause**: Environment variables for Java or Android SDK are missing.
* **Fix**:
  Set environment variables in `cmd`:
  ```cmd
  set JAVA_HOME=<path-to-jdk-installation>
  set ANDROID_HOME=<path-to-android-sdk>
  ```
  Or create a `local.properties` file in the project root containing:
  ```properties
  sdk.dir=<path-to-android-sdk>
  ```
