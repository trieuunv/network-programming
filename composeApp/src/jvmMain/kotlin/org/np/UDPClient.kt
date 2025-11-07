package org.np

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.net.*
import kotlin.concurrent.thread

class UDPClient(private val username: String) {
    private var udpSocket: DatagramSocket? = null
    private var serverAddr: InetAddress? = null

    @Volatile private var isRunning = false

    private val messageChannel = Channel<String>(Channel.UNLIMITED)
    val incomingMessages: Flow<String> = messageChannel.receiveAsFlow()

    private fun report(msg: String) = messageChannel.trySendBlocking(msg)

    fun connect(serverHost: String = Constants.HOST, serverPort: Int = Constants.UDP_PORT): Boolean {
        return try {
            serverAddr = InetAddress.getByName(serverHost)
            udpSocket = DatagramSocket()
            val joinMsg = "JOIN:$username".toByteArray()
            udpSocket!!.send(DatagramPacket(joinMsg, joinMsg.size, serverAddr, serverPort))

            val buffer = ByteArray(1024)
            val resp = DatagramPacket(buffer, buffer.size)
            udpSocket!!.soTimeout = 3000
            udpSocket!!.receive(resp)
            val response = String(resp.data, 0, resp.length).trim()

            if (response == "JOIN_OK") {
                report("✅ Đã kết nối server.")
                startListener()
                isRunning = true
                true
            } else {
                report("❌ $response")
                false
            }
        } catch (e: Exception) {
            report("❌ Kết nối thất bại: ${e.message}")
            false
        }
    }

    private fun startListener() {
        thread(isDaemon = true) {
            val buffer = ByteArray(2048)
            while (isRunning && udpSocket != null && !udpSocket!!.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket!!.receive(packet)
                    val msg = String(packet.data, 0, packet.length).trim()
                    when {
                        // Tin nhắn chung
                        msg.startsWith("USERLIST:") -> report(msg)
                        msg.startsWith("PRIVATE:") -> report("💌 ${msg.substringAfter("PRIVATE:")}")
                        msg.startsWith("PRIVATE_SENT:") -> {
                            val content = msg.substringAfter("PRIVATE_SENT:")
                            // Format tin nhắn: 💌 (đến [receiver]): [content]
                            report("💌 (đến $content")
                        }
                        // ✅ Tin nhắn nhóm
                        msg.startsWith("GROUP_LIST:") -> report(msg)
                        msg.startsWith("GROUP_MSG:") -> {
                            // Format: GROUP_MSG:<Tên nhóm>:<Nội dung>
                            val parts = msg.substringAfter("GROUP_MSG:").split(":", limit = 2)
                            val groupName = parts[0]
                            val content = parts[1]
                            // Client nhận tin nhắn nhóm. Nếu content bắt đầu bằng [Username]:, đó là tin nhắn của người dùng khác.
                            // Nếu content bắt đầu bằng [currentUsername]:, đó là tin nhắn của chính mình.
                            report("[Nhóm $groupName] $content")
                        }

                        // ✅ Phản hồi từ Server
                        msg.startsWith("GROUP_CREATED:") -> report("✅ Đã tạo nhóm: ${msg.substringAfter("GROUP_CREATED:")}")
                        msg.startsWith("GROUP_JOINED:") -> report("✅ Đã tham gia nhóm: ${msg.substringAfter("GROUP_JOINED:")}")
                        msg.startsWith("GROUP_MSG_SENT:") -> {
                            // Không cần hiển thị tin này, chỉ là xác nhận
                        }

                        msg.startsWith("GROUP_LEFT:") -> report("🚪 Đã rời nhóm: ${msg.substringAfter("GROUP_LEFT:")}")

                        else -> report(msg)
                    }
                } catch (_: SocketTimeoutException) {
                } catch (e: Exception) {
                    if (isRunning) report("⚠️ Lỗi nhận: ${e.message}")
                }
            }
        }
    }

    fun sendMessage(msg: String) {
        if (udpSocket == null || serverAddr == null) return
        val data = "MSG:$msg".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    fun sendPrivateMessage(toUser: String, msg: String) {
        if (udpSocket == null || serverAddr == null) return
        val data = "PRIVATE:$toUser|$msg".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    // ✅ Hàm mới: Gửi lệnh nhóm
    fun createGroup(groupName: String) {
        if (udpSocket == null || serverAddr == null) return
        val data = "CREATE_GROUP:$groupName".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    fun requestGroupList() {
        if (udpSocket == null || serverAddr == null) return
        val data = "GROUP_LIST".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    fun joinGroup(groupName: String) {
        if (udpSocket == null || serverAddr == null) return
        val data = "JOIN_GROUP:$groupName".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    fun leaveGroup(groupName: String) {
        if (udpSocket == null || serverAddr == null) return
        val data = "LEAVE_GROUP:$groupName".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    fun sendGroupMessage(groupName: String, msg: String) {
        if (udpSocket == null || serverAddr == null) return
        // Format: GROUP_MSG:<Tên nhóm>|<Nội dung>
        val data = "GROUP_MSG:$groupName|$msg".toByteArray()
        udpSocket!!.send(DatagramPacket(data, data.size, serverAddr, 9876))
    }

    fun disconnect() {
        if (udpSocket == null) return
        val leave = "LEAVE:$username".toByteArray()
        udpSocket!!.send(DatagramPacket(leave, leave.size, serverAddr, 9876))
        isRunning = false
        udpSocket?.close()
        report("🚪 Đã rời phòng.")
    }
}