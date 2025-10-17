package org.np.ui.chatmulticast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import androidx.compose.ui.window.application

class ChatMulticastScreen : Screen {
    @Composable
    override fun Content() {
        var inputUsername by remember { mutableStateOf("") }
        var isUsernameSet by remember { mutableStateOf(false) }

        // TRẠNG THÁI MỚI: Lưu người dùng đang được chọn để gửi tin riêng
        var targetUser by remember { mutableStateOf<String?>(null) }

        val viewModel: MulticastVM = viewModel(
            key = if (isUsernameSet && inputUsername.isNotBlank()) inputUsername else "Guest"
        ) {
            MulticastVM(username = if (isUsernameSet && inputUsername.isNotBlank()) inputUsername else "Guest")
        }

        val isJoined by viewModel.isJoined.collectAsState(false)
        val status by viewModel.status.collectAsState("Chưa khởi tạo")
        val messages = viewModel.messages
        val userList = viewModel.userList

        var messageInput by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        val currentUsername = viewModel.username

        // Xác định loại tin nhắn và người nhận
        val isPrivateMode = targetUser != null
        val inputLabel = if (isPrivateMode) "Nhắn riêng đến ${targetUser!!}..." else "Nhập tin nhắn chung..."
        val sendButtonText = if (isPrivateMode) "Gửi Riêng" else "Gửi (Chung)"

        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text("Multicast Chat (${if (isJoined) "Online" else "Offline"})")
                })
            },
            bottomBar = {
                Column {
                    // Hiển thị thanh thông báo chế độ riêng tư (nếu có)
                    if (isPrivateMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF0D0)) // Màu vàng nhạt
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Chế độ riêng tư: Gửi đến ${targetUser!!}",
                                color = Color.Black,
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { targetUser = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Hủy tin nhắn riêng")
                            }
                        }
                    }

                    // Thanh trạng thái
                    Text(
                        text = status,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Thanh nhập tin nhắn (đã sửa đổi cho P2P)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            label = { Text(inputLabel) },
                            enabled = isJoined,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (isPrivateMode) {
                                    viewModel.sendPrivateMessage(targetUser!!, messageInput)
                                } else {
                                    viewModel.sendMessage(messageInput)
                                }
                                messageInput = ""
                                // Nếu gửi riêng thành công, trở về chế độ chung
                                if(isPrivateMode) targetUser = null
                            },
                            enabled = isJoined && messageInput.isNotBlank()
                        ) {
                            Text(sendButtonText)
                        }
                    }
                }
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // --- SIDEBAR DANH SÁCH NGƯỜI DÙNG (CÓ CHỨC NĂNG CLICK) ---
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .border(1.dp, Color.LightGray)
                        .padding(8.dp)
                ) {
                    Text("Người dùng online (${userList.size})", style = MaterialTheme.typography.subtitle1)
                    Divider(Modifier.padding(vertical = 4.dp))
                    LazyColumn {
                        items(userList) { user ->
                            val isSelf = user == currentUsername
                            val isTarget = user == targetUser
                            val color = if (isSelf) MaterialTheme.colors.primary else Color.Black

                            Text(
                                text = if (isSelf) "$user (Bạn)" else user,
                                color = color,
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(enabled = isJoined && !isSelf) {
                                        // Khi click, chọn/hủy chọn làm người nhận riêng tư
                                        targetUser = if (isTarget) null else user
                                    }
                                    .background(if (isTarget) Color(0xFFE0E0FF) else Color.Transparent) // Highlight người đang chọn
                            )
                        }
                    }
                }

                // --- KHU VỰC CHAT CHÍNH ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // --- Nhập username ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputUsername,
                            onValueChange = { inputUsername = it },
                            label = { Text("Nhập Username") },
                            enabled = !isUsernameSet,
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputUsername.isNotBlank()) {
                                    isUsernameSet = true
                                }
                            },
                            enabled = !isUsernameSet && inputUsername.isNotBlank()
                        ) {
                            Text("Set")
                        }
                    }

                    // --- Nút Join / Leave ---
                    Button(
                        onClick = if (isJoined) viewModel::leaveRoom else viewModel::joinRoom,
                        enabled = isUsernameSet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isJoined) "Leave Room" else "Join Room")
                    }

                    // --- Danh sách tin nhắn ---
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp)
                            .background(Color.White)
                    ) {
                        items(messages) { message ->
                            MessageBubble(message, currentUsername)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MessageBubble(message: String, currentUsername: String) {
        val isSystemMessage = message.contains("📡") ||
                message.contains("👋") ||
                message.contains("❌") ||
                message.contains("⚠️") ||
                message.contains("🚪")

        val isPrivateSent = message.startsWith("💌 (đến ") // Tin nhắn riêng đã gửi
        val isPrivateReceived = message.startsWith("💌 ") // Tin nhắn riêng nhận được

        val isMyMessage = message.startsWith("[$currentUsername]")

        val backgroundColor = when {
            isSystemMessage -> Color.LightGray
            isPrivateSent -> Color(0xFFC6E7F8) // Xanh nhạt gửi đi
            isPrivateReceived -> Color(0xFFE0E0FF) // Tím nhạt nhận được
            isMyMessage -> Color(0xFFDCF8C6) // Xanh lá chung gửi đi
            else -> Color.White
        }

        val textColor = if (isSystemMessage) Color.DarkGray else Color.Black

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = when {
                isPrivateSent || isMyMessage -> Arrangement.End
                else -> Arrangement.Start
            }
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                backgroundColor = backgroundColor,
                elevation = 1.dp,
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                Text(
                    text = if (isPrivateReceived) "[TIN RIÊNG] $message" else message,
                    color = textColor,
                    style = if (isSystemMessage)
                        MaterialTheme.typography.caption
                    else
                        MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}