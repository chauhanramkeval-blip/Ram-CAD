# CAD Mobile Viewer & Editor — Production Build & Release Guide

This document provides step-by-step instructions for building production-ready, signed APKs and Android App Bundles (AAB) for Google Play distribution and direct enterprise deployment.

---

## 1. Release Architecture & Configuration

The application build system in `app/build.gradle.kts` is pre-configured with:
- **Application ID**: `com.aistudio.cadmobile.vweqzt`
- **Target SDK**: Android 16 / API 36 (Backward compatible down to Android 7.0 / API 24)
- **ProGuard / R8 Optimization**: Configured in `app/proguard-rules.pro` to keep Room entities, CAD geometry models, and Compose runtimes while stripping dead code.
- **Dynamic Signing**: Automatically uses custom release keystores when provided via environment variables, with graceful fallback to standard debug signing for local test builds.

---

## 2. Generating a Production Release Keystore

To create a new cryptographic signing key for Google Play or direct distribution:

```bash
keytool -genkey -v \
  -keystore release-upload-key.jks \
  -alias cad-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Store `release-upload-key.jks` in a secure location (or CI/CD secret manager). **Never commit production keystores to public version control.**

---

## 3. Environment Variables for Signed Builds

Set the following environment variables before invoking the Gradle build:

```bash
export KEYSTORE_PATH="/path/to/release-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="cad-upload"
export KEY_PASSWORD="your_key_password"
```

---

## 4. Building Production Artifacts

### A. Android App Bundle (AAB) — Recommended for Google Play Store
Google Play requires `.aab` bundles for new app submissions to enable Dynamic Delivery and optimized APK sizes for each device architecture:

```bash
gradle bundleRelease
```
*Output location*: `app/build/outputs/bundle/release/app-release.aab`

### B. Universal Release APK — For Direct Sideloading / Enterprise Distribution

```bash
gradle assembleRelease
```
*Output location*: `app/build/outputs/apk/release/app-release.apk`

### C. Standard Debug APK — For Rapid Testing

```bash
gradle assembleDebug
```
*Output location*: `app/build/outputs/apk/debug/app-debug.apk`

---

## 5. Verifying Built APKs

To verify signature alignment and v1/v2/v3 signing schemes on your generated APK:

```bash
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

---

## 6. Offline & Permissions Checklist

- **Offline Support**: 100% pure on-device CAD parser and Room database. No mandatory network connectivity required.
- **Permissions**: `READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE` declared for device drawing access, paired with modern Android Storage Access Framework (`ActivityResultContracts.OpenDocument`).
- **Data Safety**: Zero external analytics tracking or cloud telemetry in release builds.
