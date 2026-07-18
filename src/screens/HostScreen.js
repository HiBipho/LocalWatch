import React, { useState, useEffect, useRef } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Dimensions } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import { Video, ResizeMode } from 'expo-av';
import NetworkService from '../services/NetworkService';

const { width } = Dimensions.get('window');

export default function HostScreen({ onBack }) {
  const [videoFile, setVideoFile] = useState(null);
  const [logs, setLogs] = useState([]);
  const [clients, setClients] = useState([]);
  const videoRef = useRef(null);
  const clientsRef = useRef([]);

  const addLog = (msg) => setLogs(prev => [...prev, msg]);

  useEffect(() => {
    NetworkService.onClientConnected = (socket) => {
      clientsRef.current.push(socket);
      setClients([...clientsRef.current]);
      
      // When a new client connects, let them know what file we are hosting
      if (videoFile) {
        const fileName = videoFile.name || 'video.mp4';
        socket.write(JSON.stringify({ type: 'INIT', fileName }));
      }
    };

    return () => {
      NetworkService.stopHost();
    };
  }, [videoFile]);

  const pickVideo = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({ type: 'video/mp4' });
      if (!result.canceled && result.assets && result.assets.length > 0) {
        const file = result.assets[0];
        // file.uri contains the local path
        // expo-document-picker copies the file to the app's cache directory sometimes, 
        // we use that path for the static server.
        let localPath = file.uri.replace('file://', '');
        setVideoFile({ uri: file.uri, path: localPath, name: file.name });
        
        await NetworkService.startHost(localPath, addLog);
      }
    } catch (err) {
      addLog(`Error picking video: ${err.message}`);
    }
  };

  const handlePlaybackStatusUpdate = (status) => {
    if (!status.isLoaded) return;
    
    // Broadcast status to clients
    NetworkService.broadcastToClients(clientsRef.current, {
      type: 'SYNC',
      isPlaying: status.isPlaying,
      positionMillis: status.positionMillis,
      rate: status.rate,
    });
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.backButton} onPress={onBack}>
        <Text style={styles.backButtonText}>← Back</Text>
      </TouchableOpacity>
      
      <Text style={styles.title}>Host Room</Text>

      {!videoFile ? (
        <TouchableOpacity style={styles.pickButton} onPress={pickVideo}>
          <Text style={styles.pickButtonText}>Pick MP4 Video</Text>
        </TouchableOpacity>
      ) : (
        <View style={styles.playerContainer}>
          <Video
            ref={videoRef}
            style={styles.video}
            source={{ uri: videoFile.uri }}
            useNativeControls
            resizeMode={ResizeMode.CONTAIN}
            onPlaybackStatusUpdate={handlePlaybackStatusUpdate}
          />
          <Text style={styles.infoText}>Hosting: {videoFile.name}</Text>
          <Text style={styles.infoText}>Clients connected: {clients.length}</Text>
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
  pickButton: {
    backgroundColor: '#FFFFFF',
    padding: 16,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 24,
  },
  pickButtonText: {
    color: '#000000',
    fontSize: 16,
    fontWeight: '600',
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
    marginTop: 8,
    fontSize: 14,
  },
  logContainer: {
    flex: 1,
    backgroundColor: '#1A1A1A',
    borderRadius: 8,
    padding: 12,
  },
  logText: {
    color: '#888888',
    fontSize: 12,
    marginBottom: 4,
  }
});
