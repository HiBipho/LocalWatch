# Changelog

All notable changes to this project will be documented in this file.

## [V3.0.0-alpha] - 2026-07-18
### Added
- **Live Emoji Reactions**: Banjir emoji terbang di atas layar secara Real-Time.
- **Picture-in-Picture (PiP)**: Layar film bisa diperkecil agar bisa membalas chat/WhatsApp.
- **Floating Live Chat**: Sistem obrolan melayang (Glassmorphism) transparan di atas video.
- **Auto-Reconnect System**: Anti putus, aplikasi otomatis menyambung kembali 5x jika koneksi WiFi tidak stabil.
- **Riwayat Ruangan (One-Tap Join)**: Mengingat IP Host terakhir untuk bergabung super instan.
- **Dukungan Subtitle (.srt)**: Host bisa memasukkan file subtitle yang akan muncul secara ajaib di layar seluruh penonton.
- **Bioskop Tanpa Gangguan (WakeLock)**: Mencegah layar mati atau meredup selama pemutaran film.
- **Perombakan Visual**: AMOLED Dark Mode dengan sentuhan Neon Cyan dan antarmuka premium.
- **Sistem Katalog V1 (Netflix Style)**: Host bisa memilih satu Folder penuh untuk memunculkan daftar JSON seluruh film `.mp4`/`.mkv`. Klien hanya tinggal duduk manis!

## [2.0.0-alpha] - 2026-07-18
### Changed
- **Massive Architecture Rewrite**: Completely rebuilt the app from React Native to 100% Native Android (Kotlin + Jetpack Compose) to solve unsolvable native plugin crashes on modern Android devices.
- **Networking Engine**: Replaced `react-native-zeroconf` with a custom-built, lightweight UDP Broadcasting engine for room discovery.
- **Media Server**: Replaced `@dr.pogodin/react-native-static-server` with `Ktor` (embedded Netty HTTP server) allowing direct streaming from Android's `ContentResolver`.
- **Video Player**: Upgraded to `androidx.media3` (ExoPlayer) for native video playback.
- **UI**: Recreated the interface using Jetpack Compose.

## [1.0.4-debug] - 2026-07-18 (Deprecated)
- Attempted to inject global JS error catcher. App still crashed at native initialization level.

## [1.0.0 - 1.0.3] (Deprecated)
- Initial React Native prototypes. Abandoned due to native module instability.
