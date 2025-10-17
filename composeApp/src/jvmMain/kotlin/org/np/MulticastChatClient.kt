package org.np

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.net.*
import kotlin.concurrent.thread

class MulticastChatClient(private val username: String) {
    private lateinit var group: InetAddress
    private var port: Int = 0
    private var multicastSocket: MulticastSocket? = null
    private var udpSocket: DatagramSocket? = null  // Socket riêng cho tin nhắn cá nhân

    @Volatile private var isRunning = false
    @Volatile private var isListeningPrivate = false

    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    val incomingMessages: Flow<String> = messageChannel.receiveAsFlow()

    private fun report(msg: String) {
        messageChannel.trySendBlocking(msg)
    }

    fun requestRoomFromServer(serverHost: String = "localhost", serverPort: Int = 9876): Boolean {
        try {
            // Tạo UDP socket và GIỮ MỞ để nhận tin nhắn riêng
            udpSocket = DatagramSocket()
            val serverAddr = InetAddress.getByName(serverHost)

            // Gửi lệnh JOIN:[USERNAME]
            val request = "JOIN:$username".toByteArray()
            val packet = DatagramPacket(request, request.size, serverAddr, serverPort)
            udpSocket!!.send(packet)

            // Nhận phản hồi từ server
            val buffer = ByteArray(1024)
            val responsePacket = DatagramPacket(buffer, buffer.size)
            udpSocket!!.receive(responsePacket)

            val response = String(responsePacket.data, 0, responsePacket.length).trim()

            return if (response.startsWith("ROOM:")) {
                val parts = response.split(":")
                group = InetAddress.getByName(parts[1])
                port = parts[2].toInt()

                val localPort = udpSocket!!.localPort
                report("📡 Nhận phòng: ${group.hostAddress}:$port")
                report("🔌 UDP Socket cá nhân: localhost:$localPort")

                // Bắt đầu lắng nghe tin nhắn riêng
                startPrivateMessageListener()

                true
            } else if (response.startsWith("ERROR:")) {
                report("❌ ${response.substring(6)}")
                udpSocket?.close()
                udpSocket = null
                false
            } else {
                report("⚠️ Server trả về: $response")
                udpSocket?.close()
                udpSocket = null
                false
            }
        } catch (e: Exception) {
            report("❌ Lỗi yêu cầu phòng: ${e.message}")
            udpSocket?.close()
            udpSocket = null
            return false
        }
    }

    private fun startPrivateMessageListener() {
        isListeningPrivate = true
        thread(isDaemon = true, name = "PrivateMessageListener") {
            val buffer = ByteArray(1024)
            report("👂 Bắt đầu lắng nghe tin nhắn riêng...")

            while (isListeningPrivate && udpSocket != null && !udpSocket!!.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket!!.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()

                    when {
                        message.startsWith("PRIVATE:") -> {
                            // Tin nhắn riêng từ server
                            val content = message.substring(8)
                            report("💌 $content")
                        }
                        message.startsWith("PRIVATE_SENT:") -> {
                            // Xác nhận đã gửi tin riêng
                            val toUser = message.substring(13)
                            report("✅ Tin nhắn riêng đã gửi đến $toUser")
                        }
                        message.startsWith("ERROR:") -> {
                            report("❌ ${message.substring(6)}")
                        }
                        message == "PONG" -> {
                            // Heartbeat response
                        }
                        else -> {
                            report("📨 $message")
                        }
                    }
                } catch (e: SocketException) {
                    if (isListeningPrivate) {
                        report("⚠️ UDP socket đóng: ${e.message}")
                    }
                    break
                } catch (e: Exception) {
                    if (isListeningPrivate) {
                        report("❌ Lỗi nhận tin riêng: ${e.message}")
                    }
                }
            }
            report("🔇 Dừng lắng nghe tin nhắn riêng")
        }
    }

    fun sendPrivateMessage(toUser: String, msg: String) {
        if (udpSocket == null || udpSocket!!.isClosed) {
            report("⚠️ Chưa kết nối đến server.")
            return
        }

        try {
            val serverAddr = InetAddress.getByName("localhost")
            val request = "PRIVATE:$toUser|$username: $msg".toByteArray()
            val packet = DatagramPacket(request, request.size, serverAddr, 9876)
            udpSocket!!.send(packet)
            report("💌 Đang gửi đến $toUser: $msg")
        } catch (e: Exception) {
            report("❌ Lỗi gửi tin riêng: ${e.message}")
        }
    }

    fun joinRoom(): Boolean {
        if (!::group.isInitialized || port == 0) {
            report("⚠️ Chưa nhận được thông tin phòng.")
            return false
        }

        try {
            multicastSocket = MulticastSocket(port)
            multicastSocket!!.joinGroup(group)
            isRunning = true
            report("👋 $username đã tham gia phòng ${group.hostAddress}:$port")

            // Luồng nhận tin nhắn multicast
            thread(isDaemon = true, name = "MulticastListener") {
                val buffer = ByteArray(1024)
                while (isRunning && multicastSocket != null && !multicastSocket!!.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        multicastSocket!!.receive(packet)
                        val fullMessage = String(packet.data, 0, packet.length).trim()

                        when {
                            fullMessage.startsWith("USERLIST:") -> {
                                report(fullMessage)
                            }
                            !fullMessage.startsWith("[$username]") -> {
                                report(fullMessage)
                            }
                        }

                    } catch (e: SocketException) {
                        if (isRunning) {
                            report("⚠️ Multicast socket đóng: ${e.message}")
                        }
                        break
                    } catch (e: Exception) {
                        if (isRunning) {
                            report("❌ Lỗi nhận tin multicast: ${e.message}")
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            report("❌ Lỗi tham gia phòng: ${e.message}")
            isRunning = false
            return false
        }
    }

    fun sendMessage(msg: String) {
        if (multicastSocket == null || !isRunning) {
            report("⚠️ Chưa tham gia phòng.")
            return
        }

        try {
            val fullMsg = "[$username]: $msg"
            val data = fullMsg.toByteArray()
            val packet = DatagramPacket(data, data.size, group, port)
            multicastSocket!!.send(packet)
            report(fullMsg)
        } catch (e: Exception) {
            report("❌ Lỗi gửi tin: ${e.message}")
        }
    }

    fun leaveRoom() {
        if (!isRunning && !isListeningPrivate) return

        // Gửi lệnh LEAVE đến server
        try {
            if (udpSocket != null && !udpSocket!!.isClosed) {
                val serverAddr = InetAddress.getByName("localhost")
                val request = "LEAVE:$username".toByteArray()
                val packet = DatagramPacket(request, request.size, serverAddr, 9876)
                udpSocket!!.send(packet)
            }
        } catch (e: Exception) {
            report("⚠️ Lỗi khi gửi lệnh LEAVE: ${e.message}")
        }

        // Dừng các luồng lắng nghe
        isRunning = false
        isListeningPrivate = false

        // Đóng multicast socket
        try {
            multicastSocket?.leaveGroup(group)
            multicastSocket?.close()
            multicastSocket = null
        } catch (e: Exception) {
            report("⚠️ Lỗi đóng multicast socket: ${e.message}")
        }

        // Đóng UDP socket
        try {
            udpSocket?.close()
            udpSocket = null
        } catch (e: Exception) {
            report("⚠️ Lỗi đóng UDP socket: ${e.message}")
        }

        messageChannel.close()
        report("🚪 $username đã rời phòng.")
    }

    fun isConnected(): Boolean {
        return udpSocket != null && !udpSocket!!.isClosed && isListeningPrivate
    }
}