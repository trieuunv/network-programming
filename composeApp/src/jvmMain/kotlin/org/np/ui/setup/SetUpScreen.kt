package org.np.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.np.ui.mailregister.MailRegisterScreen
import org.np.ui.register.RegisterScreen

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
                TextField(
                    value = viewModel.host,
                    onValueChange = { viewModel.onHostChange(it) },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    label = { Text("Host") })

                TextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.onPortChange(it) },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    label = { Text("Port") })

                Button(onClick = { viewModel.connect(); navigator.push(RegisterScreen()) }) {
                    Text("Connect to Chat")
                }

                Button(onClick = { viewModel.connect(); navigator.push(MailRegisterScreen()) }) {
                    Text("Connect to Mail")
                }
            }
        }
    }
}