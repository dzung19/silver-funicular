# Smart Home Core Hub 🏠📱

An architecture demonstration of a **multi-process Android application** built with Kotlin, Jetpack Compose, Coroutines/Flow, AIDL/Binder IPC, and Protocol Buffers.

---

## 🏗️ Architecture Overview

The application is split into two isolated Linux processes to simulate real-world hardware controller isolation:

```
┌─────────────────────────────────────────────────────────────┐
│                   Main Process (:main)                      │
│                                                             │
│   ┌──────────────────┐          ┌───────────────────────┐   │
│   │  Jetpack Compose │ ◄─────── │     MainViewModel     │   │
│   │    UI Screen     │          └───────────┬───────────┘   │
│   └──────────────────┘                      │               │
│                                             ▼               │
│                                 ┌───────────────────────┐   │
│                                 │   IotClientManager    │   │
│                                 │ (ServiceConnection)   │   │
│                                 └───────────┬───────────┘   │
└─────────────────────────────────────────────┼───────────────┘
                                              │
                                     AIDL / Binder IPC
                                  Protobuf byte[] payloads
                                              │
┌─────────────────────────────────────────────┼───────────────┐
│              Core Service Process (:iot_core)               │
│                                             ▼               │
│                                 ┌───────────────────────┐   │
│                                 │    CoreIotService     │   │
│                                 │     (IIotHub.Stub)    │   │
│                                 └───────────┬───────────┘   │
│                                             │               │
│                                             ▼               │
│                                 ┌───────────────────────┐   │
│                                 │   FakeBleRepository   │   │
│                                 │  (Flow Telemetry 3s)  │   │
│                                 └───────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Process Breakdown

1. **Client UI Process (`com.dzungphung.smarthome`)**:
   - Runs the Jetpack Compose user interface on the main thread.
   - Binds to `CoreIotService` via `ServiceConnection` and AIDL proxy (`IIotHub`).
   - Observes real-time telemetry converted into `StateFlow<DeviceState?>`.

2. **Core Service Process (`com.dzungphung.smarthome:iot_core`)**:
   - Configured with `android:process=":iot_core"` in `AndroidManifest.xml`.
   - Runs in the background as an isolated process.
   - Houses the `FakeBleRepository` which emits simulated BLE temperature data every 3 seconds.
   - Hosts the `IIotHub.Stub` AIDL implementation and broadcasts state updates thread-safely via `RemoteCallbackList`.

---

## 🛠️ Tech Stack

| Component | Technology | Description |
|---|---|---|
| **Language** | Kotlin 2.2.10 | Modern concise Kotlin with strict null-safety |
| **UI Framework** | Jetpack Compose | Declarative UI powered by Material 3 |
| **Asynchronous Programming** | Kotlin Coroutines & Flow | Asynchronous streams, state management (`StateFlow`) |
| **IPC Mechanism** | AIDL & Binder | Android Interface Definition Language over raw Binder driver |
| **Data Serialization** | Protocol Buffers (Protobuf) | `protobuf-javalite` binary payload transport across process boundary |
| **Architecture Pattern** | Clean Architecture / MVVM | Decoupled UI, ViewModel, Repository, and IPC layers |
| **CI / CD** | GitHub Actions | Automated build pipeline compiling debug APKs |

---

## 📁 Project Structure

```
MyApplication/
├── .github/
│   └── workflows/
│       └── android.yml              # GitHub Actions CI workflow
├── app/
│   ├── build.gradle.kts             # Module build script (Protobuf & AIDL configured)
│   └── src/
│       └── main/
│           ├── aidl/
│           │   └── com/dzungphung/smarthome/
│           │       ├── IIotCallback.aidl   # AIDL interface for client callbacks
│           │       └── IIotHub.aidl        # AIDL interface for service commands & registration
│           ├── java/
│           │   └── com/dzungphung/smarthome/
│           │       ├── core/
│           │       │   ├── CoreIotService.kt   # Service running in :iot_core process
│           │       │   └── FakeBleRepository.kt # Mock BLE hardware emitting telemetry
│           │       ├── ui/                 # Jetpack Compose theme & UI screens
│           │       └── MainActivity.kt
│           ├── proto/
│           │   └── device_message.proto    # Protobuf definitions (DeviceState & DeviceCommand)
│           └── AndroidManifest.xml         # Service process declaration (:iot_core)
├── gradle/
│   └── libs.versions.toml           # Version catalog for dependencies and plugins
├── build.gradle.kts                 # Root project build file
├── settings.gradle.kts
└── README.md
```

---

## 📋 Implementation Roadmap

- [x] **Step 1: Setup & Contracts (Protobuf + AIDL)**
  - Protobuf plugin and `javalite` runtime setup.
  - Defined `DeviceState` and `DeviceCommand` messages in `device_message.proto`.
  - Defined `IIotHub.aidl` and `IIotCallback.aidl` contracts.
  - GitHub Actions CI workflow created.

- [x] **Step 2: Mock Hardware & Core Service (`:iot_core`)**
  - Created `FakeBleRepository` emitting periodic telemetry (20-30°C).
  - Implemented `CoreIotService` running under `android:process=":iot_core"`.
  - Used `RemoteCallbackList` for thread-safe multi-process callback broadcasting.
  - Protobuf byte array serialization across process boundary.

- [ ] **Step 3: Client Connection Manager**
  - Create `IotClientManager` for binding to `CoreIotService`.
  - Expose `StateFlow<DeviceState?>` for UI observation.
  - Handle `RemoteException` and Binder death recovery (`DeathRecipient`).

- [ ] **Step 4: UI Implementation (Jetpack Compose)**
  - `MainViewModel` integrating `IotClientManager`.
  - Jetpack Compose screen showing connection status, real-time temperature, and command toggle switch.

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or JDK 21
- Android SDK 36 (Min SDK 28)

### Build Command
```bash
./gradlew assembleDebug
```

### GitHub Actions Artifacts
Every push to `main` automatically triggers a build and attaches the resulting `app-debug.apk` under the Actions tab:
🔗 [GitHub Repository Actions](https://github.com/dzung19/silver-funicular/actions)
