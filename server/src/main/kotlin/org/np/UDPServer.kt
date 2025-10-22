package org.np

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class UDPServer(private val port: Int = 9876) {
    @Volatile private var isRunning = false
    private var socket: DatagramSocket? = null

    // Registry for users: Username -> (Address, Port)
    private val clientRegistry = ConcurrentHashMap<String, Pair<InetAddress, Int>>()

    // ✅ Registry for groups: GroupName -> Set<Username>
    private val groupRegistry = ConcurrentHashMap<String, MutableSet<String>>()

    fun start() {
        isRunning = true
        socket = DatagramSocket(port)
        println("🚀 Server đang chạy trên cổng $port (Unicast)...")

        thread(isDaemon = true) {
            val buffer = ByteArray(2048)
            while (isRunning) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket!!.receive(packet)
                    val msg = String(packet.data, 0, packet.length).trim()
                    processMessage(packet, msg)
                } catch (e: SocketException) {
                    if (isRunning) println("❌ Socket lỗi: ${e.message}")
                    break
                } catch (e: Exception) {
                    println("❌ Lỗi xử lý: ${e.message}")
                }
            }
        }
    }

    fun stop() {
        isRunning = false
        socket?.close()
        println("🛑 Server đã dừng.")
    }

    private fun send(to: InetAddress, port: Int, message: String) {
        try {
            val data = message.toByteArray()
            socket?.send(DatagramPacket(data, data.size, to, port))
        } catch (e: Exception) {
            println("❌ Lỗi gửi đến ${to.hostAddress}:$port - ${e.message}")
        }
    }

    private fun broadcast(message: String, excludeUser: String? = null) {
        for ((user, info) in clientRegistry) {
            if (user == excludeUser) continue
            send(info.first, info.second, message)
        }
    }

    private fun updateUserList() {
        val list = clientRegistry.keys.joinToString(",")
        val msg = "USERLIST:$list"
        println("📢 Cập nhật danh sách: $list")
        broadcast(msg)
    }

    // ✅ Hàm mới: Gửi danh sách nhóm
    private fun sendGroupList(to: InetAddress? = null, port: Int? = null) {
        val list = groupRegistry.keys.joinToString(",")
        val msg = "GROUP_LIST:$list"
        println("📢 Cập nhật danh sách nhóm: $list")

        if (to != null && port != null) {
            send(to, port, msg) // Gửi riêng tới một client
        } else {
            broadcast(msg) // Broadcast tới tất cả client
        }
    }

    // ✅ Hàm mới: Gửi tin nhắn đến các thành viên trong nhóm
    private fun sendToGroup(groupName: String, message: String, excludeUser: String? = null) {
        // Gửi tin nhắn theo format: GROUP_MSG:<Tên nhóm>:<Nội dung>
        val msgToSend = "GROUP_MSG:$groupName:$message"
        val members = groupRegistry[groupName] ?: return

        for (user in members) {
            if (user == excludeUser) continue
            val info = clientRegistry[user]
            if (info != null) {
                send(info.first, info.second, msgToSend)
            }
        }
    }

    private fun processMessage(packet: DatagramPacket, msg: String) {
        val addr = packet.address
        val port = packet.port

        // Tìm người gửi hiện tại
        val sender = clientRegistry.entries.find { it.value == Pair(addr, port) }?.key

        when {
            // ... (Lệnh JOIN, MSG, PRIVATE, PING, LEAVE giữ nguyên)
            msg.startsWith("JOIN:") -> {
                // ... (Logic JOIN cũ)
                val user = msg.substringAfter("JOIN:").trim()
                if (user.isEmpty()) {
                    send(addr, port, "ERROR:Thiếu tên người dùng")
                    return
                }
                if (clientRegistry.containsKey(user)) {
                    send(addr, port, "ERROR:Tên '$user' đã tồn tại")
                    return
                }
                clientRegistry[user] = Pair(addr, port)
                println("✅ $user đã tham gia (${addr.hostAddress}:$port)")
                send(addr, port, "JOIN_OK")
                updateUserList()
                sendGroupList(addr, port) // Gửi danh sách nhóm khi join
                broadcast("📢 $user đã tham gia!", user)
            }

            msg.startsWith("MSG:") -> {
                if (sender != null) {
                    val content = msg.substringAfter("MSG:").trim()
                    broadcast("[$sender]: $content", excludeUser = null)
                }
            }

            msg.startsWith("PRIVATE:") -> {
                val parts = msg.substringAfter("PRIVATE:").split("|", limit = 2)
                if (parts.size < 2) {
                    send(addr, port, "ERROR:Sai cú pháp PRIVATE")
                    return
                }
                val toUser = parts[0]
                val content = parts[1]
                val target = clientRegistry[toUser]
                val sender = clientRegistry.entries.find { it.value == Pair(addr, port) }?.key

                if (target != null && sender != null) {
                    // 1. Gửi tin nhắn đến người nhận (PRIVATE:[sender]: [content])
                    send(target.first, target.second, "PRIVATE:$sender: $content")

                    // 2. ✅ SỬA LỖI: Gửi lại tin nhắn đến người gửi (PRIVATE_SENT:[toUser]: [content])
                    //    Sử dụng tiền tố PRIVATE_SENT để client nhận biết đây là tin nhắn của chính mình
                    send(addr, port, "PRIVATE_SENT:$toUser: $content")

                } else send(addr, port, "ERROR:Người dùng không tồn tại")
            }

            msg.startsWith("LEAVE:") -> {
                val user = msg.substringAfter("LEAVE:").trim()
                if (clientRegistry.remove(user) != null) {
                    // Xóa người dùng khỏi tất cả các nhóm
                    groupRegistry.values.forEach { it.remove(user) }

                    println("🚪 $user đã rời phòng.")
                    updateUserList()
                    broadcast("🚪 $user đã rời phòng.")
                }
            }

            // ✅ Lệnh: CREATE_GROUP
            msg.startsWith("CREATE_GROUP:") -> {
                val groupName = msg.substringAfter("CREATE_GROUP:").trim()
                if (sender == null || groupName.isEmpty()) {
                    send(addr, port, "ERROR:Tạo nhóm thất bại.")
                    return
                }
                if (groupRegistry.containsKey(groupName)) {
                    send(addr, port, "ERROR:Nhóm '$groupName' đã tồn tại.")
                    return
                }

                groupRegistry[groupName] = ConcurrentHashMap.newKeySet<String>().apply { add(sender) }
                println("✅ $sender đã tạo nhóm: $groupName")
                send(addr, port, "GROUP_CREATED:$groupName")
                sendGroupList()
            }

            // ✅ Lệnh: GROUP_LIST
            msg == "GROUP_LIST" -> {
                sendGroupList(addr, port)
            }

            // ✅ Lệnh: JOIN_GROUP
            msg.startsWith("JOIN_GROUP:") -> {
                val groupName = msg.substringAfter("JOIN_GROUP:").trim()
                if (sender == null || groupName.isEmpty()) {
                    send(addr, port, "ERROR:Tham gia nhóm thất bại.")
                    return
                }
                val group = groupRegistry[groupName]
                if (group == null) {
                    send(addr, port, "ERROR:Nhóm '$groupName' không tồn tại.")
                    return
                }

                if (group.add(sender)) {
                    println("✅ $sender đã tham gia nhóm: $groupName")
                    send(addr, port, "GROUP_JOINED:$groupName")
                    sendToGroup(groupName, "📢 $sender đã tham gia nhóm.", excludeUser = sender)
                } else {
                    send(addr, port, "ERROR:Bạn đã là thành viên của nhóm này.")
                }
            }

            // ✅ Lệnh: GROUP_MSG
            msg.startsWith("GROUP_MSG:") -> {
                val parts = msg.substringAfter("GROUP_MSG:").split("|", limit = 2)
                if (parts.size < 2 || sender == null) {
                    send(addr, port, "ERROR:Sai cú pháp GROUP_MSG")
                    return
                }
                val groupName = parts[0]
                val content = parts[1]

                if (groupRegistry[groupName]?.contains(sender) == true) {
                    val formattedMsg = "[$sender]: $content"
                    sendToGroup(groupName, formattedMsg, excludeUser = null)
                    send(addr, port, "GROUP_MSG_SENT:$groupName")
                } else {
                    send(addr, port, "ERROR:Bạn không phải thành viên của nhóm '$groupName'")
                }
            }

            msg.startsWith("LEAVE_GROUP:") -> {
                val groupName = msg.substringAfter("LEAVE_GROUP:").trim()
                if (sender == null || groupName.isEmpty()) {
                    send(addr, port, "ERROR:Rời nhóm thất bại.")
                    return
                }

                val group = groupRegistry[groupName]
                if (group == null) {
                    send(addr, port, "ERROR:Nhóm '$groupName' không tồn tại.")
                    return
                }

                if (group.remove(sender)) {
                    println("🚪 $sender đã rời nhóm: $groupName")
                    send(addr, port, "GROUP_LEFT:$groupName")

                    // Thông báo cho các thành viên còn lại
                    sendToGroup(groupName, "🚪 $sender đã rời nhóm.", excludeUser = sender)

                    // Nếu nhóm không còn ai, xóa nhóm
                    if (group.isEmpty()) {
                        groupRegistry.remove(groupName)
                        sendGroupList() // Cập nhật danh sách nhóm cho tất cả client
                        println("❌ Nhóm $groupName đã bị xóa vì không còn thành viên.")
                    }
                } else {
                    send(addr, port, "ERROR:Bạn không phải là thành viên của nhóm '$groupName'")
                }
            }

            msg == "PING" -> send(addr, port, "PONG")

            else -> send(addr, port, "ERROR:Lệnh không hợp lệ")
        }
    }
}