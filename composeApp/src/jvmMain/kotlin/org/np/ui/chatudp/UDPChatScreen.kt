package org.np.ui.chatudp

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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen

class UDPChatScreen : Screen {
    @Composable
    override fun Content() {
        var inputUsername by remember { mutableStateOf("") }
        var isUsernameSet by remember { mutableStateOf(false) }
        var targetUser by remember { mutableStateOf<String?>(null) }
        var targetGroup by remember { mutableStateOf<String?>(null) }
        var selectedTabIndex by remember { mutableStateOf(0) }

        val viewModel: UDPChatVM = viewModel(
            key = if (isUsernameSet && inputUsername.isNotBlank()) inputUsername else "Guest"
        ) {
            UDPChatVM(username = if (isUsernameSet && inputUsername.isNotBlank()) inputUsername else "Guest")
        }

        val isConnected by viewModel.isConnected.collectAsState(false)
        val status by viewModel.status.collectAsState("Chưa khởi tạo")
        val messages = viewModel.messages
        val userList = viewModel.userList
        val groupList = viewModel.groupList
        var messageInput by remember { mutableStateOf("") }
        val listState = rememberLazyListState()
        val currentUsername = viewModel.username

        val isPrivateMode = targetUser != null
        val isGroupMode = targetGroup != null

        val inputLabel = when {
            isPrivateMode -> "Nhắn riêng đến ${targetUser!!}..."
            isGroupMode -> "Nhắn nhóm ${targetGroup!!}..."
            else -> "Nhập tin nhắn chung..."
        }
        val sendButtonText = when {
            isPrivateMode -> "Gửi Riêng"
            isGroupMode -> "Gửi Nhóm"
            else -> "Gửi Chung"
        }

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
        }

