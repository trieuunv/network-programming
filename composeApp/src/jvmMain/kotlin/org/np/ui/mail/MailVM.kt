package org.np.ui.mail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import org.np.TCPClient
import org.np.dto.MailDto
import org.np.dto.MailSendDto
import java.time.format.DateTimeFormatter

data class MailUiState(
    val username: String = "",
    val title: String = "",
    val content: String = ""
)

class MailVM : ViewModel() {
    val client = TCPClient

    val mails = mutableStateListOf<MailDto>()

    var sendState by mutableStateOf(MailUiState())
        private set

    fun onChangeUsername(newUsername: String) {
        sendState = sendState.copy(username = newUsername)
    }

    fun onChangeTitle(newTitle: String) {
        sendState = sendState.copy(title = newTitle)
    }

    fun onChangeContent(newContent: String) {
        sendState = sendState.copy(content = newContent)
    }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val _isSuccessNow = MutableStateFlow(false)
    val isSuccessNow = _isSuccessNow.asStateFlow()

    private val _sendError = MutableStateFlow("")
    val sendError = _sendError.asStateFlow()

    fun setSuccess(value: Boolean) {
        _isSuccessNow.value = value
    }

    fun setSendError(value: String) {
        _sendError.value = value
    }

    init {
        client.on<List<MailDto>>("get_mails_rs") { list ->
            MainScope().launch {
                mails.clear()
                if (list != null) {
                    mails.addAll(
                        list.sortedByDescending {
                            LocalDateTime.parse(it.sendAt, formatter)
                        }
                    )
                }
            }
        }

        client.on<MailDto>("new_mail") { mail ->
            if (mail != null) {
                mails.add(mail)
            }
        }

        client.on("send_error") {
            viewModelScope.launch {
                _sendError.emit("Send Error")
            }
        }

        client.on("send_success") {
            viewModelScope.launch {
                _isSuccessNow.emit(true)
            }
        }

        client.emit("get_mails")
    }

    fun sendMail() {
        if (sendState.username.isNotBlank() && sendState.title.isNotBlank() && sendState.content.isNotBlank()) {
            val sendDto = MailSendDto(sendState.username, sendState.title, sendState.content)
            client.emit("send_mail", sendDto)
        }
    }
}