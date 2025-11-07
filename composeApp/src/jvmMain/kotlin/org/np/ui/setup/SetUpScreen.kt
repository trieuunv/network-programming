package org.np.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.np.ui.chatudp.UDPChatScreen
import org.np.ui.mailregister.MailRegisterScreen
import org.np.ui.register.RegisterScreen
import org.np.ui.webclient.WebClientScreen

class SetUpScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: SetUpViewModel = viewModel()
        val navigator = LocalNavigator.currentOrThrow

        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column (modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { navigator.push(RegisterScreen()) }) {
                    Text("Chat with file")
                }

                Button(onClick = { navigator.push(MailRegisterScreen()) }) {
                    Text("Mail")
                }

                Button(onClick = { navigator.push(UDPChatScreen()) }) {
                    Text("Chat")
                }

                Button(onClick = { navigator.push(WebClientScreen()) }) {
                    Text("Web Browser")
                }
            }
        }
    }
}