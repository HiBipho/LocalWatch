# 🌌 OmniSync Framework (LocalWatch V4.0)

## 1. Konsep Utama (The Grand Vision)
**OmniSync** adalah revolusi dari LocalWatch V3.0. Jika V3.0 mengandalkan satu *Host* sebagai pusat (Client-Server TCP), V4.0 akan menggunakan arsitektur **Decentralized Mesh Network** dipadukan dengan **WebRTC Data Channels**. 

Tujuannya adalah: *Cross-Platform, Global Reach, & Infinite Scalability*.
Tidak hanya di WiFi lokal, tapi bisa melintasi benua dengan latensi sub-millisecond, mendukung *Spatial Audio*, dan *Walkie-Talkie* terintegrasi.

## 2. Cara Kerja (Mechanics)
- **Signaling Server (Minimalis):** Digunakan hanya di awal untuk pertukaran *SDP (Session Description Protocol)* antar pengguna.
- **P2P Mesh Topology:** Setiap perangkat (Node) terhubung ke perangkat lain. Jika *Host* utama terputus, algoritma **Raft Consensus** akan otomatis memilih *Host* baru dalam waktu kurang dari 50ms (Zero Downtime).
- **Time-Drift Sync Algorithm:** Algoritma internal yang menghitung `(Network_RTT / 2) + Processing_Delay` untuk memastikan video diputar pada *frame* yang sama persis hingga tingkat *mikro-detik* di seluruh perangkat.

## 3. Komposisi UI/UX (Desain Estetika)
### Tema: "Cyber-Glass"
- **Background Utama:** AMOLED Pure Black (`#000000`).
- **Aksen Primer (Neon):** Cyber Cyan (`#00F0FF`) & Hot Pink (`#FF003C`) untuk *states* (Pause/Play).
- **Glassmorphism Formula:** 
  - Latar Belakang: `rgba(255, 255, 255, 0.05)`
  - Blur Radius: `24px`
  - Border: `1px solid rgba(0, 240, 255, 0.3)`
- **Tipografi:** *Inter* untuk UI, *Space Mono* untuk *Timecodes*.

### UX Flow Baru
- **Haptic Feedback:** Setiap *Emoji Reaction* yang mengapung ke layar akan disertai getaran *haptic* mikro yang disinkronisasi dengan nada suara di film.
- **Dynamic PiP 2.0:** Layar mengecil secara *seamless* tanpa layar hitam (flicker-free transition).

## 4. Fundamental Arsitektur (Clean Architecture)
Mengikuti *Uncle Bob's Clean Architecture*:
1. **Domain Layer:** Entitas inti seperti `Room`, `Peer`, `MediaState`, `SyncClock`.
2. **Use Case Layer:** `ElectNewHost`, `CalculateTimeDrift`, `BroadcastReaction`.
3. **Interface Adapters:** `WebRtcController`, `MediaPlayerController`.
4. **Infrastructure Layer:** Jetpack Compose UI, ExoPlayer (Media3), WebRTC Native C++ Libraries.

## 5. Algoritma Kunci: Precision Time Sync (PTS)
Untuk memastikan tidak ada "Echo" saat semua HP memutar suara yang sama di satu ruangan.
```kotlin
fun calculateTruePlaybackPosition(
    hostPosition: Long, 
    hostTimestamp: Long, 
    localTimestamp: Long, 
    rtt: Long
): Long {
    val networkDelay = rtt / 2
    val timePassed = localTimestamp - hostTimestamp
    return hostPosition + timePassed + networkDelay
}
```
*(Algoritma ini akan terus menerus mengoreksi 'Drift' pemutar video setiap 5 detik dengan Micro-Adjustments).*

---
*Dokumen ini adalah cetak biru untuk masa depan LocalWatch. Pengujian teknis untuk Precision Time Sync (PTS) sedang berjalan di laboratorium sistem.*
