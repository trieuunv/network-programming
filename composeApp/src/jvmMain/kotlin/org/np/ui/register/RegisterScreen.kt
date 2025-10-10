package org.np.ui.register

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import networkprogramming.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.*
import org.np.ui.chat.ChatScreen

class RegisterScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: RegisterViewModel = viewModel()
        val username = viewModel.username
        val registeredUser = viewModel.registeredUser
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(registeredUser) {
            registeredUser?.let {
                navigator.push(ChatScreen(registeredUser))
            }
        }

        Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.Start) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color.LightGray),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeAnimation()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text(text = "Welcome to ChatBox", modifier = Modifier.padding(8.dp))
                Text(text = "Please register to continue", modifier = Modifier.padding(8.dp))

                TextField(
                    value = username,
                    onValueChange = { viewModel.onUsernameChanged(it) },
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    label = { Text("Username") })

                Button(
                    onClick = {viewModel.register()}, modifier = Modifier.fillMaxWidth().padding(8.dp), enabled = username.isNotBlank()
                ) {
                    Text("Register")
                }
            }
        }
    }
}

@Composable
fun WelcomeAnimation() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/welcome.json").decodeToString()
        )
    }
    val progress by animateLottieCompositionAsState(composition)

    Image(
        painter = rememberLottiePainter(
            composition = composition, iterations = Compottie.IterateForever

        ), contentDescription = "Lottie animation"
    )
}