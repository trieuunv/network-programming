package org.np.ui.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.np.ui.chat.ChatViewModel
import java.io.File
import javax.imageio.ImageIO

@Composable
fun MessageInput(viewModel: ChatViewModel) {
    Column (modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        viewModel.selectedFile?.let { file ->
            val isImage = remember(file) {
                file.extension.lowercase() in listOf("png", "jpg", "jpeg", "gif", "svg")
            }

            if (isImage) {
                val bitmap: ImageBitmap? = remember(file) {
                    try {
                        ImageIO.read(file)?.toComposeImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }

                bitmap?.let {
                    Image(
                        painter = BitmapPainter(bitmap),
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            } else {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "File")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(file.name, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { viewModel.pickFile() }) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Pick file"
                )
            }
            OutlinedTextField(
                value = viewModel.inputText,
                onValueChange = { viewModel.onInputChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Aa...") },
                textStyle = TextStyle(fontSize = 14.sp),
            )
            IconButton(onClick = { viewModel.sendMessage() }) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}