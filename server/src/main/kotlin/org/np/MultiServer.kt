package org.np

import DataSyncServiceImpl
import java.rmi.registry.LocateRegistry
import java.rmi.Naming

val SERVER_CONFIGS = listOf(
    Pair("ServerA", 1100),
    Pair("ServerB", 1101)
)

fun startRmiServers() {
    val allPeerAddresses = SERVER_CONFIGS.map { (name, port) ->
        "rmi://localhost:$port/$name"
    }

    SERVER_CONFIGS.forEach { (name, port) ->
        startServer(name, port, allPeerAddresses)
    }
}

fun startServer(name: String, port: Int, peerAddresses: List<String>) {
    Thread {
        try {
            LocateRegistry.createRegistry(port)

            val service = DataSyncServiceImpl(name, peerAddresses)

            val serviceName = "rmi://localhost:$port/$name"
            Naming.rebind(serviceName, service)

            println("✅ [${name}] Server RMI đang chạy tại $serviceName...")
        } catch (e: Exception) {
            System.err.println("❌ Lỗi khởi động $name: ${e.message}")
            e.printStackTrace()
        }
    }.start()
}