package org.np

import DataSyncServiceImpl
import java.rmi.registry.LocateRegistry
import java.rmi.Naming

val SERVER_A_IP = "172.20.10.6"
val SERVER_B_IP = "172.20.10.10"

val SERVER_CONFIGS = listOf(
    Pair("ServerA", 1100),
    Pair("ServerB", 1101)
)

fun startServer(name: String, port: Int, localIp: String, peerAddresses: List<String>) {
    Thread {
        try {
            // 1. Cấu hình IP mà RMI Server sẽ công bố ra bên ngoài
            System.setProperty("java.rmi.server.hostname", localIp)

            // 2. Tạo Registry
            // Registry sẽ lắng nghe trên IP cục bộ và port đã chỉ định
            LocateRegistry.createRegistry(port)

            // 3. Khởi tạo Service (với danh sách peer chứa IP từ xa)
            val service = DataSyncServiceImpl(name, peerAddresses)

            // 4. Đăng ký tên dịch vụ
            val serviceName = "rmi://${localIp}:$port/$name" // Dùng IP cục bộ
            Naming.rebind(serviceName, service)

            println("✅ [${name}] Server RMI đang chạy tại $serviceName...")
            println("Peer List: $peerAddresses")
        } catch (e: Exception) {
            System.err.println("❌ Lỗi khởi động $name: ${e.message}")
            e.printStackTrace()
        }
    }.start()
}

fun mainServerA() {
    val localIp = SERVER_A_IP
    val configA = SERVER_CONFIGS[0] // ServerA: 1100
    val configB = SERVER_CONFIGS[1] // ServerB: 1101

    // Địa chỉ của Peer (Server B)
    val peerBAddress = "rmi://${SERVER_B_IP}:${configB.second}/${configB.first}"

    // Danh sách Peer cho Server A (chỉ cần Server B)
    val peerAddresses = listOf(peerBAddress)

    // Khởi động Server A
    startServer(configA.first, configA.second, localIp, peerAddresses)
}

/**
 * Hàm chính để chạy trên VM 2 (Server B)
 */
fun mainServerB() {
    val localIp = SERVER_B_IP
    val configA = SERVER_CONFIGS[0] // ServerA: 1100
    val configB = SERVER_CONFIGS[1] // ServerB: 1101

    // Địa chỉ của Peer (Server A)
    val peerAAddress = "rmi://${SERVER_A_IP}:${configA.second}/${configA.first}"

    // Danh sách Peer cho Server B (chỉ cần Server A)
    val peerAddresses = listOf(peerAAddress)

    // Khởi động Server B
    startServer(configB.first, configB.second, localIp, peerAddresses)
}