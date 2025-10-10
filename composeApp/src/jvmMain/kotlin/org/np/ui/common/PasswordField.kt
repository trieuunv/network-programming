package org.np.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun PasswordField(text: String, value: String, onChangePassword: (String) -> Unit, error: Boolean) {
    var showPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { onChangePassword(it) },
        label = { Text(text) },
        // placeholder = { Text("Nhập mật khẩu") },
        singleLine = true,
        isError = error,
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                if (showPassword) {
                    Text("🙈")
                } else {
                    Text("👁️")
                }
            }
        }
    )
}
