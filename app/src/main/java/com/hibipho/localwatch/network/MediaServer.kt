package com.hibipho.localwatch.network

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.header
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.json.JSONArray
import org.json.JSONObject

data class VideoEntry(val id: String, val name: String, val uri: Uri, val size: Long)

class MediaServer(private val androidContext: Context) {
    private var server: NettyApplicationEngine? = null
    var currentFolderUri: Uri? = null
    var currentSubtitleUri: Uri? = null
    
    private val videoCache = mutableMapOf<String, VideoEntry>()

    fun refreshCatalog() {
        videoCache.clear()
        val uri = currentFolderUri ?: return
        val root = DocumentFile.fromTreeUri(androidContext, uri) ?: return
        
        var counter = 1
        root.listFiles().forEach { file ->
            if (file.isFile) {
                val name = file.name ?: ""
                if (name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true)) {
                    val id = counter.toString()
                    videoCache[id] = VideoEntry(id, name, file.uri, file.length())
                    counter++
                }
            }
        }
    }

    fun startServer(port: Int = 8080) {
        if (server != null) return
        
        server = embeddedServer(Netty, port = port) {
            install(PartialContent)
            install(AutoHeadResponse)
            
            routing {
                get("/catalog") {
                    val arr = JSONArray()
                    videoCache.values.forEach { v ->
                        val obj = JSONObject()
                        obj.put("id", v.id)
                        obj.put("name", v.name)
                        obj.put("size", v.size)
                        arr.put(obj)
                    }
                    call.respond(arr.toString())
                }
                
                get("/video") {
                    val id = call.request.queryParameters["id"]
                    val entry = videoCache[id]
                    
                    if (entry == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    
                    try {
                        val contentResolver = androidContext.contentResolver
                        val inputStream = contentResolver.openInputStream(entry.uri)
                        
                        if (inputStream == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }
                        
                        call.response.header("Content-Length", entry.size.toString())
                        call.response.header("Accept-Ranges", "bytes")
                        call.respondOutputStream {
                            inputStream.use { input ->
                                input.copyTo(this)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
                get("/subtitle") {
                    val uri = currentSubtitleUri
                    if (uri == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
                    try {
                        val inputStream = androidContext.contentResolver.openInputStream(uri)
                        if (inputStream == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@get
                        }
                        call.response.header("Content-Type", "text/plain")
                        call.respondOutputStream {
                            inputStream.use { input -> input.copyTo(this) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }.start(wait = false)
    }

    fun stopServer() {
        server?.stop(1000, 2000)
        server = null
    }
}
