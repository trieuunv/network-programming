package org.np.ui.chatudp

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.np.UDPClient

class UDPChatVM(val username: String = "User${(1000..9999).random()}") : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = UDPClient(username)

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _status = MutableStateFlow("Người dùng: $username. Nhấn Connect.")
    val status: StateFlow<String> = _status.asStateFlow()

    val messages = mutableStateListOf<String>()
    val userList = mutableStateListOf<String>()
    val groupList = mutableStateListOf<String>()

    init {
        scope.launch {
            client.incomingMessages.collect { msg ->
                when {
                    msg.startsWith("USERLIST:") -> {
                        val users = msg.substringAfter("USERLIST:").split(",").filter { it.isNotBlank() }
                        withContext(Dispatchers.Main) {
                            userList.clear()
                            userList.addAll(users)
                            _status.value = "👥 ${users.size} người đang online. 💬 ${groupList.size} nhóm."
                        }
                    }
                    msg.startsWith("GROUP_LIST:") -> {
                        val groups = msg.substringAfter("GROUP_LIST:").split(",").filter { it.isNotBlank() }
                        withContext(Dispatchers.Main) {
                            groupList.clear()
                            groupList.addAll(groups)
                            _status.value = "👥 ${userList.size} online. 💬 ${groups.size} nhóm."
                        }
                    }
                    else -> withContext(Dispatchers.Main) { messages.add(msg) }
                }
            }
        }
    }

    // Join / Leave
    fun joinRoom() {
        _status.value = "🔌 Đang kết nối..."
        scope.launch(Dispatchers.IO) {
            val ok = client.connect()
            withContext(Dispatchers.Main) {
                _isConnected.value = ok
                _status.value = if (ok) "✅ Đã kết nối." else "❌ Kết nối thất bại."
                // ✅ Thêm lời gọi yêu cầu danh sách nhóm sau khi join
                if (ok) requestGroupList()
            }
        }
    }

    fun leaveRoom() {
        client.disconnect()
        _isConnected.value = false
        userList.clear()
        groupList.clear()
        _status.value = "❌ Đã rời server."
    }

    // Gửi tin nhắn
    fun sendMessage(msg: String) {
        if (msg.isBlank() || !_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.sendMessage(msg) }
    }

    fun sendPrivateMessage(toUser: String, msg: String) {
        if (msg.isBlank() || !_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.sendPrivateMessage(toUser, msg) }
    }

    fun requestGroupList() {
        if (!_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.requestGroupList() }
    }

    fun createGroup(groupName: String) {
        if (groupName.isBlank() || !_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.createGroup(groupName) }
    }

    fun joinGroup(groupName: String) {
        if (groupName.isBlank() || !_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.joinGroup(groupName) }
    }

    fun sendGroupMessage(groupName: String, msg: String) {
        if (msg.isBlank() || !_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.sendGroupMessage(groupName, msg) }
    }

    fun leaveGroup(groupName: String) {
        if (groupName.isBlank() || !_isConnected.value) return
        scope.launch(Dispatchers.IO) { client.leaveGroup(groupName) }
    }

    override fun onCleared() {
        leaveRoom()
        scope.cancel()
    }
}