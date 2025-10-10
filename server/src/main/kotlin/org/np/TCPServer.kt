package org.np

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.np.dto.SocketMessage
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class ClientSocket(
    val id: String = UUID.randomUUID().toString(),
    val socket: Socket,
    val output: ByteWriteChannel
)

object TCPServer {
    private var serverSocket: ServerSocket? = null
    private val selectorManager = SelectorManager(Dispatchers.IO)
    val clients = ConcurrentHashMap<String, ClientSocket>()
    val rooms = ConcurrentHashMap<String, MutableSet<String>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    val listeners = mutableMapOf<String, MutableList<(ClientSocket, String?) -> Unit>>()
    private val connectionListeners = mutableListOf<(ClientSocket) -> Unit>()
    private val disconnectionListeners = mutableListOf<(ClientSocket) -> Unit>()

    var isRunning = false
        private set

    fun start(port: Int = 9000) {
        if (isRunning) return
        isRunning = true

        scope.launch(Dispatchers.IO) {
            serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", port)
            println("Server listening on port $port")

            while (true) {
                val socket = serverSocket!!.accept()

                val writeChannel = socket.openWriteChannel(autoFlush = true)
                val client = ClientSocket(socket = socket, output = writeChannel)
                clients[client.id] = client

                connectionListeners.forEach { listener ->
                    try {
                        listener(client)
                    } catch (e: Exception) {
                        println("Connection listener error: ${e.message}")
                    }
                }

                handleClient(client)
            }
        }
    }

    private fun handleClient(client: ClientSocket) = scope.launch {
        val input = client.socket.openReadChannel()

        try {
            while (true) {
                val line = input.readUTF8Line() ?: break
                val socketMessage = json.decodeFromString(SocketMessage.serializer(), line)
                listeners[socketMessage.event]?.forEach { callback ->
                    callback(client, socketMessage.data)
                }
            }
        } catch (e: Exception) {
            println("Client error: ${e.message}")
        } finally {
            clients.remove(client.id)

            rooms.values.forEach { room ->
                room.remove(client.id)
            }

            disconnectionListeners.forEach { listener ->
                try {
                    listener(client)
                } catch (e: Exception) {
                    println("Disconnection listener error: ${e.message}")
                }
            }

            client.socket.close()
        }
    }

    inline fun <reified T> subscribe(event: String, noinline callback: (ClientSocket, T) -> Unit) {
        val list = listeners.getOrPut(event) { mutableListOf() }

        val wrapper: (ClientSocket, String?) -> Unit = { client, data ->
            if (data == null) {
                println("No data for event $event")
            } else {
                try {
                    val obj = json.decodeFromString<T>(data)
                    callback(client, obj)
                } catch (e: Exception) {
                    println("Decode failed for event '$event': ${e.message}")
                }
            }
        }

        list.add(wrapper)
    }

    fun subscribe(event: String, callback: (ClientSocket) -> Unit) {
        val list = listeners.getOrPut(event) { mutableListOf() }

        val wrapper: (ClientSocket, String?) -> Unit = { client, _ ->
            callback(client)
        }

        list.add(wrapper)
    }

    suspend inline fun <reified T> sendToClient(client: ClientSocket, event: String, data: T) {
        val jsonData = json.encodeToString(data)
        val socketMessage = SocketMessage(event, jsonData)
        val jsonSocket = json.encodeToString(socketMessage)

        try {
            client.output.writeStringUtf8("$jsonSocket\n")
        } catch (e: Exception) {
            println("Send to client failed: ${e.message}")
            clients.remove(client.id)
            client.socket.close()
        }
    }

    suspend fun sendToClient(client: ClientSocket, event: String) {
        val socketMessage = SocketMessage(event, null)
        val jsonSocket = json.encodeToString(socketMessage)
        try {
            client.output.writeStringUtf8("$jsonSocket\n")
        } catch (e: Exception) {
            println("Send to client failed: ${e.message}")
            clients.remove(client.id)
            client.socket.close()
        }
    }

    private suspend inline fun <reified T> broadcast(event: String, data: T) {
        val jsonData = json.encodeToString(data)
        val socketMessage = SocketMessage(event, jsonData)
        val jsonSocket = json.encodeToString(socketMessage)

        clients.values.forEach { client ->
            try {
                client.output.writeStringUtf8("$jsonSocket\n")
            } catch (e: Exception) {
                println("Send failed: ${e.message}")
            }
        }
    }

    fun join(room: String, client: ClientSocket) {
        rooms.computeIfAbsent(room) { mutableSetOf() }.add(client.id)
        println("Client ${client.socket.remoteAddress} joined room $room")
    }

    fun leave(room: String, client: ClientSocket) {
        rooms[room]?.remove(client.id)
        println("Client ${client.socket.remoteAddress} left room $room")
    }

    suspend inline fun <reified T> broadcastToRoom(room: String, event: String, data: T) {
        val jsonData = json.encodeToString(data)
        val socketMessage = SocketMessage(event, jsonData)
        val jsonSocket = json.encodeToString(socketMessage)

        rooms[room]?.forEach { clientId ->
            val client = clients[clientId]
            if (client != null) {
                try {
                    client.output.writeStringUtf8("$jsonSocket\n")
                } catch (e: Exception) {
                    println("Send failed to ${client.socket.remoteAddress}: ${e.message}")
                }
            }
        }
    }

    fun onConnection(listener: (ClientSocket) -> Unit) {
        connectionListeners.add(listener)
    }

    fun onDisconnection(listener: (ClientSocket) -> Unit) {
        disconnectionListeners.add(listener)
    }

    fun stop() {
        scope.launch {
            clients.values.forEach { client -> client.socket.close() }
            clients.clear()
            serverSocket?.close()
            isRunning = false
            println("Server stopped")
        }
    }
}