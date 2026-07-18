# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-07-18
### Added
- **Core Engine:** Initialized React Native Expo project.
- **Network Discovery:** Implemented `react-native-zeroconf` for mDNS auto-discovery (Host broadcasting and Client scanning) on local networks.
- **Media Server:** Integrated `@dr.pogodin/react-native-static-server` to serve MP4 video files locally from the Host device via HTTP.
- **Sync Engine:** Created a fast TCP socket server (`react-native-tcp-socket`) for low-latency synchronization of `Play`, `Pause`, and `Seek` commands between Host and Clients.
- **Host Screen:** Added UI to pick a local video file (`expo-document-picker`), host it, and control playback (`expo-av`).
- **Client Screen:** Added UI to automatically discover available rooms, connect seamlessly, and stream synchronized video playback.
- **UI/UX:** Implemented a minimal, premium Dark Mode aesthetic.
- **Permissions:** Configured `app.json` with required Android permissions (`INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `READ_EXTERNAL_STORAGE`) and cleartext traffic bypass.
- **Release:** Built and distributed the first `app-release.apk` (MVP).
