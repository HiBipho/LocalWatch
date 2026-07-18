import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Dimensions } from 'react-native';
import { Video, ResizeMode } from 'expo-av';
import NetworkService from '../services/NetworkService';

const { width } = Dimensions.get('window');

export default function ClientScreen({ onBack }) {
  const [logs, setLogs] = useState([]);
  const [hosts, setHosts] = useState([]);
  const [connectedHost, setConnectedHost] = useState(null);
  const [videoUrl, setVideoUrl] = useState(null);
  const [videoName, setVideoName] = useState('');
  
  const videoRef = useRef(null);

  const addLog = (msg) => setLogs(prev => [...prev, msg]);

  useEffect(() => {
    NetworkService.onHostFound = (service) => {
      setHosts(prev => {
        if (!prev.find(h => h.name === service.name)) {
          return [...prev, service];
        }
        return prev;
      });
    };

    NetworkService.onMessageReceived = (msg) => {
      if (msg.type === 'INIT') {
        setVideoName(msg.fileName);
        // We construct the HTTP URL to stream the video.
        // Assuming the host's HTTP server is running on port 9000
        const hostIp = connectedHost?.host;
        if (hostIp) {
          // Because expo-document-picker uses URI names which might be encoded, we just use the file name
          const url = `http://${hostIp}:9000/${msg.fileName}`;
          addLog(`Setting video URL to: ${url}`);
          setVideoUrl(url);
        }
      } else if (msg.type === 'SYNC') {
        if (videoRef.current) {
          // Manually sync playback
          if (msg.isPlaying) {
            videoRef.current.playAsync();
          } else {
            videoRef.current.pauseAsync();
          }
          
          // Optionally, seek if the position difference is too large (> 1 second)
          // To avoid stuttering, we would normally compare with current position before seeking
          // For MVP, we will just set position if they provide it. We'll do a simple check later.
          // videoRef.current.setPositionAsync(msg.positionMillis);
        }
      }
    };

    NetworkService.startDiscovery(addLog);

    return () => {
      NetworkService.stopDiscovery();
      NetworkService.disconnectClient();
    };
  }, [connectedHost]);

  const connectToRoom = async (host) => {
    try {
      setConnectedHost(host);
      // The host's port in mDNS is the TCP port (9001)
      await NetworkService.connectToHost(host.addresses[0] || host.host, host.port, addLog);
    } catch (error) {
      addLog(`Failed to connect: ${error.message}`);
      setConnectedHost(null);
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.backButton} onPress={onBack}>
        <Text style={styles.backButtonText}>← Back</Text>
      </TouchableOpacity>
      
      <Text style={styles.title}>Join Room</Text>

      {!connectedHost ? (
        <View style={styles.listContainer}>
          <Text style={styles.sectionTitle}>Available Rooms</Text>
          {hosts.length === 0 && <Text style={styles.infoText}>Scanning...</Text>}
          {hosts.map((host, index) => (
            <TouchableOpacity 
              key={index} 
              style={styles.hostCard}
              onPress={() => connectToRoom(host)}
            >
              <Text style={styles.hostName}>{host.name}</Text>
              <Text style={styles.hostIp}>{host.addresses[0] || host.host}</Text>
            </TouchableOpacity>
          ))}
        </View>
      ) : (
        <View style={styles.playerContainer}>
          {videoUrl ? (
            <>
              <Video
                ref={videoRef}
                style={styles.video}
                source={{ uri: videoUrl }}
                resizeMode={ResizeMode.CONTAIN}
                // We disable native controls for the client, so they can't mess with sync
                useNativeControls={false} 
              />
              <Text style={styles.infoText}>Now Playing: {videoName}</Text>
              <Text style={styles.infoTextDim}>Waiting for host to play/pause...</Text>
            </>
          ) : (
            <Text style={styles.infoText}>Waiting for host to load video...</Text>
          )}
        </View>
      )}

      <ScrollView style={styles.logContainer}>
        {logs.map((log, index) => (
          <Text key={index} style={styles.logText}>• {log}</Text>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0F0F0F',
    padding: 24,
  },
  backButton: {
    marginBottom: 24,
    marginTop: 12,
  },
  backButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '500',
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: '#FFFFFF',
    marginBottom: 24,
  },
  listContainer: {
    flex: 1,
  },
  sectionTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '600',
    marginBottom: 12,
  },
  hostCard: {
    backgroundColor: '#1A1A1A',
    padding: 16,
    borderRadius: 8,
    marginBottom: 8,
  },
  hostName: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '500',
  },
  hostIp: {
    color: '#888888',
    fontSize: 12,
    marginTop: 4,
  },
  playerContainer: {
    width: '100%',
    alignItems: 'center',
    marginBottom: 24,
  },
  video: {
    width: width - 48,
    height: (width - 48) * (9/16),
    backgroundColor: '#000000',
    borderRadius: 8,
  },
  infoText: {
    color: '#FFFFFF',
    marginTop: 12,
    fontSize: 16,
    fontWeight: '500',
  },
  infoTextDim: {
    color: '#888888',
    marginTop: 4,
    fontSize: 14,
  },
  logContainer: {
    flex: 1,
    backgroundColor: '#1A1A1A',
    borderRadius: 8,
    padding: 12,
    marginTop: 12,
  },
  logText: {
    color: '#888888',
    fontSize: 12,
    marginBottom: 4,
  }
});
