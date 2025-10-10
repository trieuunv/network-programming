package org.np

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.time.LocalDateTime

class UDPServer(private val port: Int = 9876) {
    fun start() {
        Thread {
            val socket = DatagramSocket(port)
            println("🚀 UDP Server đang lắng nghe trên port $port...")

            while (true) {
                try {
                    // Chuẩn bị buffer để nhận dữ liệu
                    val buffer = ByteArray(1024)
                    val receivePacket = DatagramPacket(buffer, buffer.size)

                    // Chờ nhận packet từ client
                    socket.receive(receivePacket)

                    // Xử lý packet nhận được
                    processPacket(socket, receivePacket)

                } catch (e: Exception) {
                    println("❌ Lỗi khi xử lý packet: ${e.message}")
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun processPacket(socket: DatagramSocket, packet: DatagramPacket) {
        val clientAddress = packet.address
        val clientPort = packet.port
        val message = String(packet.data, 0, packet.length)

        println("📥 Nhận từ $clientAddress:$clientPort: $message")

        // Xử lý message và tạo phản hồi
        val response = processMessage(message)
        val responseData = response.toByteArray()

        // Gửi phản hồi lại cho client
        val responsePacket = DatagramPacket(
            responseData,
            responseData.size,
            clientAddress,
            clientPort
        )
        socket.send(responsePacket)

        println("📤 Đã gửi phản hồi: $response")
    }

    private fun processMessage(message: String): String {
        return when (message.trim().lowercase()) {
            "time" -> "⏰ Thời gian hiện tại: ${LocalDateTime.now()}"
            "ping" -> "🏓 pong"
            "hello" -> "👋 Xin chào! Tôi là UDP Server"
            "exit" -> "👋 Tạm biệt!"
            else -> "✅ Server đã nhận: '$message'"
        }
    }

    fun stop() {
        println("🛑 Dừng UDP Server...")
    }
}