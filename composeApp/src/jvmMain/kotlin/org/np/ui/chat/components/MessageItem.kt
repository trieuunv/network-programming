package org.np.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.np.dto.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MessageItem(currentUserUsername: String?, message: Message, onDownload: (Message) -> Unit) {
    val isMe = message.sender == currentUserUsername
    val isSystem = message.type == "system"
    val backgroundColor = if (isMe) Color(0xFFD1FFC4) else Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Hiển thị tên sender nếu không phải mình
        if (!isMe && !isSystem) {
            Text(
                text = message.sender,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        when (message.type) {
            "image" -> {
                Column(
                    modifier = Modifier.widthIn(max = 250.dp)
                ) {
                    if (message.file?.fileUrl != null) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = "http://localhost:8080/${message.file?.fileUrl}",
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            IconButton(
                                onClick = { onDownload(message) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color.White
                                )
                            }
                        }

                        // timestamp ở dưới ảnh
                        Text(
                            text = formatTimestamp(message.timestamp),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 2.dp, end = 4.dp)
                        )
                    }
                }
            }

            "file" -> {
                Column(
                    modifier = Modifier.widthIn(max = 250.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFEDEDED), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📄", fontSize = 20.sp) // icon file
                        Text(
                            text = message.file?.fileName ?: "unknown",
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onDownload(message)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                tint = Color.Black
                            )
                        }

                    }

                    // timestamp
                    Text(
                        text = formatTimestamp(message.timestamp),
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 2.dp, end = 4.dp)
                    )
                }
            }

            "system" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.content ?: "",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            else -> {
                // Text message (bubble)
                Box(
                    modifier = Modifier
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isMe) 12.dp else 0.dp,
                                bottomEnd = if (isMe) 0.dp else 12.dp
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 250.dp)
                ) {
                    Column {
                        Text(
                            text = message.content ?: "",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                        Text(
                            text = formatTimestamp(message.timestamp),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}


