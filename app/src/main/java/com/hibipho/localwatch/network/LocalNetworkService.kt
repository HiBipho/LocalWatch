package com.hibipho.localwatch.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap

data class Room(val name: String, val ip: String, val port: Int)
data class User(val id: String, val name: String)
data class ChatMessage(val senderId: String, val senderName: String, val message: String, val timestamp: Long)

class LocalNetworkService(private val context: Context) {

    private val UDP_PORT = 9002
    private val TCP_PORT = 9001
    
    var myUsername: String = "Guest"
    val myUserId: String = UUID.randomUUID().toString()
    
    private var udpSocket: DatagramSocket? = null
    private var isBroadcasting = AtomicBoolean(false)
    private var isListening = AtomicBoolean(false)
    
    private val _availableRooms = MutableStateFlow<List<Room>>(emptyList())
    val availableRooms: StateFlow<List<Room>> = _availableRooms

    private val _connectedUsers = MutableStateFlow<List<User>>(emptyList())
    val connectedUsers: StateFlow<List<User>> = _connectedUsers
    
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    
    var currentVideoId: String? = null
    
    // Server-side list of writers to broadcast to all clients
    private val clientWriters = ConcurrentHashMap<String, PrintWriter>()
    // Single writer for Client-side
    private var hostWriter: PrintWriter? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var isServerRunning = AtomicBoolean(false)
    
    var onSyncMessageReceived: ((JSONObject) -> Unit)? = null
    var onToastMessage: ((String) -> Unit)? = null

