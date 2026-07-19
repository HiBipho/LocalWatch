package com.hibipho.localwatch

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.app.PictureInPictureParams
import android.util.Rational
import android.os.Build
import androidx.compose.animation.core.*
import java.util.UUID
import com.hibipho.localwatch.network.LocalNetworkService
import com.hibipho.localwatch.network.MediaServer
import com.hibipho.localwatch.network.Room
import com.hibipho.localwatch.network.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import android.app.Activity
import android.view.WindowManager

// --- Aesthetics ---
val GlassColor = Color(0x66000000)
val NeonAccent = Color(0xFF00FFCC)

class MainActivity : ComponentActivity() {

    private lateinit var networkService: LocalNetworkService
    private lateinit var mediaServer: MediaServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        networkService = LocalNetworkService(this)
        mediaServer = MediaServer(this)
        
        val prefs = getSharedPreferences("LocalWatchPrefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("username", null)
        
        networkService.onToastMessage = { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = NeonAccent, background = Color.Black)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var username by remember { mutableStateOf(savedName) }
                    
                    if (username == null) {
                        WelcomeScreen { name ->
                            prefs.edit().putString("username", name).apply()
                            username = name
                        }
                    } else {
                        networkService.myUsername = username!!
                        LocalWatchApp(networkService, mediaServer) {
                            prefs.edit().remove("username").apply()
                            username = null
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkService.cleanup()
        mediaServer.stopServer()
    }
}

@Composable
fun WelcomeScreen(onNameEntered: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Siapa nama kamu?", style = MaterialTheme.typography.headlineMedium, color = NeonAccent)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nama/Panggilan") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonAccent,
                focusedLabelColor = NeonAccent
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { if (name.isNotBlank()) onNameEntered(name.trim()) },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black)
        ) {
            Text("Masuk")
        }
    }
}

@Composable
fun LocalWatchApp(networkService: LocalNetworkService, mediaServer: MediaServer, onLogout: () -> Unit) {
    var role by remember { mutableStateOf<String?>(null) }
    
    if (role == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LocalWatch Native", style = MaterialTheme.typography.headlineLarge, color = NeonAccent)
            Text("Halo, ${networkService.myUsername}!", style = MaterialTheme.typography.bodyLarge, color = Color.LightGray)
            
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { role = "host" }, 
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
            ) {
                Text("Host a Room", color = NeonAccent)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { role = "client" }, 
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
            ) {
                Text("Join a Room", color = NeonAccent)
            }
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = onLogout) {
                Text("Ganti Nama", color = Color.Red)
            }
        }
    } else if (role == "host") {
        HostScreen(networkService, mediaServer) { role = null }
    } else {
        ClientScreen(networkService) { role = null }
    }
}

@Composable
fun HostScreen(networkService: LocalNetworkService, mediaServer: MediaServer, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isRoomActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            mediaServer.currentFolderUri = it
            mediaServer.refreshCatalog()
            mediaServer.startServer(8080)
            
            coroutineScope.launch {
                networkService.startTcpServer()
                networkService.startBroadcasting("Ruangan ${networkService.myUsername}")
            }
            isRoomActive = true
        }
    }

    if (!isRoomActive) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pilih Folder Film (Katalog)", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { launcher.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black)
            ) {
                Text("Pilih Folder")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("Kembali", color = Color.Gray) }
        }
    } else {
        RoomScreen(isHost = true, hostIp = "127.0.0.1", networkService = networkService, mediaServer = mediaServer)
    }
}

