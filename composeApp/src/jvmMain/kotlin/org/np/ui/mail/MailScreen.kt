package org.np.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import org.np.dto.MailDto

class MailScreen : Screen {
    @Composable
    override fun Content() {
        NavigationRailDemo()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRailDemo() {
    val viewModel: MailVM = viewModel()
    var searchText by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Send")
    val sendError by viewModel.sendError.collectAsState()
    val isSuccessNow by viewModel.isSuccessNow.collectAsState()

    if (sendError != "") {
        AlertDialog(
            onDismissRequest = { viewModel.setSendError("") },
            title = { Text("Error") },
            text = { Text(sendError ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.setSendError("") }) {
                    Text("OK")
                }
            }
        )
    }

    if (isSuccessNow) {
        AlertDialog(
            onDismissRequest = { viewModel.setSuccess(false) },
            title = { Text("Notification") },
            text = { Text("Send Mail Successfully") },
            confirmButton = {
                TextButton(onClick = { viewModel.setSuccess(false) }) {
                    Text("OK")
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight().background(Color(0xFFEFEFEF))
        ) {
            items.forEachIndexed { index, label ->
                NavigationRailItem(
                    selected = selectedItem == index,
                    onClick = { selectedItem = index },
                    icon = {
                        when (index) {
                            0 -> Icon(Icons.Default.Home, contentDescription = "Home")
                            1 -> Icon(Icons.Default.PersonAdd, contentDescription = "Register")
                            2 -> Icon(Icons.Default.Login, contentDescription = "Login")
                        }
                    },
                    label = { Text(label) }
                )
            }
        }

        Scaffold(
            containerColor = Color.White,
            topBar = {}
        ) {
            when (selectedItem) {
                0 -> MailListView(viewModel.mails)
                1 -> ComposeLetter(viewModel)
            }
        }
    }
}

@Composable
fun MailListView(mails: List<MailDto>) {
    var searchText by remember { mutableStateOf("") }
    var currentMail by remember { mutableStateOf<MailDto?>(null) }

    if (currentMail != null) {
        MailDetailView(currentMail!!) {
            currentMail = null
        }
    } else {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Tìm kiếm...") },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                    )
            ) {
                val filtered = mails.filter {
                    it.title.contains(searchText, ignoreCase = true) ||
                            it.from.contains(searchText, ignoreCase = true)
                }

                MailList(
                    mails = filtered,
                    onClickItem = { mail ->
                        currentMail = mail
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailList(
    mails: List<MailDto>,
    onClickItem: (MailDto) -> Unit
) {
    var selectedItem by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(mails.size) { index ->
            val mail = mails[index]
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedItem = mail.sendAt
                        onClickItem(mail)
                    }
                    .background(
                        color = if (selectedItem == mail.sendAt) Color(0xFFE0E0E0) else Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                headlineContent = { Text(text = mail.title) },
                supportingContent = { Text(text = mail.content) },
                overlineContent = { Text(text = "From: ${mail.from}") },
                trailingContent = {
                    Text(
                        text = mail.sendAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            )
        }
    }
}

@Composable
fun ComposeLetter(viewModel: MailVM) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxSize()
    ) {
        Text("Compose a Letter")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = viewModel.sendState.username,
            onValueChange = { viewModel.onChangeUsername(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") })

        OutlinedTextField(
            value = viewModel.sendState.title,
            onValueChange = { viewModel.onChangeTitle(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") })

        OutlinedTextField(
            value = viewModel.sendState.content,
            onValueChange = { viewModel.onChangeContent(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Content") })

        Button(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), onClick = { viewModel.sendMail() }) {
            Text("Send")
        }
    }
}

@Composable
fun MailDetailView(mail: MailDto, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Title: ${mail.title}", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Content: ${mail.content}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Send At: ${mail.sendAt}", style = MaterialTheme.typography.bodyLarge)
    }
}