    @Suppress("DEPRECATION")
    private fun getBroadcastAddress(): InetAddress? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo ?: return null
            val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
            val quads = ByteArray(4)
            for (k in 0..3) {
                quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
            }
            return InetAddress.getByAddress(quads)
        } catch (e: Exception) {
            return null
        }
    }

    @Suppress("DEPRECATION")
    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            return "127.0.0.1"
        }
    }

    suspend fun startBroadcasting(roomName: String) = withContext(Dispatchers.IO) {
        if (isBroadcasting.get()) return@withContext
        isBroadcasting.set(true)
        val ip = getLocalIpAddress()
        
        // Host is the first user
        _connectedUsers.value = listOf(User(myUserId, myUsername))
        
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            val broadcastAddress = getBroadcastAddress() ?: InetAddress.getByName("255.255.255.255")
            
            while (isBroadcasting.get() && isActive) {
                val message = JSONObject().apply {
                    put("type", "announce")
                    put("name", roomName)
                    put("ip", ip)
                    put("port", TCP_PORT)
                }.toString()
                
                val buffer = message.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, UDP_PORT)
                socket.send(packet)
                delay(2000)
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopBroadcasting() {
        isBroadcasting.set(false)
    }

    suspend fun startListeningForRooms() = withContext(Dispatchers.IO) {
        if (isListening.get()) return@withContext
        isListening.set(true)
        
        try {
            udpSocket = DatagramSocket(UDP_PORT)
            udpSocket?.broadcast = true
            val buffer = ByteArray(1024)
            
            val roomsMap = mutableMapOf<String, Room>()
            
            while (isListening.get() && isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                udpSocket?.receive(packet)
                val message = String(packet.data, 0, packet.length)
                
                try {
                    val json = JSONObject(message)
                    if (json.optString("type") == "announce") {
                        val name = json.getString("name")
                        val ip = json.getString("ip")
                        val port = json.getInt("port")
                        val room = Room(name, ip, port)
                        
                        if (!roomsMap.containsKey(ip)) {
                            roomsMap[ip] = room
                            _availableRooms.value = roomsMap.values.toList()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        isListening.set(false)
        udpSocket?.close()
    }

    // TCP Server (Host)
    suspend fun startTcpServer() = withContext(Dispatchers.IO) {
        if (isServerRunning.get()) return@withContext
        isServerRunning.set(true)
        
        try {
            serverSocket = ServerSocket(TCP_PORT)
            while (isServerRunning.get() && isActive) {
                val socket = serverSocket?.accept() ?: break
                launch { handleClientAsServer(socket) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private suspend fun handleClientAsServer(socket: Socket) = withContext(Dispatchers.IO) {
        var clientId: String? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            
            while (isActive) {
                val line = reader.readLine() ?: break
                val json = JSONObject(line)
                
                val action = json.optString("action")
                if (action == "join") {
                    clientId = json.optString("userId")
                    val name = json.optString("username")
                    clientWriters[clientId!!] = writer
                    
                    val newList = _connectedUsers.value.toMutableList()
                    newList.add(User(clientId!!, name))
                    _connectedUsers.value = newList
                    
                    broadcastToClients(JSONObject().apply {
                        put("action", "users_update")
                        val arr = JSONArray()
                        newList.forEach { u ->
                            arr.put(JSONObject().put("id", u.id).put("name", u.name))
                        }
                        put("users", arr)
                    })
                    
                    // Sync current video state to the newly joined client
                    writer.println(JSONObject().apply {
                        put("action", "room_state")
                        put("activeVideoId", currentVideoId ?: "null")
                    }.toString())
                    
                    withContext(Dispatchers.Main) {
                        onToastMessage?.invoke("$name bergabung ke ruangan")
                    }
                } else {
                    if (action == "change_video") {
                        currentVideoId = if (json.optString("id") == "null") null else json.optString("id")
                    } else if (action == "chat") {
                        val chat = ChatMessage(
                            senderId = json.optString("senderId"),
                            senderName = json.optString("senderName"),
                            message = json.optString("message"),
                            timestamp = json.optLong("timestamp")
                        )
                        _chatMessages.value = _chatMessages.value + chat
                    } else if (action == "reaction") {
                        // Pass to UI
                    }
                    clientWriters.forEach { (id, out) ->
                        if (id != clientId) {
                            out.println(line)
                        }
                    }
                    // Also trigger local host action
                    withContext(Dispatchers.Main) {
                        onSyncMessageReceived?.invoke(json)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (clientId != null) {
                clientWriters.remove(clientId)
                val newList = _connectedUsers.value.filter { it.id != clientId }
                _connectedUsers.value = newList
                
                broadcastToClients(JSONObject().apply {
                    put("action", "users_update")
                    val arr = JSONArray()
                    newList.forEach { u ->
                        arr.put(JSONObject().put("id", u.id).put("name", u.name))
                    }
                    put("users", arr)
                })
            }
            socket.close()
        }
    }

    private fun broadcastToClients(message: JSONObject) {
        val msgStr = message.toString()
        clientWriters.values.forEach {
            it.println(msgStr)
        }
    }

    // TCP Client (Joiner)
    suspend fun connectToHost(ip: String, port: Int) = withContext(Dispatchers.IO) {
        var retryCount = 0
        val maxRetries = 5
        var connected = false
        
        while (retryCount < maxRetries && !connected && isActive) {
            try {
                clientSocket = Socket(ip, port)
                hostWriter = PrintWriter(clientSocket!!.getOutputStream(), true)
                connected = true
                
                withContext(Dispatchers.Main) {
                    onToastMessage?.invoke("Berhasil terhubung!")
                }
                
                // Send join message
                hostWriter?.println(JSONObject().apply {
                    put("action", "join")
                    put("userId", myUserId)
                    put("username", myUsername)
                }.toString())
                
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    try {
                        val json = JSONObject(line)
                        val action = json.optString("action")
                        if (action == "users_update") {
                            val arr = json.getJSONArray("users")
                            val newList = mutableListOf<User>()
                            for (i in 0 until arr.length()) {
                                val obj = arr.getJSONObject(i)
                                newList.add(User(obj.getString("id"), obj.getString("name")))
                            }
                            _connectedUsers.value = newList
                        } else if (action == "chat") {
                            val chat = ChatMessage(
                                senderId = json.optString("senderId"),
                                senderName = json.optString("senderName"),
                                message = json.optString("message"),
                                timestamp = json.optLong("timestamp")
                            )
                            _chatMessages.value = _chatMessages.value + chat
                        } else {
                            withContext(Dispatchers.Main) {
                                onSyncMessageReceived?.invoke(json)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                clientSocket?.close()
                connected = false
            }
            
            if (isActive && !connected) {
                retryCount++
                withContext(Dispatchers.Main) {
                    onToastMessage?.invoke("Koneksi terputus. Mencoba ulang... ($retryCount/$maxRetries)")
                }
                delay(2000)
            }
        }
        
        if (!connected) {
            withContext(Dispatchers.Main) {
                onToastMessage?.invoke("Gagal terhubung setelah $maxRetries percobaan.")
            }
        }
    }

    fun sendSyncMessage(message: JSONObject) {
        Thread {
            if (isServerRunning.get()) {
                if (message.optString("action") == "change_video") {
                    val id = message.optString("id")
                    currentVideoId = if (id == "null") null else id
                } else if (message.optString("action") == "chat") {
                    val chat = ChatMessage(
                        senderId = message.optString("senderId"),
                        senderName = message.optString("senderName"),
                        message = message.optString("message"),
                        timestamp = message.optLong("timestamp")
                    )
                    _chatMessages.value = _chatMessages.value + chat
                } else if (message.optString("action") == "reaction") {
                    // Pass to UI
                }
                // If Host, broadcast to all clients
                broadcastToClients(message)
            } else {
                // If Client, send to Host
                hostWriter?.println(message.toString())
            }
        }.start()
    }

    fun cleanup() {
        stopBroadcasting()
        stopListening()
        isServerRunning.set(false)
        serverSocket?.close()
        clientSocket?.close()
        clientWriters.clear()
        _connectedUsers.value = emptyList()
    }
}