        LaunchedEffect(isConnected) {
            if (!isConnected) {
                targetUser = null
                targetGroup = null
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text("Unicast Chat (${if (isConnected) "Online" else "Offline"})")
                })
            },
            bottomBar = {
                Column {
                    val activeModeText = when {
                        isPrivateMode -> "Chế độ riêng tư: Gửi đến ${targetUser!!}"
                        isGroupMode -> "Chế độ nhóm: Gửi đến ${targetGroup!!}"
                        else -> null
                    }

                    if (activeModeText != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isPrivateMode) Color(0xFFFFF0D0) else Color(0xFFD0FFF0))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                activeModeText,
                                color = Color.Black,
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                targetUser = null
                                targetGroup = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Hủy chế độ chat")
                            }
                        }
                    }

                    Text(
                        text = status,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

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
                            enabled = isConnected,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                when {
                                    isPrivateMode -> viewModel.sendPrivateMessage(targetUser!!, messageInput)
                                    isGroupMode -> viewModel.sendGroupMessage(targetGroup!!, messageInput)
                                    else -> viewModel.sendMessage(messageInput)
                                }
                                messageInput = ""
                            },
                            enabled = isConnected && messageInput.isNotBlank()
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
                // Sidebar cho User và Group
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight()
                        .border(1.dp, Color.LightGray)
                ) {
                    TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Users (${userList.size})") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = {
                                selectedTabIndex = 1
                                if (isConnected) viewModel.requestGroupList()
                            },
                            text = { Text("Nhóm (${groupList.size})") }
                        )
                    }

                    if (selectedTabIndex == 0) {
                        UserListPanel(
                            userList = userList,
                            currentUsername = currentUsername,
                            targetUser = targetUser,
                            isConnected = isConnected,
                            onUserClick = { user ->
                                targetUser = if (targetUser == user) null else user
                                targetGroup = null
                            }
                        )
                    } else {
                        GroupListPanel(
                            groupList = groupList,
                            targetGroup = targetGroup,
                            isConnected = isConnected,
                            onCreateGroup = viewModel::createGroup,
                            onJoinGroup = viewModel::joinGroup,
                            onLeaveGroup = viewModel::leaveGroup, // Truyền hàm rời nhóm
                            onGroupClick = { group ->
                                targetGroup = if (targetGroup == group) null else group
                                targetUser = null
                            }
                        )
                    }
                }

                // Chat main
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
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
                            onClick = { if (inputUsername.isNotBlank()) isUsernameSet = true },
                            enabled = !isUsernameSet && inputUsername.isNotBlank()
                        ) { Text("Set") }
                    }

                    Button(
                        onClick = {
                            if (isConnected) {
                                viewModel.leaveRoom()
                            } else {
                                viewModel.joinRoom()
                            }
                        },
                        enabled = isUsernameSet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(if (isConnected) "Leave Server" else "Join Server")
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp)
                            .background(Color.White)
                    ) {
                        items(messages) { message -> MessageBubble(message, currentUsername) }
                    }
                }
            }
        }
    }

    @Composable
    private fun UserListPanel(
        userList: List<String>,
        currentUsername: String,
        targetUser: String?,
        isConnected: Boolean,
        onUserClick: (String) -> Unit
    ) {
        LazyColumn(modifier = Modifier.padding(8.dp)) {
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
                        .clickable(enabled = isConnected && !isSelf) { onUserClick(user) }
                        .background(if (isTarget) Color(0xFFE0E0FF) else Color.Transparent)
                        .padding(4.dp)
                )
            }
        }
    }

    @Composable
    private fun GroupListPanel(
        groupList: List<String>,
        targetGroup: String?,
        isConnected: Boolean,
        onCreateGroup: (String) -> Unit,
        onJoinGroup: (String) -> Unit,
        onLeaveGroup: (String) -> Unit, // ✅ Nhận hàm rời nhóm
        onGroupClick: (String) -> Unit
    ) {
        var newGroupName by remember { mutableStateOf("") }
        var showCreateDialog by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

            Button(
                onClick = { showCreateDialog = true },
                enabled = isConnected,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Tạo nhóm")
                Spacer(Modifier.width(4.dp))
                Text("Tạo Nhóm Mới")
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("Tạo Nhóm Mới") },
                    text = {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Tên nhóm") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onCreateGroup(newGroupName)
                                newGroupName = ""
                                showCreateDialog = false
                            },
                            enabled = newGroupName.isNotBlank()
                        ) { Text("Tạo") }
                    },
                    dismissButton = {
                        Button(onClick = { showCreateDialog = false }) { Text("Hủy") }
                    }
                )
            }

            Divider(Modifier.padding(vertical = 4.dp))
            Text("Các Nhóm (Click để tham gia/chat)", style = MaterialTheme.typography.caption)

            LazyColumn {
                items(groupList) { group ->
                    val isTarget = group == targetGroup

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(if (isTarget) Color(0xFFE0F0FF) else Color.Transparent)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💬 $group",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = isConnected) { onGroupClick(group) }
                        )

                        if (isConnected) {
                            if (isTarget) {
                                // Tình huống 1: ĐANG chat nhóm này (Giả định là thành viên)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = {
                                            // 1. Gửi lệnh Rời nhóm
                                            onLeaveGroup(group)
                                            // 2. Thoát chế độ chat nhóm
                                            onGroupClick(group)
                                        },
                                        modifier = Modifier.height(24.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFCCBC))
                                    ) {
                                        Text("Rời nhóm", style = MaterialTheme.typography.caption)
                                    }
                                    Icon(Icons.Default.Send, contentDescription = "Đang chat", tint = MaterialTheme.colors.primary)
                                }
                            } else {
                                // Tình huống 2: CHƯA chat nhóm này
                                Button(
                                    onClick = {
                                        // Gửi lệnh tham gia và tự động chuyển sang chat nhóm đó
                                        onJoinGroup(group)
                                        onGroupClick(group)
                                    },
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFC8E6C9))
                                ) {
                                    Text("Tham gia", style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MessageBubble(message: String, currentUsername: String) {
        // Kiểm tra các loại tin nhắn đặc biệt
        val isSystemMessage = listOf("📡", "👋", "❌", "⚠️", "🚪", "✅", "📢").any { message.contains(it) }

        // ✅ BỔ SUNG: Tin nhắn riêng tư đã gửi (được ViewModel tạo ra)
        // Format: 💌 (đến [receiver]): content
        val isPrivateSent = message.startsWith("💌 (đến ")

        // Tin nhắn riêng tư nhận được (từ Server)
        // Format: 💌 [sender]: content
        val isPrivateReceived = message.startsWith("💌 ") && !isPrivateSent

        val isGroupMessage = message.startsWith("[Nhóm ")

        // Tin nhắn nhóm của chính mình (Server gửi lại)
        // Format: [Nhóm General] [Alice]: Hello
        val isGroupMyMessage = isGroupMessage && message.contains("[$currentUsername]:")

        // Tin nhắn chung của chính mình (Server gửi lại)
        // Format: [Alice]: Hello
        val isMyMessage = !isGroupMessage && !isPrivateReceived && !isPrivateSent && message.startsWith("[$currentUsername]")

        // Phân loại màu nền (Ưu tiên kiểm tra các loại tin nhắn của chính mình trước)
        val backgroundColor = when {
            isSystemMessage -> Color.LightGray         // Hệ thống/Thông báo
            isPrivateSent -> Color(0xFFC6E7F8)         // Tin riêng đã gửi (Xanh dương nhạt)
            isPrivateReceived -> Color(0xFFE0E0FF)     // Tin riêng nhận (Tím nhạt)
            isMyMessage || isGroupMyMessage -> Color(0xFFDCF8C6) // Tin của mình (Chung hoặc Nhóm)
            isGroupMessage -> Color(0xFFFFF7DB)         // Tin nhóm của người khác (Vàng nhạt)
            else -> Color.White                         // Tin chung của người khác
        }

        // Sắp xếp tin nhắn
        val alignment = when {
            // Tin nhắn của chính mình (chung, nhóm, hoặc riêng đã gửi) được đẩy sang phải
            isMyMessage || isGroupMyMessage || isPrivateSent -> Arrangement.End
            else -> Arrangement.Start
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = alignment
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                backgroundColor = backgroundColor,
                elevation = 1.dp,
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                Text(
                    text = message,
                    color = if (isSystemMessage) Color.DarkGray else Color.Black,
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