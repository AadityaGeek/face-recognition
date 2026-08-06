<div align="center">

  <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/app/src/main/ic_launcher-web.png" width="128" height="128" alt="Liveness Shield Logo" />

  # 🛡️ Liveness Shield
  ### **Next-Gen Biometric Registration & AI-Powered Liveness Verification**

  *An enterprise-grade, camera-first Android application designed for biometric identity enrollment, digital QR credential generation, and real-time facial liveness verification with anti-spoofing protection.*

  <br />

  [![Download APK](https://img.shields.io/badge/Download-Latest%20APK-007ACC?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AadityaGeek/face-recognition/releases)
  [![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-green?style=for-the-badge&logo=android)](https://developer.android.com/)
  [![License](https://img.shields.io/badge/License-MIT-orange?style=for-the-badge)](LICENSE)
  [![Developer](https://img.shields.io/badge/Developer-Aaditya-purple?style=for-the-badge&logo=github)](https://github.com/AadityaGeek)

  <br />

</div>

---

## 📋 Executive Summary

**Liveness Shield** is a sleek, security-focused mobile application built to modernize digital identity workflows. Designed for rapid identity enrollment, secure QR credential issuance, and live face verification, Liveness Shield combines intuitive mobile camera capture with sophisticated server-side AI passive liveness algorithms to prevent presentation attacks (photos, screen replays, paper masks).

Whether deployed for KYC onboarding, access control, identity presentation demos, or enterprise security portfolios, Liveness Shield delivers a fluid, high-confidence user experience from first launch to final audit log.

---

## 🌟 Key Capabilities & Product Features

```
                   ┌─────────────────────────────────────────┐
                   │           LIVENESS SHIELD HUB           │
                   └────────────────────┬────────────────────┘
                                        │
           ┌────────────────────────────┴────────────────────────────┐
           ▼                                                         ▼
┌──────────────────────┐                                  ┌──────────────────────┐
│ IDENTITY ENROLLMENT  │                                  │ LIVENESS VERIFY      │
├──────────────────────┤                                  ├──────────────────────┤
│ • Profile Creation   │                                  │ • ID / QR Code Lookup│
│ • Photo Capture      │                                  │ • Live Camera Check  │
│ • QR Pass Generation │                                  │ • Server Anti-Spoof  │
│ • Server Sync        │                                  │ • Match Score Summary│
└──────────────────────┘                                  └──────────────────────┘
```

### 1. 🪪 Biometric Identity Registration
- **Guided Profile Capture**: Streamlined registration kiosk flow for capturing full identity profiles (Name, DOB, Unique ID).
- **Camera-guided Face Capture**: Real-time facial framing guidelines ensuring high-quality biometric enrollment photos.
- **Instant QR Credential Pass**: Generates an encrypted, scannable QR identity pass upon enrollment completion.

### 2. 🔍 Real-Time Liveness & Anti-Spoofing
- **Passive Liveness Checks**: Evaluates subtle facial cues, texture consistencies, and depth signatures without requiring unnatural user motions.
- **Anti-Spoofing Shield**: Protects against common spoofing techniques including printed photo attacks, digital display playbacks, and static cutouts.
- **Guided Camera Overlay**: High-visibility oval target frame with live status indicators guiding users into optimal verification positioning.

### 3. 📲 Instant QR Verification Scanner
- **Sub-Second Lookup**: Integrated QR scanner allows verification officers or system administrators to scan a user's QR pass for instant identity retrieval.
- **Manual ID Resolution**: Fallback ID lookup mode when physical QR passes are not immediately accessible.

### 4. 📊 Verification Analytics & Audit Logs
- **Confidence Scoring**: Clear match percentages, liveness probability scores, and pass/fail verification flags.
- **Historical Audit Trail**: Logs past verification results for quick security review and audit tracking.

### 5. 🎨 Modern Presentation Design
- **Material Design 3**: Built with Jetpack Compose featuring smooth micro-animations and polished state transitions.
- **Adaptive Dark & Light Theme**: Gorgeous, high-contrast UI tailored for modern dark mode aesthetics and bright environment readability.

---

## 📱 Detailed App Walkthrough & Screenshot Gallery

> Below is a structured visual presentation of the Liveness Shield user experience. *(Replace image placeholder URLs with hosted screenshot assets).*

### Phase 1: Onboarding & Identity Enrollment

<table>
  <tr>
    <td align="center" width="25%"><b>1. Branded Splash</b></td>
    <td align="center" width="25%"><b>2. Identity Hub</b></td>
    <td align="center" width="25%"><b>3. Registration Form</b></td>
    <td align="center" width="25%"><b>4. Photo Capture</b></td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_splash.jpg" width="220" alt="Splash Screen" /><br/>
      <sub>*Branded initial launch experience.*</sub>
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_landing.jpg" width="220" alt="Landing Page" /><br/>
      <sub>*Central landing navigation hub.*</sub>
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_register.jpg" width="220" alt="Registration Form" /><br/>
      <sub>*Structured credential input.*</sub>
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_capture.jpg" width="220" alt="Photo Enrollment" /><br/>
      <sub>*Guided enrollment camera.*</sub>
    </td>
  </tr>
</table>

### Phase 2: QR Pass, Scanning & Live Verification

<table>
  <tr>
    <td align="center" width="25%"><b>5. Digital QR Pass</b></td>
    <td align="center" width="25%"><b>6. QR Scanner</b></td>
    <td align="center" width="25%"><b>7. Live Face Check</b></td>
    <td align="center" width="25%"><b>8. Verification Result</b></td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_qrpass.jpg" width="220" alt="Generated QR Pass" /><br/>
      <sub>*Shareable QR identity pass.*</sub>
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_qrscan.jpg" width="220" alt="QR Scanner" /><br/>
      <sub>*High-speed barcode scanner.*</sub>
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_verify.jpg" width="220" alt="Camera Verification" /><br/>
      <sub>*Live camera anti-spoofing check.*</sub>
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/AadityaGeek/face-recognition/refs/heads/main/.github/assets/screenshot_result.jpg" width="220" alt="Result Screen" /><br/>
      <sub>*High-confidence match summary.*</sub>
    </td>
  </tr>
</table>

---

## 🔒 Security Architecture Highlights

Liveness Shield is engineered with a **Privacy-First & Secure-by-Design** philosophy:

- **Server-Based Authentication**: Identity records, biometric registration profiles, and verification checks are securely managed and authenticated via backend server APIs.
- **Server-Side AI Liveness Engine**: Face matching and anti-spoofing calculations are evaluated on the secure server to ensure tamper-proof verification integrity.
- **Anti-Spoofing Shield**: Backend algorithms analyze live camera frame captures against multi-factor passive checks (texture variance, lighting dynamics, and biometric matching).
- **Secure Encrypted Transport**: All communication between the mobile client and backend server utilizes encrypted TLS (HTTPS) transmission.

---

## 🚀 Downloading & Running the App

### Option 1: Direct APK Download (Recommended for Testing)

Pre-built standalone **APK binaries** are available directly under the **GitHub Releases** section of this repository.

1. Open the **[GitHub Releases Page](https://github.com/AadityaGeek/face-recognition/releases)** on your Android device or computer.
2. Download the latest `LivenessShield.apk` file.
3. Install the APK on your Android device *(Ensure "Install from Unknown Sources" is enabled in system settings if prompted)*.
4. Launch **Liveness Shield**, grant Camera permissions, and begin exploring the enrollment & verification flows!

### System Requirements

- **Operating System**: Android 7.0 (API Level 24) or higher
- **Hardware**: Rear/Front camera required for face verification and QR scanning
- **Connectivity**: Internet connection for cloud-assisted anti-spoofing verification

---

## 🎯 Target Use Cases

- **Access Control Kiosks**: Physical entry verification using digital QR passes and facial confirmation.
- **Remote KYC Onboarding**: Identity verification for financial apps, telecom registration, or digital banking.
- **Portfolio & Client Demonstrations**: A complete, working mobile security application for executive presentations and technical showcases.
- **Identity Research**: Testing ground for passive liveness performance and mobile biometric user experience.

---

## 👨‍💻 Developer & Project Credits

**Liveness Shield** was designed and developed by **Aaditya**.

<div align="center">

| Developer | Portfolio / Profile | Links |
| :--- | :--- | :--- |
| **Aaditya** | Mobile Security & Android Engineer | [![GitHub](https://img.shields.io/badge/GitHub-AadityaGeek-181717?style=flat&logo=github)](https://github.com/AadityaGeek) [![LinkedIn](https://img.shields.io/badge/LinkedIn-aadityakr-0A66C2?style=flat&logo=linkedin)](https://linkedin.com/in/aadityakr) |

</div>

---

## 📄 License & Terms

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for complete details.

<div align="center">

---

**If you find Liveness Shield impressive or helpful for your project, please give it a ⭐ on GitHub!**

*Created with passion by Aaditya • Liveness Shield Security*

</div>
