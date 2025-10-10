package org.np.ui.mailregister

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import kotlinx.coroutines.flow.collectLatest
import org.np.ui.common.PasswordField
import org.np.ui.mail.MailScreen

class MailRegisterScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel: MailRegisterVM = viewModel()
        val navigator = LocalNavigator.currentOrThrow
        var showDialog by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            viewModel.navigationEvent.collect { event ->
                when(event) {
                    "toHome" -> navigator.push(MailScreen())
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.registerError.collectLatest { error ->
                showDialog = error
            }
        }

        if (showDialog != null) {
            AlertDialog(
                onDismissRequest = { showDialog = null },
                title = { Text("Error") },
                text = { Text(showDialog ?: "") },
                confirmButton = {
                    TextButton(onClick = { showDialog = null }) {
                        Text("OK")
                    }
                }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight(), horizontalArrangement = Arrangement.Start) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color.LightGray),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                org.np.ui.register.WelcomeAnimation()
            }

            Column (modifier = Modifier.fillMaxWidth(.5f).fillMaxHeight().padding(20.dp)) {
                SegmentedButtonsDemo(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtonsDemo(viewModel: MailRegisterVM) {
    var selectedIndex by remember { mutableStateOf(0) }
    val options = listOf("Register", "Login")

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, text ->
            SegmentedButton(
                selected = selectedIndex == index,
                onClick = { selectedIndex = index },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size
                )
            ) {
                Text(text)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    when (selectedIndex) {
        0 -> RegisterView(viewModel)
        1 -> LoginView(viewModel)
    }
}

@Composable
fun RegisterView(viewModel: MailRegisterVM) {
    Column (
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = viewModel.registerUsername,
            onValueChange = { viewModel.onChangeRegisterUsername(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") })

        PasswordField("Password", viewModel.registerPassword, { newValue -> viewModel.onChangeRegisterPassword(newValue) }, false)

        PasswordField("Password Confirm", viewModel.registerPasswordAgain, { newValue -> viewModel.onChangeRegisterPasswordAgain(newValue) }, viewModel.registerPassword != viewModel.registerPasswordAgain)

        Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.register() }) {
            Text("Register")
        }
    }
}

@Composable
fun LoginView(viewModel: MailRegisterVM) {
    Column (
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = viewModel.loginUsername,
            onValueChange = { viewModel.onChangeLoginUsername(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") })

        PasswordField("Password", viewModel.loginPassword, { newValue -> viewModel.onChangeLoginPassword(newValue) }, false)

        Button(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.login() }) {
            Text("Login")
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