@Composable
fun ClientScreen(networkService: LocalNetworkService, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val availableRooms by networkService.availableRooms.collectAsState()
    var joinedRoom by remember { mutableStateOf<Room?>(null) }
    
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("LocalWatchPrefs", Context.MODE_PRIVATE)
    val lastIp = prefs.getString("last_room_ip", null)
    val lastName = prefs.getString("last_room_name", null)
    
    DisposableEffect(Unit) {
        coroutineScope.launch {
            networkService.startListeningForRooms()
        }
        onDispose {
            networkService.stopListening()
        }
    }

    if (joinedRoom == null) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Ruangan Tersedia", style = MaterialTheme.typography.headlineMedium, color = NeonAccent)
            Text("Mencari Host di jaringan WiFi...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (lastIp != null && lastName != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            coroutineScope.launch {
                                networkService.connectToHost(lastIp, 9001)
                                joinedRoom = Room(lastName, lastIp, 9001)
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF332222)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Riwayat Terakhir (Tap to Reconnect)", color = NeonAccent, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(lastName, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("IP: $lastIp", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
            
            LazyColumn {
                items(availableRooms) { room ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                coroutineScope.launch {
                                    prefs.edit()
                                        .putString("last_room_ip", room.ip)
                                        .putString("last_room_name", room.name)
                                        .apply()
                                        
                                    networkService.connectToHost(room.ip, room.port)
                                    joinedRoom = room
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(room.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("IP: ${room.ip}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("Kembali", color = Color.Gray) }
        }
    } else {
        RoomScreen(isHost = false, hostIp = joinedRoom!!.ip, networkService = networkService, mediaServer = null)
    }
}

@Composable
fun RoomScreen(isHost: Boolean, hostIp: String, networkService: LocalNetworkService, mediaServer: MediaServer?) {
    var activeVideoId by remember { mutableStateOf<String?>(null) }
    var hasSubtitle by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    DisposableEffect(Unit) {
        val originalListener = networkService.onSyncMessageReceived
        networkService.onSyncMessageReceived = { json ->
            val action = json.optString("action")
            if (action == "change_video") {
                val newId = json.optString("id")
                activeVideoId = if (newId == "null") null else newId
                hasSubtitle = false // reset subtitle on change
            } else if (action == "room_state") {
                val newId = json.optString("activeVideoId")
                activeVideoId = if (newId == "null") null else newId
                hasSubtitle = json.optBoolean("hasSubtitle", false)
            } else if (action == "subtitle_loaded") {
                hasSubtitle = true
            } else {
                originalListener?.invoke(json)
            }
        }
        onDispose {
            networkService.onSyncMessageReceived = originalListener
        }
    }

    if (activeVideoId == null) {
        CatalogScreen(hostIp = hostIp) { videoId ->
            if (isHost) {
                activeVideoId = videoId
                networkService.currentVideoId = videoId
                networkService.sendSyncMessage(JSONObject().put("action", "change_video").put("id", videoId))
            } else {
                Toast.makeText(context, "Hanya Host yang bisa mengganti film!", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        VideoPlayerScreen(
            videoUri = Uri.parse("http://$hostIp:8080/video?id=$activeVideoId"),
            subtitleUri = if (hasSubtitle) Uri.parse("http://$hostIp:8080/subtitle") else null,
            isHost = isHost,
            hostIp = hostIp,
            networkService = networkService,
            mediaServer = mediaServer,
            onBackToCatalog = { 
                if (isHost) {
                    activeVideoId = null 
                    networkService.currentVideoId = null
                    networkService.sendSyncMessage(JSONObject().put("action", "change_video").put("id", "null"))
                }
            }
        )
    }
}

data class CatalogItem(val id: String, val name: String, val size: Long)

@Composable
fun CatalogScreen(hostIp: String, onVideoSelected: (String) -> Unit) {
    var catalog by remember { mutableStateOf<List<CatalogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = URL("http://$hostIp:8080/catalog").readText()
                val arr = JSONArray(jsonString)
                val list = mutableListOf<CatalogItem>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(CatalogItem(obj.getString("id"), obj.getString("name"), obj.getLong("size")))
                }
                withContext(Dispatchers.Main) {
                    catalog = list
                    isLoading = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    error = e.message
                    isLoading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Katalog Film", style = MaterialTheme.typography.headlineMedium, color = NeonAccent)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = NeonAccent)
        } else if (error != null) {
            Text("Error memuat katalog: $error", color = Color.Red)
        } else if (catalog.isEmpty()) {
            Text("Folder kosong / Tidak ada file mp4/mkv.", color = Color.Gray)
        } else {
            LazyColumn {
                items(catalog) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onVideoSelected(item.id) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text("Ukuran: ${item.size / (1024 * 1024)} MB", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(
    videoUri: Uri?,
    subtitleUri: Uri?,
    isHost: Boolean,
    hostIp: String,
    networkService: LocalNetworkService,
    mediaServer: MediaServer?,
    onBackToCatalog: () -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    // FLAG_KEEP_SCREEN_ON to prevent device from sleeping
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    
    val subtitleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (isHost && mediaServer != null) {
                mediaServer.currentSubtitleUri = it
                networkService.sendSyncMessage(JSONObject().put("action", "subtitle_loaded"))
            }
        }
    }

    data class FloatingEmoji(val id: String, val emoji: String, val startX: Float)
    var floatingEmojis by remember { mutableStateOf<List<FloatingEmoji>>(emptyList()) }

    // Player rebuilds when videoUri or subtitleUri changes
    DisposableEffect(videoUri, subtitleUri) {
        val player = ExoPlayer.Builder(context).build().apply {
            videoUri?.let { uri ->
                val mediaItemBuilder = MediaItem.Builder().setUri(uri)
                subtitleUri?.let { subUri ->
                    val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                        .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                        .setLanguage("id")
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build()
                    mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
                }
                setMediaItem(mediaItemBuilder.build())
                prepare()
                playWhenReady = false
            }
        }
        exoPlayer = player
        
        val originalListener = networkService.onSyncMessageReceived
        networkService.onSyncMessageReceived = { json ->
            when (json.optString("action")) {
                "play" -> player.play()
                "pause" -> player.pause()
                "seek" -> player.seekTo(json.optLong("position"))
                "reaction" -> {
                    val emoji = json.optString("emoji")
                    floatingEmojis = floatingEmojis + FloatingEmoji(UUID.randomUUID().toString(), emoji, (0..100).random() / 100f)
                }
                else -> originalListener?.invoke(json)
            }
        }

        onDispose {
            player.release()
            networkService.onSyncMessageReceived = originalListener
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Floating Emojis Layer
                floatingEmojis.forEach { fe ->
                    FloatingEmojiAnim(fe.emoji, fe.startX) {
                        floatingEmojis = floatingEmojis.filter { it.id != fe.id }
                    }
                }
            }
            
            // Controls & Setup Panel (Glassmorphism)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassColor)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (isHost) {
                        Button(
                            onClick = onBackToCatalog,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text("Katalog") }
                        
                        Button(
                            onClick = { subtitleLauncher.launch("application/x-subrip") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4444FF))
                        ) { Text("Subtitle (.srt)") }
                    }
                    
                    Button(
                        onClick = {
                            exoPlayer?.play()
                            networkService.sendSyncMessage(JSONObject().put("action", "play"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonAccent, contentColor = Color.Black)
                    ) { Text("Play") }
                    
                    Button(
                        onClick = {
                            exoPlayer?.pause()
                            networkService.sendSyncMessage(JSONObject().put("action", "pause"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White)
                    ) { Text("Pause") }
                    
                    if (isHost) {
                        Button(
                            onClick = {
                                val pos = (exoPlayer?.currentPosition ?: 0L) + 10000L
                                exoPlayer?.seekTo(pos)
                                networkService.sendSyncMessage(JSONObject().put("action", "seek").put("position", pos))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text(">> 10s") }
                    }
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val params = PictureInPictureParams.Builder()
                                    .setAspectRatio(Rational(16, 9))
                                    .build()
                                (context as? Activity)?.enterPictureInPictureMode(params)
                            } else {
                                Toast.makeText(context, "PiP tidak didukung di Android ini.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("PiP") }
                }
                
                // Emoji Reaction Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val emojis = listOf("😂", "😱", "🔥", "😭", "😍")
                    emojis.forEach { emoji ->
                        IconButton(onClick = {
                            networkService.sendSyncMessage(JSONObject().put("action", "reaction").put("emoji", emoji))
                            floatingEmojis = floatingEmojis + FloatingEmoji(UUID.randomUUID().toString(), emoji, (0..100).random() / 100f)
                        }) {
                            Text(emoji, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
        
        // Chat & Participants Overlay (Floating)
        ChatOverlay(networkService, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
fun ChatOverlay(networkService: LocalNetworkService, modifier: Modifier = Modifier) {
    val messages by networkService.chatMessages.collectAsState()
    val users by networkService.connectedUsers.collectAsState()
    var chatText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.padding(16.dp)) {
        if (!isExpanded) {
            Button(
                onClick = { isExpanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = GlassColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("💬 Chat (${users.size} Users)", color = NeonAccent)
            }
        } else {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .height(400.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassColor)
                    .padding(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Live Chat (${users.size})", color = NeonAccent, style = MaterialTheme.typography.titleMedium)
                    Text("❌", modifier = Modifier.clickable { isExpanded = false })
                }
                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(messages) { msg ->
                        Text(
                            text = "${msg.senderName}: ${msg.message}",
                            color = if (msg.senderId == networkService.myUserId) NeonAccent else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Pesan...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        )
                    )
                    IconButton(onClick = {
                        if (chatText.isNotBlank()) {
                            networkService.sendSyncMessage(JSONObject().apply {
                                put("action", "chat")
                                put("senderId", networkService.myUserId)
                                put("senderName", networkService.myUsername)
                                put("message", chatText)
                                put("timestamp", System.currentTimeMillis())
                            })
                            chatText = ""
                        }
                    }) {
                        Text("Kirim", color = NeonAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingEmojiAnim(emoji: String, startX: Float, onEnd: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    
    val transition = updateTransition(targetState = isVisible, label = "FloatUp")
    
    val offsetY by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 2000, easing = LinearOutSlowInEasing) },
        label = "offsetY"
    ) { visible -> if (visible) -500f else 0f }
    
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 2000, easing = LinearOutSlowInEasing) },
        label = "alpha"
    ) { visible -> if (visible) 0f else 1f }

    LaunchedEffect(Unit) {
        isVisible = true
        kotlinx.coroutines.delay(2000)
        onEnd()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 50.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (startX * 300).dp, y = offsetY.dp)
                .alpha(alpha)
        )
    }
}
