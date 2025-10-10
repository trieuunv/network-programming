package org.np

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.np.dto.SocketMessage
import java.io.*
import java.net.Socket

object TCPClient {
    private var socket: Socket? = null

    @PublishedApi
    internal var writer: PrintWriter? = null

    private var reader: BufferedReader? = null

    @PublishedApi
    internal val listeners = mutableMapOf<String, MutableList<(String?) -> Unit>>()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val json = Json { ignoreUnknownKeys = true }

    var onConnectSuccess: (() -> Unit)? = null

    fun connect(host: String = "127.0.0.1", port: Int = 9000) {
        if (isConnected.value) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val s = Socket(host, port)
                val w = PrintWriter(s.getOutputStream(), true)
                val r = BufferedReader(InputStreamReader(s.getInputStream()))

                socket = s
                writer = w
                reader = r
                _isConnected.value = true

                onConnectSuccess?.invoke()

                launch {
                    try {
                        while (true) {
                            val line = r.readLine() ?: break
                            val socketMessage = json.decodeFromString(SocketMessage.serializer(), line)
                            listeners[socketMessage.event]?.forEach { callback ->
                                callback(socketMessage.data)
                            }
                        }
                    } catch (_: Exception) {}
                }

            } catch (e: Exception) {
                println("Connection failed: ${e.message}")
                _isConnected.value = false
            }
        }
    }

    inline fun <reified T> emit(event: String, data: T? = null) {
        val jsonData = data?.let { json.encodeToString(it) }
        val socketMessage = SocketMessage(event, jsonData)
        val jsonSocket = json.encodeToString(socketMessage)

        if (!isConnected.value) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                writer?.println("$jsonSocket")
            } catch (e: Exception) {
                println("Send failed: ${e.message}")
            }
        }
    }

    fun emit(event: String) {
        val socketMessage = SocketMessage(event, null)
        val jsonSocket = json.encodeToString(socketMessage)
        writer?.println(jsonSocket)
    }

    inline fun <reified T> on(event: String, noinline callback: (T?) -> Unit) {
        val list = listeners.getOrPut(event) { mutableListOf() }

        val wrapper: (String?) -> Unit = { data ->
            if (data == null) {
                callback(null)
            } else {
                try {
                    val obj = json.decodeFromString<T>(data)
                    callback(obj)
                } catch (e: Exception) {
                    println("Decode failed for event '$event': ${e.message}")
                }
            }
        }

        list.add(wrapper)
    }

    fun on(event: String, callback: () -> Unit) {
        val list = listeners.getOrPut(event) { mutableListOf() }

        val wrapper: (String?) -> Unit = {
            callback()
        }

        list.add(wrapper)
    }

    fun disconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                socket?.close()
                _isConnected.value = false
                println("Disconnected")
            } catch (_: Exception) {}
        }
    }
}