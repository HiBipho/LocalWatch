import TcpSocket from 'react-native-tcp-socket';
import Zeroconf from 'react-native-zeroconf';
import StaticServer from '@dr.pogodin/react-native-static-server';

const PORT_TCP = 9001;
const PORT_HTTP = 9000;
const SERVICE_TYPE = 'localwatch';
const SERVICE_PROTOCOL = 'tcp';
const SERVICE_DOMAIN = 'local.';

class NetworkService {
  constructor() {
    this.zeroconf = new Zeroconf();
    this.server = null;
    this.client = null;
    this.staticServer = null;
    
    // Callbacks
    this.onClientConnected = null;
    this.onMessageReceived = null;
    this.onHostFound = null;
  }

  // --- HOST METHODS ---

  async startHost(mediaPath, onLog) {
    try {
      onLog('Starting HTTP Server for media...');
      // Extract directory from path
      const dirPath = mediaPath.substring(0, mediaPath.lastIndexOf('/'));
      
      this.staticServer = new StaticServer(PORT_HTTP, dirPath, { localOnly: true });
      const url = await this.staticServer.start();
      onLog(`HTTP Server started at ${url}`);

      onLog('Starting TCP Sync Server...');
      this.server = TcpSocket.createServer((socket) => {
        onLog(`Client connected: ${socket.remoteAddress}`);
        if (this.onClientConnected) this.onClientConnected(socket);

        socket.on('data', (data) => {
          if (this.onMessageReceived) {
            try {
              const msg = JSON.parse(data.toString());
              this.onMessageReceived(msg, socket);
            } catch(e) {}
          }
        });

        socket.on('error', (error) => onLog(`Client error: ${error}`));
        socket.on('close', () => onLog(`Client disconnected`));
      });

      this.server.listen({ port: PORT_TCP, host: '0.0.0.0' }, () => {
        onLog(`TCP Server listening on port ${PORT_TCP}`);
        // Start mDNS broadcasting
        this.zeroconf.publish(SERVICE_TYPE, SERVICE_PROTOCOL, SERVICE_DOMAIN, 'LocalWatch Host', PORT_TCP);
        onLog('mDNS Broadcasting started...');
      });

      return url;
    } catch (error) {
      onLog(`Error starting host: ${error.message}`);
      throw error;
    }
  }

  broadcastToClients(clients, message) {
    const data = JSON.stringify(message);
    clients.forEach(clientSocket => {
      clientSocket.write(data);
    });
  }

  stopHost() {
    if (this.zeroconf) this.zeroconf.unpublishAll();
    if (this.server) {
      this.server.close();
      this.server = null;
    }
    if (this.staticServer) {
      this.staticServer.stop();
      this.staticServer = null;
    }
  }

  // --- CLIENT METHODS ---

  startDiscovery(onLog) {
    onLog('Scanning for hosts...');
    this.zeroconf.on('resolved', (service) => {
      if (service.name.includes('LocalWatch Host')) {
        onLog(`Found host: ${service.name} at ${service.host}:${service.port}`);
        if (this.onHostFound) this.onHostFound(service);
      }
    });
    
    this.zeroconf.on('error', (err) => onLog(`Discovery error: ${err}`));
    this.zeroconf.scan(SERVICE_TYPE, SERVICE_PROTOCOL, SERVICE_DOMAIN);
  }

  stopDiscovery() {
    this.zeroconf.stop();
    this.zeroconf.removeDeviceListeners();
  }

  connectToHost(ip, port, onLog) {
    return new Promise((resolve, reject) => {
      onLog(`Connecting to ${ip}:${port}...`);
      this.client = TcpSocket.createConnection({ port, host: ip }, () => {
        onLog('Connected to Host!');
        resolve();
      });

      this.client.on('data', (data) => {
        if (this.onMessageReceived) {
          try {
            const msg = JSON.parse(data.toString());
            this.onMessageReceived(msg);
          } catch(e) {}
        }
      });

      this.client.on('error', (error) => {
        onLog(`Connection error: ${error}`);
        reject(error);
      });

      this.client.on('close', () => {
        onLog('Disconnected from host');
      });
    });
  }

  sendMessageToHost(message) {
    if (this.client) {
      this.client.write(JSON.stringify(message));
    }
  }

  disconnectClient() {
    if (this.client) {
      this.client.destroy();
      this.client = null;
    }
  }
}

export default new NetworkService();
