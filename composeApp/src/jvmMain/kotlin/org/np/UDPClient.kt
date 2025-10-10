package org.np

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPClient {

    fun sendMessage(
        host: String,
        port: Int,
        message: String,
        timeout: Int = 5000
    ): String? {
        var socket: DatagramSocket? = null

        return try {
            // Tạo socket với timeout
            socket = DatagramSocket().apply {
                soTimeout = timeout
            }

            val serverAddress = InetAddress.getByName(host)
            val sendData = message.toByteArray()

            // Tạo và gửi packet đến server
            val sendPacket = DatagramPacket(
                sendData,
                sendData.size,
                serverAddress,
                port
            )
            socket.send(sendPacket)
            println("📤 Đã gửi đến $host:$port: $message")

            // Chuẩn bị nhận phản hồi
            val receiveBuffer = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            // Chờ nhận phản hồi từ server
            socket.receive(receivePacket)

            // Chuyển đổi dữ liệu nhận được thành chuỗi
            val response = String(
                receivePacket.data,
                0,
                receivePacket.length
            )

            println("📥 Nhận phản hồi: $response")
            response

        } catch (e: Exception) {
            println("❌ Lỗi khi gửi/nhận: ${e.message}")
            null
        } finally {
            socket?.close()
        }
    }

    fun startInteractiveMode(host: String, port: Int) {
        println("🎮 Chế độ tương tác UDP Client")
        println("Gõ 'exit' để thoát")
        println("----------------------------")

        while (true) {
            print("Nhập message: ")
            val message = readLine()?.trim() ?: continue

            if (message.equals("exit", ignoreCase = true)) {
                println("👋 Tạm biệt!")
                break
            }

            if (message.isBlank()) {
                continue
            }

            val response = sendMessage(host, port, message)
            if (response == null) {
                println("⚠️ Không nhận được phản hồi từ server")
            }

            println()
        }
    }
}