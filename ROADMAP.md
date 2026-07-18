# LocalWatch - Roadmap & Documentation

Welcome to the internal documentation for **LocalWatch**. This document outlines the technical flow of the application, current limitations, and the exciting roadmap for future updates.

---

## 📖 Technical Documentation

### 1. Architecture Overview
LocalWatch relies on three main pillars running entirely locally on mobile devices without relying on external servers or the internet.
- **mDNS (Multicast DNS):** Used for auto-discovery. The Host broadcasts a service named `localwatch` (`_localwatch._tcp.local.`). Clients scan the local network for this service to find the Host's IP address.
- **HTTP Static Server:** The Host runs a lightweight HTTP server on port `9000`. This server exposes the directory containing the chosen `.mp4` video so that Clients can stream it using a standard URL (`http://<HOST_IP>:9000/video.mp4`).
- **TCP Socket (Sync Engine):** The Host runs a raw TCP server on port `9001`. Clients connect to this port to receive a constant stream of JSON messages (e.g., `{"type":"SYNC", "isPlaying":true, "positionMillis":120500}`) dictating exactly when to play, pause, or seek.

### 2. File Structure
- `src/services/NetworkService.js`: The core engine handling mDNS, TCP Server/Client, and HTTP Static Server.
- `src/screens/HostScreen.js`: UI and logic for the Host (Picking video, initiating servers, broadcasting sync).
- `src/screens/ClientScreen.js`: UI and logic for the Client (Scanning for Hosts, receiving sync, playing video).
- `App.js`: Main entry point and minimalist dark-mode routing.

---

## 🚀 What's Next? (Roadmap for v2.0)

While Version 1.0 (MVP) successfully proves the core concept of offline P2P streaming, here are the targeted features for the next major release:

### Phase 1: Enhanced Media Library (Jellyfin Style)
- [ ] **Folder Scanning:** Instead of picking one video at a time, allow the Host to select an entire folder.
- [ ] **Media Catalog UI:** Parse video metadata and display a Netflix-like grid of available movies on the Host and Client screens.
- [ ] **Client Selection:** Allow Clients to browse the Host's catalog and request to watch a specific movie.

### Phase 2: Interactivity & Social
- [ ] **Local Chat Room:** Add a lightweight chat interface synchronized via the existing TCP socket, allowing users to type messages during the movie.
- [ ] **Client Controls:** Add an option for the Host to grant "Remote Control" to a Client, allowing anyone in the room to pause or seek the video.
- [ ] **Audio-Only Mode:** Support for `.mp3` and `.flac` files for synchronized music listening parties.

### Phase 3: Stability & Optimization
- [ ] **Format Support:** Integrate a more robust media player engine (like VLC core/Media3) to support `.mkv`, `.avi`, and subtitles (`.srt`).
- [ ] **Buffer Management:** Implement a smarter sync algorithm that calculates network latency (ping) and slightly pre-buffers the video before playing to ensure millisecond-perfect audio sync across devices.
- [ ] **Background Execution:** Keep the Host server alive even if the screen turns off or the app is minimized (Requires Android Foreground Service).

---

## 🛠️ Contributing & Issues
If you find any bugs (e.g., video stuttering, connection drops on specific routers), please open an **Issue** on the GitHub repository with the model of your Android device and the Android version.
