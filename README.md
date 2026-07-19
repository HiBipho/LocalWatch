# LocalWatch 🎬🍿

**LocalWatch V3.0 (The Ultimate Watch Party)** is a hyper-fast, 100% Native Android application built with **Kotlin and Jetpack Compose**. It allows users on the same local network (WiFi/LAN) to watch movies together in perfect synchronization without needing an active internet connection.

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)
![Architecture](https://img.shields.io/badge/Architecture-Native_Kotlin-7F52FF?style=flat&logo=kotlin)
![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat)
![Server](https://img.shields.io/badge/Server-Ktor_Netty-085C92?style=flat)
![Status](https://img.shields.io/badge/Status-V3.0_Stable-success?style=flat)

---

## 🌟 The V3.0 Experience (Aesthetic & Interactive)

### 1. **Cinematic AMOLED & Glassmorphism UI**
The entire application features a pitch-black AMOLED dark mode designed to save battery and reduce eye strain. Core UI elements like the chat box and control panels utilize a modern **Glassmorphism** (frosted glass) effect with Neon Cyan accents.

### 2. **Netflix-Style Media Catalog**
The Host no longer selects a single file. Instead, the Host selects a **Folder**. The built-in Ktor server automatically scans the folder for `.mp4` and `.mkv` files, presenting a beautiful JSON-powered **Catalog Interface** to all connected clients. Clients can browse the catalog while the Host controls the playback!

### 3. **Floating Live Chat & Emoji Reactions**
- **Live Chat Overlay**: A transparent, floating chat box sits over the top right corner of the video player, allowing real-time communication.
- **Banjir Emoji (Live Reactions)**: Users can tap floating emoji buttons (😂, 😱, 🔥, 😭) that physically float up and across the screens of *everyone in the room* simultaneously!

### 4. **Synchronized Subtitles (.srt)**
The Host can load a `.srt` subtitle file from their device. Through the magic of ExoPlayer and Ktor, the subtitle will instantly and automatically render on all clients' screens perfectly synced with the video.

### 5. **Picture-in-Picture (PiP) Mode**
Need to reply to a WhatsApp message? Tap the **PiP** button and the movie shrinks into a small floating window, maintaining perfect TCP synchronization in the background while you multitask.

### 6. **Rock-Solid Resilience (Auto-Reconnect & WakeLock)**
- **Auto-Reconnect**: If a client's WiFi drops for a moment, the system will silently retry connection up to 5 times.
- **One-Tap Rejoin**: Clients remember the last connected Host IP for instant rejoin without waiting for UDP broadcasts.
- **WakeLock**: The screen will never sleep or dim while the movie is playing.

---

## 🏗️ Architecture & Technology Stack

LocalWatch originally started as a React Native prototype but was completely rebuilt in Native Kotlin to overcome unresolvable plugin issues (Zeroconf limitations on modern Android).

### 1. **Discovery (UDP Broadcast)**
- **Host**: Broadcasts UDP packets on port `9002` announcing the Room Name and IP address every 2 seconds.
- **Client**: Listens on port `9002` for incoming announcements and displays available rooms in a lazy list.

### 2. **Synchronization (TCP Sockets)**
- A robust, low-level TCP Socket Server runs on the Host (Port `9001`).
- Used to broadcast `play`, `pause`, `seek`, `change_video`, `chat`, and `reaction` commands.
- Sub-millisecond latency for perfect playback synchronization.

### 3. **Media Streaming (Ktor & ExoPlayer)**
- **Ktor Netty Server**: Runs directly on the Host's smartphone (Port `8080`). It exposes endpoints:
  - `/catalog`: Returns a JSON array of all videos in the selected folder.
  - `/video?id=X`: Streams the selected video file using HTTP Partial Content (Byte Ranges) for smooth seeking.
  - `/subtitle`: Streams the selected `.srt` file.
- **ExoPlayer (Media3)**: The standard-bearer for Android media playback. Integrates seamlessly with Ktor streams and parses SubRip (`.srt`) dynamically.

---

## 🛠️ How to Build & Run

### Prerequisites
- Android Studio (Flamingo or later)
- JDK 17
- Minimum Android SDK 26 (Android 8.0 Oreo)

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/hibipho/LocalWatch.git

# Enter the directory
cd LocalWatch

# Build the APK
./gradlew assembleDebug
```
The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 Roadmap History

- **V1.0**: React Native prototype (Discontinued due to native module failures).
- **V2.0**: Massive rewrite to Native Android (Kotlin/Compose). Established basic TCP/UDP sync and Ktor streaming.
- **V2.1**: Added Social Identity (Usernames), Welcome Screen, and Host-Controlled Catalog.
- **V2.2**: "The Social & Cinematic Update". Added Glassmorphism UI, Live Chat, and Subtitle support.
- **V2.3**: "Resilience Update". Auto-Reconnect, WakeLock, and PiP.
- **V3.0**: Official Stable Release incorporating all V2.X patches.

*Built with ❤️ for LAN Watch Parties.*
