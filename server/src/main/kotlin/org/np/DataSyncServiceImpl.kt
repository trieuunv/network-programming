// File: DataSyncServiceImpl.kt

import org.np.DataSyncService
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import java.rmi.Naming
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class DataSyncServiceImpl(
    private val serverName: String,
    private val peerAddresses: List<String>
) : UnicastRemoteObject(), DataSyncService {

    // Đường dẫn file cục bộ dựa trên tên server
    private val dataFile = File("${serverName.lowercase()}_data.txt")

    // Cache trong bộ nhớ để đọc nhanh hơn và tránh lỗi I/O đa luồng
    private val sharedDataCache = ConcurrentHashMap<String, String>()

    init {
        // Khởi tạo: Đọc dữ liệu từ file vào cache khi Server khởi động
        loadDataFromFile()
        println("[$serverName] Đã khởi tạo. Dữ liệu cục bộ ban đầu: ${sharedDataCache.size} bản ghi từ ${dataFile.name}")
    }

    private fun loadDataFromFile() {
        if (dataFile.exists()) {
            dataFile.readLines().forEach { line ->
                // Giả định mỗi dòng là key=value
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    sharedDataCache[parts[0].trim()] = parts[1].trim()
                }
            }
        }
    }

    private fun saveDataToFile() {
        // Ghi toàn bộ cache trở lại file
        val content = sharedDataCache.map { "${it.key}=${it.value}" }.joinToString("\n")
        dataFile.writeText(content)
    }

    // Phương thức Client gọi để thay đổi dữ liệu
    @Throws(RemoteException::class)
    override fun sendData(key: String, value: String): String {
        println("[$serverName] Client yêu cầu thay đổi: $key -> $value")

        // 1. Cập nhật dữ liệu trong cache
        sharedDataCache[key] = value

        // 2. Ghi vào file cục bộ (Lưu trữ lâu dài)
        saveDataToFile()

        // 3. Kích hoạt nhân rộng
        replicateToPeers(key, value)

        return "[$serverName] Đã nhận, lưu vào file, và bắt đầu đồng bộ hóa."
    }

    // Phương thức Server A gọi Server B để nhân rộng thay đổi
    @Throws(RemoteException::class)
    override fun replicateChange(key: String, value: String, sourceServerName: String): String {
        if (sourceServerName == serverName) {
            return "[$serverName] Bỏ qua thay đổi từ chính nó."
        }

        // 1. Cập nhật dữ liệu trong cache
        sharedDataCache[key] = value

        // 2. Ghi vào file cục bộ
        saveDataToFile()

        println("[$serverName] **NHẬN NHÂN RỘNG** từ $sourceServerName: $key -> $value.")
        return "[$serverName] Đã nhận và cập nhật file thành công."
    }

    private fun replicateToPeers(key: String, value: String) {
        println("[$serverName] Bắt đầu nhân rộng thay đổi ($key) lên ${peerAddresses.size} Peers...")

        // Duyệt qua danh sách địa chỉ RMI của các Server khác (Peers)
        for (peerAddress in peerAddresses) {
            if (!peerAddress.contains(serverName)) {
                try {
                    // 1. Tra cứu dịch vụ RMI của Peer
                    // Import java.rmi.Naming
                    val peerService = Naming.lookup(peerAddress) as DataSyncService

                    // 2. Gọi phương thức nhân rộng từ xa
                    val response = peerService.replicateChange(key, value, serverName)

                    println("[$serverName] ✅ Nhân rộng thành công đến $peerAddress. Phản hồi: $response")
                } catch (e: Exception) {
                    System.err.println("[$serverName] ❌ Lỗi khi nhân rộng đến $peerAddress: ${e.message}")
                }
            }
        }
    }
}