package org.np.ui.RMI

import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

// Import các thành phần Compose UI cơ bản
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

class DataSyncScreen(private val serverAddress: String) : Screen {
    private val viewModel = DataSyncViewModel(serverAddress)

    @Composable
    override fun Content() {
        val syncStatus by viewModel.syncStatus.collectAsState()

        Column(modifier = Modifier.padding(16.dp)) {

            Text(text = "Trạng thái: $syncStatus",
                modifier = Modifier.padding(bottom = 8.dp))

            Button(
                onClick = {
                    val key = "data_${System.currentTimeMillis()}"
                    val value = "client_data_123"
                    viewModel.sendData(key, value)
                }
            ) {
                Text("Gửi Dữ liệu RMI")
            }
        }
    }

    fun dispose() {
        viewModel.onCleared()
    }
}