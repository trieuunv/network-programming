package org.np.ui.RMI

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.np.DataSyncService
import java.rmi.Naming

class DataSyncViewModel(private val serverAddress: String) {

    private val viewModelJob = SupervisorJob()
    private val viewModelScope = CoroutineScope(Dispatchers.IO + viewModelJob) // Sử dụng Dispatchers.IO cho RMI

    private val _syncStatus = MutableStateFlow("Chưa kết nối")
    val syncStatus: StateFlow<String> = _syncStatus

    private val rmiService: DataSyncService by lazy {
        try {
            _syncStatus.value = "Đang kết nối..."
            Naming.lookup(serverAddress) as DataSyncService
        } catch (e: Exception) {
            val errorMsg = "Lỗi kết nối RMI: ${e.message}"
            _syncStatus.value = errorMsg
            System.err.println(errorMsg)
            throw e
        }
    }

    fun sendData(key: String, value: String) {
        viewModelScope.launch {
            _syncStatus.value = "Đang gửi $key:$value..."
            try {
                val response = rmiService.sendData(key, value)
                _syncStatus.value = "Thành công: $response"
            } catch (e: Exception) {
                _syncStatus.value = "Lỗi RMI khi gửi dữ liệu: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun onCleared() {
        viewModelJob.cancel()
        println("ViewModel cleared. Coroutines cancelled.")
    }
}