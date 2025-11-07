package org.np.ui.mailregister

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.np.TCPClient
import org.np.dto.MailAuthDto

class MailRegisterVM : ViewModel() {
    val client = TCPClient

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _registerError = MutableSharedFlow<String>()
    val registerError = _registerError.asSharedFlow()

    var registerFullname by mutableStateOf("")
        private set

    var registerEmail by mutableStateOf("")
        private set

    var registerUsername by mutableStateOf("")
        private set

    var registerPassword by mutableStateOf("")
        private set

    var registerPasswordAgain by mutableStateOf("")
        private set

    var loginUsername by mutableStateOf("")
        private set

    var loginPassword by mutableStateOf("")
        private set

    fun onChangeRegisterFullName(v: String) { registerFullname = v }
    fun onChangeRegisterUsername(v: String) { registerUsername = v }
    fun onChangeRegisterEmail(v: String) { registerEmail = v }
    fun onChangeRegisterPassword(v: String) { registerPassword = v }
    fun onChangeRegisterPasswordAgain(v: String) { registerPasswordAgain = v }

    fun onChangeLoginUsername(newUsername: String) {
        loginUsername = newUsername
    }

    fun onChangeLoginPassword(newPassword: String) {
        loginPassword = newPassword
    }

    init {
        client.connect()

        client.on("register_success") {
            viewModelScope.launch {
                _navigationEvent.emit("toHome")
            }
        }

        client.on("login_success") {
            viewModelScope.launch {
                _navigationEvent.emit("toHome")
            }
        }

        client.on("register_error") {
            viewModelScope.launch {
                _registerError.emit("Register Error")
            }
        }

        client.on("login_error") {
            viewModelScope.launch {
                _registerError.emit("Invalid login credentials")
            }
        }
    }

    fun register() {
        if (registerUsername.isNotBlank() && registerPassword.isNotBlank()) {
            client.emit("register", MailAuthDto(registerUsername, registerPassword, registerEmail, registerFullname))
        }
    }

    fun login() {
        if (loginUsername.isNotBlank() && loginPassword.isNotBlank()) {
            client.emit("login", MailAuthDto(loginUsername, loginPassword))
        }
    }
}