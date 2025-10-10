package org.np.ui.setup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.np.TCPClient

class SetUpViewModel: ViewModel() {
    var host by mutableStateOf("localhost")
        private set

    var port by mutableStateOf("9000")
        private set

    fun onHostChange(newHost: String) {
        host = newHost
    }

    fun onPortChange(newPort: String) {
        val filteredValue = newPort.filter { it.isDigit() }
        port = filteredValue
    }

    val portInt: Int
        get() = port.toIntOrNull() ?: 8080

    fun connect() {
        if (host != "" && portInt in 1..65535) {
            TCPClient.connect()
        }
    }
}