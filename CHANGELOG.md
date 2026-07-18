# Changelog

All notable changes to this project will be documented in this file.

## [1.0.4-debug] - 2026-07-18
### Added
- **Global Error Handler:** Injected `ErrorUtils.setGlobalHandler` to catch fatal JS exceptions on launch and display them as an Alert box rather than silently crashing.
## [1.0.3] - 2026-07-18
### Fixed
- **Startup Crash:** Disabled React Native's "New Architecture" (`newArchEnabled=false`). The newer architecture is incompatible with older native plugins like `react-native-zeroconf` and was causing a fatal JS runtime error immediately upon launch on physical devices.
## [1.0.2] - 2026-07-18
### Fixed
- **Force Close Bug:** Disabled overly aggressive ProGuard minification (`minifyEnabled=false`) which was stripping native P2P networking components during release builds.
### Changed
- **UI/UX:** Replaced default Expo icons with a custom premium, neon-styled dark mode icon.
- **Build System:** Fixed `Duplicate Resources` AAPT error during `assembleRelease` by gracefully handling `.webp` and `.png` asset generation.

## [1.0.1] - 2026-07-18
### Changed
- **App Size Reduction:** Enabled Android ABI Splits (`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) to eliminate universal APK bloat. Reduced APK size by ~70% (from 102 MB down to 31 MB).

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
