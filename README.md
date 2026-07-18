# LocalWatch

Watch together, offline. 

LocalWatch is a Peer-to-Peer (P2P) local streaming application that allows you to host watch parties over a local network (WiFi or Hotspot) without requiring an internet connection. It is the perfect solution for zero-latency, offline watch parties with your friends.

## Features (v1.0.0 MVP)
- **Zero Internet Required:** Streams entirely over your local LAN or Hotspot.
- **Auto-Discovery:** Clients automatically detect hosts in the same network via mDNS (Zeroconf).
- **Synchronized Playback:** Play, Pause, and Seek events are instantly synchronized from the Host to all Clients via a low-latency TCP socket.
- **Built-in Media Server:** The Host device automatically spins up a local HTTP server to serve the selected MP4 video.
- **Premium Dark Mode:** Minimalist, cinema-like user interface.

## How It Works
1. Connect all mobile devices to the same WiFi network or Hotspot.
2. The **Host** opens the app, selects "Host a Room," and picks an MP4 video file from their storage.
3. The **Clients** open the app, select "Join a Room," and simply tap on the detected Host's room.
4. The Host has full control over the playback. When the Host presses play, the movie starts for everyone simultaneously!

## Tech Stack
- **Framework:** React Native (Expo)
- **Media Player:** `expo-av`
- **Network Sync:** `react-native-tcp-socket`
- **Local Server:** `@dr.pogodin/react-native-static-server`
- **Discovery:** `react-native-zeroconf`

## Installation
You can download the latest APK from the [Releases](https://github.com/HiBipho/LocalWatch/releases) tab.

## Building from source
```bash
npm install
npx expo prebuild -p android --clean
cd android
./gradlew assembleRelease
```
