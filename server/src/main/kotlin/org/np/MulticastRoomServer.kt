package org.np

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class MulticastRoomServer(private val port: Int = 9876) {
    private val multicastAddress = "230.0.0.1"
    private val multicastPort = 4446

    @Volatile private var isRunning = false
    private var socket: DatagramSocket? = null

    // BẢNG ĐĂNG KÝ: Username -> (Address, Port)
    private val clientRegistry = ConcurrentHashMap<String, Pair<InetAddress, Int>>()

    fun start() {
        isRunning = true
        socket = DatagramSocket(port)
        println("🚀 Server quản lý phòng đang chạy tại port $port...")

        thread(isDaemon = true) {
            val buffer = ByteArray(1024)
            while (isRunning) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket!!.receive(packet)

                    // Phân tích cú pháp: [LỆNH]:[DỮ LIỆU]
                    val fullMessage = String(packet.data, 0, packet.length).trim()
                    val parts = fullMessage.split(":", limit = 2)
                    val command = parts[0].uppercase()
                    val data = if (parts.size > 1) parts[1] else ""

                    processRequest(socket!!, packet, command, data)

                } catch (e: SocketException) {
                    if (isRunning) println("❌ Lỗi Socket: ${e.message}")
                    break
                } catch (e: Exception) {
                    println("❌ Lỗi xử lý yêu cầu: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        socket?.close()
        println("🛑 Server đã dừng.")
    }

    private fun sendResponse(socket: DatagramSocket, address: InetAddress, port: Int, response: String) {
        try {
            val responseData = response.toByteArray()
            val responsePacket = DatagramPacket(responseData, responseData.size, address, port)
            socket.send(responsePacket)
        } catch (e: Exception) {
            println("❌ Lỗi gửi phản hồi đến ${address.hostAddress}:$port - ${e.message}")
        }
    }

    private fun broadcastUserList() {
        val userList = clientRegistry.keys.joinToString(",")
        val message = "USERLIST:$userList"
        println("📢 Cập nhật danh sách người dùng: $userList")

        try {
            // Gửi qua multicast
            val group = InetAddress.getByName(multicastAddress)
            val multicastSocket = MulticastSocket()
            val packet = DatagramPacket(message.toByteArray(), message.length, group, multicastPort)
            multicastSocket.send(packet)
            multicastSocket.close()
        } catch (e: Exception) {
            println("❌ Lỗi broadcast danh sách: ${e.message}")
        }
    }

    private fun processRequest(socket: DatagramSocket, packet: DatagramPacket, command: String, data: String) {
        val clientAddress = packet.address
        val clientPort = packet.port

        when (command) {
            "JOIN" -> {
                // data = [USERNAME]
                if (data.isBlank()) {
                    sendResponse(socket, clientAddress, clientPort, "ERROR:Thiếu Username")
                    return
                }

                // Kiểm tra username đã tồn tại
                if (clientRegistry.containsKey(data)) {
                    sendResponse(socket, clientAddress, clientPort, "ERROR:Username '$data' đã được sử dụng")
                    return
                }

                // Đăng ký Client và gửi thông tin Multicast
                clientRegistry[data] = Pair(clientAddress, clientPort)
                val response = "ROOM:$multicastAddress:$multicastPort"
                sendResponse(socket, clientAddress, clientPort, response)
                println("✅ Đăng ký: $data tại ${clientAddress.hostAddress}:$clientPort")

                broadcastUserList()
            }

            "PRIVATE" -> {
                val parts = data.split("|", limit = 2)
                if (parts.size < 2) {
                    sendResponse(socket, clientAddress, clientPort, "ERROR:Sai cú pháp PRIVATE")
                    return
                }

                val toUser = parts[0]
                val content = parts[1]
                val targetInfo = clientRegistry[toUser]

                if (targetInfo != null) {
                    val privateMsg = "PRIVATE:$content"
                    sendResponse(socket, targetInfo.first, targetInfo.second, privateMsg)
                    println("📩 Tin riêng đến $toUser (${targetInfo.first.hostAddress}:${targetInfo.second}): $content")

                    // Xác nhận cho người gửi
                    sendResponse(socket, clientAddress, clientPort, "PRIVATE_SENT:$toUser")
                } else {
                    sendResponse(socket, clientAddress, clientPort, "ERROR:Người dùng '$toUser' không tồn tại")
                }
            }

            "LEAVE" -> {
                // data = [USERNAME]
                if (clientRegistry.remove(data) != null) {
                    println("🚪 Hủy đăng ký: $data")
                    broadcastUserList()
                    sendResponse(socket, clientAddress, clientPort, "LEAVE_OK:Đã rời phòng")
                }
            }

            "PING" -> {
                // Heartbeat để kiểm tra kết nối
                sendResponse(socket, clientAddress, clientPort, "PONG")
            }

            else -> {
                sendResponse(socket, clientAddress, clientPort, "ERROR:Lệnh không hợp lệ")
            }
        }
    }
}