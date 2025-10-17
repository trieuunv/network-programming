package org.np.ui.chatmulticast

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.np.MulticastChatClient // Import Client từ package org.np

class MulticastVM(
    val username: String = "User${(1000..9999).random()}"
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Trạng thái cho UI
    private val _isJoined = MutableStateFlow(false)
    val isJoined: StateFlow<Boolean> = _isJoined.asStateFlow()

    private val _status = MutableStateFlow("Người dùng: $username. Nhấn Join Room.")
    val status: StateFlow<String> = _status.asStateFlow()

    val messages = mutableStateListOf<String>()

    // DANH SÁCH NGƯỜI DÙNG MỚI CHO SIDEBAR
    val userList = mutableStateListOf<String>()

    private val client = MulticastChatClient(username)

    init {
        scope.launch(Dispatchers.Default) {
            client.incomingMessages.collect { message ->
                when {
                    message.startsWith("USERLIST:") -> {
                        val users = message.substringAfter("USERLIST:").split(",").filter { it.isNotBlank() }
                        println(users)
                        withContext(Dispatchers.Main) {
                            userList.clear()
                            userList.addAll(users)
                            _status.value = "Danh sách cập nhật: ${userList.size} người."
                        }
                    }

                    message.startsWith("📡") || message.startsWith("👋") || message.startsWith("❌") || message.startsWith("⚠️") || message.startsWith("🚪") -> {
                        _status.value = message
                        messages.add(message)
                        if (message.startsWith("🚪")) {
                            _isJoined.value = false
                        }
                    }
                    // Tin nhắn Multicast
                    else -> {
                        messages.add(message)
                    }
                }
            }
        }
    }

    fun joinRoom() {
        if (_isJoined.value) return
        _status.value = "Đang yêu cầu phòng..."
        scope.launch(Dispatchers.IO) {
            val roomFound = client.requestRoomFromServer()
            if (roomFound) {
                val joined = client.joinRoom()
                _isJoined.value = joined
            }
        }
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || !_isJoined.value) return
        scope.launch(Dispatchers.IO) {
            client.sendMessage(message)
        }
    }

    override fun onCleared() { // Dùng onCleared() của ViewModel thay vì close()
        client.leaveRoom()
        scope.cancel()
    }

    fun leaveRoom() {
        client.leaveRoom()
        _isJoined.value = false
        userList.clear() // Xóa danh sách người dùng khi rời phòng
    }

    fun sendPrivateMessage(toUser: String, msg: String) {
        if (msg.isBlank() || !_isJoined.value) return
        if (toUser == username) {
            messages.add("⚠️ Không thể gửi tin riêng cho chính mình.")
            return
        }

        scope.launch(Dispatchers.IO) {
            client.sendPrivateMessage(toUser, msg)
        }
    }
}