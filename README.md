# AAOW — Android Apps On Windows

**Run Android `.apk` applications directly on Windows via a native JVM/Java2D Compatibility Layer — no virtual machine, no emulator.**

**Chạy ứng dụng Android (`.apk`) trực tiếp trên Windows thông qua Lớp tương thích JVM/Java2D — không dùng máy ảo, không giả lập hệ điều hành.**

---

## Documentation / Tài liệu Hướng dẫn

- 🇬🇧 **[English User Guide](docs/USER_GUIDE_EN.md)** — Installation, AAOW App Hub, window controls, CLI reference, compatibility & troubleshooting.
- 🇻🇳 **[Hướng dẫn Sử dụng Tiếng Việt](docs/USER_GUIDE_VI.md)** — Cài đặt, sử dụng AAOW App Hub, phím tắt cửa sổ, lệnh CLI, khả năng tương thích & xử lý lỗi.
- 🗺️ **[Project Roadmap (VI)](docs/ROADMAP.md)** — Full 13-sprint roadmap (Phase 1 Foundation & Packaging, Phase 2 Real-World APKs).
- 📋 **[Sprint Acceptance Log (VI)](docs/SPRINT_LOG.md)** — Detailed verification metrics, benchmark results, and honest limitations for every sprint.
- 🏛️ **[Architecture Spike](docs/ARCHITECTURE_SPIKE.md)** & **[Native ABI Spike](docs/NATIVE_ABI_SPIKE.md)**.

---

## Quick Start / Bắt đầu Nhanh

### End Users (Windows 10/11 x64 — No JDK Required)
1. Download or build `build\release\AAOW-Setup-1.0.0.exe` (or extract `AAOW-Portable-1.0.0.zip`).
2. Run the installer to install `AAOW.exe` (GUI), `aaow-cli.exe` (CLI), and optional `.apk` double-click file association.
3. Launch **AAOW App Hub**, drag and drop any `.apk` file into the window, and click **Run App (`Chạy ứng dụng →`)**.

### From Source (JDK 21+)
```powershell
# Open AAOW App Hub
.\aaow.bat

# Run a specific APK directly
.\aaow.bat samples\aaow-2048.apk

# Analyze static API compatibility before running
.\aaow.bat --scan path\to\app.apk

# Rebuild fast distribution after editing code
.\aaow.bat --rebuild

# Run automated JUnit 5 test suite
.\gradlew.bat test

# Package standalone Windows EXE & installer (AAOW-Setup-1.0.0.exe)
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

---

## Architecture Overview / Tổng quan Kiến trúc

AAOW translates Android APKs directly onto Windows using **Java 21** across three Gradle modules:

| Module | Responsibility | Key Components |
|---|---|---|
| `:apk` | Binary APK parsing & verification | `ApkFile`, `AxmlParser`, `ResourceTable`, `AndroidManifest`, `ElfFile`, `ApkSignatureExtractor` (v1/v2/v3 X.509) |
| `:framework` | Re-implemented `android.*` & `microG-lite` runtime | Java2D/Swing `View`/`Canvas`, `Fragment`, `SQLiteDatabase` (SQLite JDBC), JavaFX 21 WebKit `WebView`, `ImageDecoder` + `imageio-webp`, Windows Audio/Tray/Clipboard/File Picker/Browser bridges, `NativeLibraryManager` + JNI HLE + `Arm64JniInterpreter`, `microG-lite` AIDL Binder |
| `:launcher` | Bytecode translation, App Hub & Windows integration | `DexR8Sanitizer`, `DexLoader` (`dex2jar` + parallel ASM), `ApkDexClassLoader`, `AndroidSdkFallbackClassLoader`, `ApkCompatibilityScanner`, `AppRepository`, `AppHubWindow`, `WindowsIntegration` |

---

## Project Status (13 / 13 Planned Sprints Complete)

- **Phase 1 (Sprints 1–8):** Binary APK decoder, DEX→JVM translator, App Hub multi-window manager, `ApkCompatibilityScanner`, UI/Fragment/Animation framework, SQLite JDBC/ContentProvider/Services, JavaFX WebView/Audio/Windows OS bridges, JNI HLE + ARM64 user-mode interpreter + `microG-lite`, 60fps LRU resource caching, and standalone Windows installer (`AAOW-Setup-1.0.0.exe`).
- **Phase 2 (Sprints 9–13):**
  - `DexR8Sanitizer` eliminating R8/Kotlin `VerifyError` on modern AndroidX APKs.
  - End-to-end verified Offline F-Droid apps (`Editor`, `Notes`, `Bubble Level`, `Scrambled Exif`, `Privacy Friendly Notes` with Room SQLite).
  - Real-time Non-GMS Online & Media apps (`Currency`, `Forecastie` with OkHttp3/WilliamChart/Leaflet, `OMWeather/Cirrus` with Volley/OSMDroid).
  - Early GMS/Firebase initialization, real X.509 certificate extraction, 4-byte aligned `Parcel`/`SafeParcel`, and commercial APK bring-up (`com.apkpure.aegon.apk` transitioning past `SplashActivity` into `MainTabActivity` with 97.4% call-site framework coverage).
  - Animated 60fps APK loading screen (`HostWindow`), non-blocking App Hub install progress bar (`AppHubWindow`), and boot-gated diagnostics (diagnostic report is never auto-shown before the app actually boots).
