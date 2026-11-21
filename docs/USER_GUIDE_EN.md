# AAOW User Guide (Android Apps On Windows)

🌐 **Language:** **English** | [Tiếng Việt](USER_GUIDE_VI.md)

**AAOW (Android Apps On Windows)** runs Android `.apk` files natively on Windows through a **Java 21 Compatibility Layer** (Java2D / Swing + `dex2jar` / ASM) — **no virtual machine and no Android emulator**. Each Android app runs inside its own lightweight Windows desktop window.

---

## Table of Contents
1. [System Requirements](#1-system-requirements)
2. [Installation & Launch](#2-installation--launch)
3. [Using AAOW App Hub](#3-using-aaow-app-hub)
4. [Android Window Controls & Keyboard Shortcuts](#4-android-window-controls--keyboard-shortcuts)
5. [Command-Line Interface (CLI) Reference](#5-command-line-interface-cli-reference)
6. [Data Storage, DEX Cache & Cleanup](#6-data-storage-dex-cache--cleanup)
7. [Compatibility Scope, Limitations & Troubleshooting](#7-compatibility-scope-limitations--troubleshooting)
8. [For Developers](#8-for-developers)

---

## 1. System Requirements

- **OS:** Windows 10 or Windows 11 (64-bit).
- **End Users (using `.exe` Installer or Portable `.zip`):** **No pre-installed JDK/Java required** — the distribution bundles a trimmed Java 21 runtime built with `jlink`.
- **Running from Source:** **JDK 21** is required. Having an Android SDK installed (`platforms/android-*/android.jar`) is recommended so `AndroidSdkFallbackClassLoader` and `ApkCompatibilityScanner` can automatically de-stub uncommon framework classes.

---

## 2. Installation & Launch

### Option 1: Windows Installer (`AAOW-Setup-1.0.0.exe`) — Recommended
1. Run `AAOW-Setup-1.0.0.exe` (located in `build\release\` after packaging).
2. Choose an installation folder (default: `%LOCALAPPDATA%\AAOW`, no Administrator privileges required).
3. Select optional integrations:
   - **Associate `.apk` files:** Double-click any `.apk` in Windows Explorer to launch it directly with AAOW (or right-click → **Install to AAOW App Hub**).
   - **Create Desktop and Start Menu shortcuts.**
   - **Launch AAOW App Hub immediately after installation.**
4. Click **Install**. To uninstall later, use *Windows Settings → Apps → Installed apps* or run `Uninstall-AAOW.exe` in the installation folder.

**Silent Install / Uninstall flags (`AAOW-Setup-1.0.0.exe`):**
```powershell
# Silent install to a custom directory
.\AAOW-Setup-1.0.0.exe /S /D="D:\Apps\AAOW"

# Silent install without .apk association or shortcuts
.\AAOW-Setup-1.0.0.exe /S /NOASSOC /NOSHORTCUT

# Silent uninstall
.\Uninstall-AAOW.exe /UNINSTALL /S
```

### Option 2: Portable Archive (`AAOW-Portable-1.0.0.zip`)
Extract `AAOW-Portable-1.0.0.zip` to any folder and run **`AAOW.exe`** (GUI) or **`aaow-cli.exe`** (console CLI).

### Option 3: Run from Source Repository
From the project root directory in PowerShell or Command Prompt:
```powershell
.\aaow.bat
```
*(The first run automatically builds `:launcher:installDist`; subsequent launches start in ~1–2 seconds without Gradle daemon overhead.)*

---

## 3. Using AAOW App Hub

**AAOW App Hub** is the desktop control center for managing installed `.apk` apps:

- **Installing APKs:**
  - **Drag and drop** one or more `.apk` files from Windows Explorer directly onto the App Hub window; or
  - Click **`+ Cài đặt APK...`** (`+ Install APK`) in the top toolbar to pick `.apk` files; or
  - Click **`Nạp bộ APK`** (`Batch Import APKs`) to import every `.apk` from a directory (auto-detects `build\benchmark-apks` or `offline-apks` if present).
  - An animated progress banner appears at the top of the list while AAOW extracts metadata and pre-translates DEX bytecode into JVM classes in the background without freezing the UI.
- **Search & Filter:** Type an app name or package ID into the search box to filter cards live.
- **App Card Actions:**
  - **Run App (`Chạy ứng dụng →`):** Launches the app in an independent Windows frame (supports running multiple Android apps side by side). While compiling/loading, the card displays **`◌ ĐANG TẢI...`** (*Loading*, with **View loading window** / **Cancel** buttons); once booted, it switches to **`● ĐANG CHẠY`** (*Running*, with **Bring to front** / **Stop** buttons).
  - **Inspect APK (`Soi APK`):** Displays package metadata, SDK targets, Activity list, declared permissions, native ABIs, and the decoded binary `AndroidManifest.xml` in the right pane.
  - **Scan API (`Quét API`):** Runs static bytecode compatibility analysis (Class / Unique Method / Call-Site coverage and Top 20 missing APIs) directly in the inspector pane.
  - **Clear Data (`Xóa dữ liệu`):** Wipes `SharedPreferences`, SQLite databases, caches, and private app files for that package.
  - **Uninstall (`Gỡ cài đặt`):** Stops the app if running, removes the `.apk` from the repository, and deletes its app data.
- **`.apk` Association Toggle (`Liên kết .apk` / `✓ Đã liên kết .apk`):** Registers or removes `.apk` double-click handling in Windows Explorer under `HKCU\Software\Classes` (no Admin rights needed).

---

## 4. Android Window Controls & Keyboard Shortcuts

### Animated APK Loading Screen & Boot Gating
When launching an APK, the host window opens immediately with a 60fps dual-ring spinner, shimmer progress bar, and 4-stage indicator (**`APK` → `DEX` → `Runtime` → `UI`**).
- **No premature missing-resource screen:** AAOW waits on the loading screen until the app actually renders real UI content or opens its first dialog (preventing false diagnostic screens while a `SplashActivity` is still transitioning or initializing asynchronously).
- If launch fails fatally or waits longer than 10 seconds without UI content, optional **`Xem Báo cáo Tương thích`** (*View Compatibility Report*) / **`Xem chẩn đoán`** (*View Diagnostics*) and **`Đóng`** (*Close*) buttons appear on the loading screen.

### Top Desktop Toolbar
- **`◀`**: Back action (equivalent to Android Back).
- **`↻ Ngang` / `↻ Dọc`**: Toggle Landscape / Portrait orientation and dispatch `onConfigurationChanged`.
- **Zoom (`Tự do (1:1)`, `75%`, `100%`, `125%`, `150%`)**: In `Tự do (1:1)` mode, resizing the Windows frame dynamically relayouts the Android viewport.
- **`Soi View` (`F12`)**: Toggle the **AAOW Live Inspector** side panel showing live View hierarchy, bounds, stub diagnostics, and package metadata.
- **Right Status Readout:** Live FPS, frame render duration (`ms`), and JVM heap usage (`MB`).

### Bottom Android Navigation Bar
- **Left Triangle (Back):** Dismisses active dialogs/menus or pops the Activity back stack.
- **Center Circle (Home):** Closes child Activities to return to the root Activity.
- **Right Square (Overview/Inspector):** Toggles the Live View Inspector panel.

### Keyboard Shortcuts
| Shortcut | Action |
|---|---|
| `Esc` | Android Back (closes active dialog or finishes top Activity) |
| `Ctrl + R` | Rotate screen Portrait ↔ Landscape |
| `Ctrl + =` / `Ctrl + +` | Zoom in (+25%, up to 200%) |
| `Ctrl + -` | Zoom out (-25%, down to 50%) |
| `Ctrl + 0` | Reset zoom to `Tự do (1:1)` responsive window sizing |
| `F12` | Toggle **AAOW Live Inspector** (`Soi View`) |
| `Arrow Keys` / `W A S D` / `Enter` | Dispatched as Android DPAD / Enter `KeyEvent` when no text input is focused |
| `Ctrl + C` / `Ctrl + X` / `Ctrl + V` | Two-way clipboard sync between `EditText` and Windows Clipboard |

### Windows OS Integration
- **File Picker:** `ACTION_GET_CONTENT`, `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`, and `ACTION_OPEN_DOCUMENT_TREE` open native Windows file/folder dialogs.
- **File Sharing (`ACTION_SEND` / `ACTION_CHOOSER`):** Copies shared output files into the `shared-out` directory inside AAOW's data root.
- **External Links:** `ACTION_VIEW` with `http://` or `https://` launches the default Windows web browser.
- **Notifications:** `NotificationManager` routes notifications to Windows `SystemTray`.

---

## 5. Command-Line Interface (CLI) Reference

Invoke via `.\aaow.bat` (source workspace) or `aaow-cli.exe` (installed / portable build):

| Command | Description |
|---|---|
| `aaow.bat` or `aaow.bat --hub` | Open **AAOW App Hub** |
| `aaow.bat <path.apk> [ActivityClass]` | Launch an APK directly (with optional target Activity class) |
| `aaow.bat --install <file.apk\|dir>` | Install APK(s) into repository, pre-compile DEX cache, and open App Hub |
| `aaow.bat --install-silent <file.apk\|dir>` | Install APK(s) silently without opening GUI |
| `aaow.bat --scan <file.apk>` | Run static compatibility scan (Class, Method, Call-Site coverage & Top 20 missing APIs) |
| `aaow.bat --info <file.apk> [--xml]` | Print APK summary; add `--xml` to dump decoded `AndroidManifest.xml` (AXML) |
| `aaow.bat --associate-apk` | Register `.apk` double-click association in Windows Explorer (`HKCU`) |
| `aaow.bat --unassociate-apk` | Remove `.apk` file association from Windows Explorer |
| `aaow.bat --check-associate-apk` | Print `REGISTERED` or `NOT_REGISTERED` |
| `aaow.bat --screenshot <apk> <out.png> [Act]` | Render headless viewport screenshot (`420x820`) to PNG |
| `aaow.bat --screenshot-hub <out.png>` | Render headless App Hub screenshot (`1080x700`) to PNG |
| `aaow.bat --data-dir <dir>` | Use a custom data/cache root directory for this run |
| `aaow.bat --rebuild [other-args...]` | *(`aaow.bat` only)* Rebuild `:launcher:installDist` first, then run remaining args |
| `aaow.bat --help` (or `-h`) | Print CLI usage |

---

## 6. Data Storage, DEX Cache & Cleanup

Depending on how AAOW is launched, runtime data is stored under:
- **Standalone / Installed (`AAOW.exe` / `aaow-cli.exe`):** `%LOCALAPPDATA%\AAOW\data` (fallback: `~/.aaow/data`).
- **Source Workspace (`aaow.bat`):** `build\aaow-data`.

Directory layout:
- `installed-apps\`: Installed `.apk` copies managed by App Hub.
- `dex-cache\`: Translated JVM `.jar` caches (`*-v12.jar`), external reference indexes (`.refs`), and cached 64×64 icons (`icons\`).
- `<packageName>\`: Per-app sandbox (`files\`, `cache\`, `shared_prefs\`, `databases\`, `lib\`).
- `sdcard\`: Shared external storage (`Documents\`, `Notes\`, `Pictures\`, `Download\`, etc.).
- `shared-out\`: Exported files from Android `ACTION_SEND` share intents.

> **Tip:** You can safely delete `dex-cache\` at any time to force fresh DEX bytecode translation on the next launch, or use **`Xóa dữ liệu`** (*Clear Data*) in App Hub to reset a specific app.

---

## 7. Compatibility Scope, Limitations & Troubleshooting

### What Works Well
- **Pure Java / Kotlin / AndroidX / Material Apps:** Text editors, note-taking apps, utilities using SQLite / Room, `RecyclerView`, `ViewPager`, `DrawerLayout`, `Fragment`, `Dialog`.
- **Networked & Media Apps (Non-GMS):** Apps using `HttpURLConnection`, `OkHttp3`, `Volley`, `Glide`/`Picasso`/`Coil` (including WebP decoding via `imageio-webp`), embedded JavaFX WebKit `WebView`, and 2D charts/maps.
- **Supported Commercial & HLE Patterns:** Early `FirebaseInitProvider` initialization, real APK X.509 v1/v2/v3 certificate parsing, `microG-lite` AIDL binders, and High-Level Emulation (HLE) for common native JNI libraries (`MMKV`, `WebP`, `zlib`, `MemFile`, `SQLite`, `QSea`/`QMSP`) plus a pure-Java `arm64-v8a` interpreter for small native routines.

### Known Architectural Limitations
- **3D Games / Heavy Graphics Engines:** OpenGL ES and Vulkan rendering pipelines are not implemented (AAOW renders Android UI and 2D Canvas via Java2D).
- **Complex Native `.so` Binaries:** Large C/C++ libraries relying on Linux/Bionic syscalls, anti-tamper checks, or hardware media codecs outside the HLE handlers may fall back to safe defaults or stop some background features.
- **Authenticated Google Services:** Real Google Sign-In OAuth, Play Billing, Play Integrity / SafetyNet attestation, and proprietary Google Maps SDK tiles cannot be verified against Google servers via `microG-lite`.

### Troubleshooting an APK
1. **Scan statically first:** Click **`Quét API`** in App Hub or run `.\aaow.bat --scan app.apk` to inspect class/method/call-site coverage, required native `.so` ABIs, and missing APIs.
2. **Open Diagnostics manually:** If an app fails to boot or stays on the loading screen past 10 seconds, click **`Xem Báo cáo Tương thích`** / **`Xem chẩn đoán`** on the overlay, or press **`F12`** (`Soi View`) to inspect the stack trace, called stubs, and test launching alternative Activities declared in the manifest.

---

## 8. For Developers

- **Run full JUnit 5 test suite (including `android.jar` superclass hierarchy check):**
  ```powershell
  .\gradlew.bat test --rerun-tasks --console=plain
  ```
- **Rebuild fast CLI/Hub distribution after code changes:**
  ```powershell
  .\aaow.bat --rebuild
  ```
- **Rebuild bundled sample APKs (`samples\aaow-showcase.apk`, `samples\aaow-2048.apk`):**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\build-sample-apk.ps1
  ```
- **Package standalone `AAOW.exe`, `AAOW-Portable-1.0.0.zip`, and `AAOW-Setup-1.0.0.exe`:**
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
  ```
  *(Requires JDK 21 with `jlink.exe` & `jpackage.exe`, plus .NET Framework 4 `csc.exe` included with Windows 10/11.)*
