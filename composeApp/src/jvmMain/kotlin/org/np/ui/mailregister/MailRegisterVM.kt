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

    fun onChangeRegisterUsername(newUsername: String) {
        registerUsername = newUsername
    }

    fun onChangeRegisterPassword(newPassword: String) {
        registerPassword = newPassword
    }

    fun onChangeRegisterPasswordAgain(newAgainPassword: String) {
        registerPasswordAgain = newAgainPassword
    }

    fun onChangeLoginUsername(newUsername: String) {
        loginUsername = newUsername
    }

    fun onChangeLoginPassword(newPassword: String) {
        loginPassword = newPassword
    }

    init {
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
            client.emit("register", MailAuthDto(registerUsername, registerPassword))
        }
    }

    fun login() {
        if (loginUsername.isNotBlank() && loginPassword.isNotBlank()) {
            client.emit("login", MailAuthDto(loginUsername, loginPassword))
        }
    }
}