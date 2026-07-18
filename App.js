import React, { useState } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, SafeAreaView, StatusBar } from 'react-native';
import HostScreen from './src/screens/HostScreen';
import ClientScreen from './src/screens/ClientScreen';

export default function App() {
  const [role, setRole] = useState(null);

  const renderHome = () => (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>LocalWatch</Text>
        <Text style={styles.subtitle}>Watch together, offline.</Text>
      </View>

      <View style={styles.buttonContainer}>
        <TouchableOpacity 
          style={[styles.card, styles.primaryCard]} 
          onPress={() => setRole('host')}
          activeOpacity={0.8}
        >
          <Text style={[styles.cardTitle, styles.primaryText]}>Host a Room</Text>
          <Text style={[styles.cardDesc, styles.primaryTextDim]}>Share a video from your device</Text>
        </TouchableOpacity>

        <TouchableOpacity 
          style={styles.card} 
          onPress={() => setRole('client')}
          activeOpacity={0.8}
        >
          <Text style={styles.cardTitle}>Join a Room</Text>
          <Text style={styles.cardDesc}>Connect to a nearby host</Text>
        </TouchableOpacity>
      </View>
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="light-content" backgroundColor="#0F0F0F" />
      {role === null && renderHome()}
      {role === 'host' && <HostScreen onBack={() => setRole(null)} />}
      {role === 'client' && <ClientScreen onBack={() => setRole(null)} />}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: '#0F0F0F',
  },
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
  },
  header: {
    marginBottom: 48,
  },
  title: {
    fontSize: 32,
    fontWeight: '700',
    color: '#FFFFFF',
    letterSpacing: -0.5,
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#888888',
    fontWeight: '400',
  },
  buttonContainer: {
    gap: 16,
  },
  card: {
    backgroundColor: '#1A1A1A',
    padding: 24,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#2A2A2A',
  },
  primaryCard: {
    backgroundColor: '#FFFFFF',
    borderColor: '#FFFFFF',
  },
  primaryText: {
    color: '#000000',
  },
  primaryTextDim: {
    color: '#555555',
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#FFFFFF',
    marginBottom: 6,
  },
  cardDesc: {
    fontSize: 14,
    color: '#888888',
  },
});
