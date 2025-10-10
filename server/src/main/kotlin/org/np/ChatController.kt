package org.np

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.np.dto.*
import org.np.utils.DateUtils
import org.np.utils.FileUtils
import java.io.File
import java.util.*

object ChatController {
    val server = TCPServer
    val clientParticipants = Collections.synchronizedMap(mutableMapOf<ClientSocket, ParticipantDto>())
    val messages = Collections.synchronizedList(mutableListOf<Message>())

    fun start() {
        server.start();

        server.onConnection { client ->
            CoroutineScope(Dispatchers.IO).launch {
                server.join("chat_room", client)
                val clientAddr = client.socket.remoteAddress.toString()
                println("[${DateUtils.currentTime()}] Client connected: $clientAddr")
            }
        }

        server.onDisconnection { client ->
            CoroutineScope(Dispatchers.IO).launch {
                val participant = clientParticipants.remove(client)
                if (participant != null) {
                    server.broadcastToRoom("chat_room", "participant_left", participant)
                    val message = Message(
                        id = UUID.randomUUID().toString(),
                        sender = "0",
                        content = "${participant.username} left the chat",
                        type = "system",
                        file = null,
                        timestamp = System.currentTimeMillis()
                    )

                    server.broadcastToRoom("chat_room", "message", message)
                }
            }
        }

        server.subscribe<ChatRegisterDto>("chat_register") { client, data ->
            CoroutineScope(Dispatchers.IO).launch {
                val participant = ParticipantDto(UUID.randomUUID().toString(), data.username)
                clientParticipants[client] = participant

                server.sendToClient(client, "res_register", participant)

                server.broadcastToRoom("chat_room", "new_participant", participant)

                val message = Message(
                    id = UUID.randomUUID().toString(),
                    sender = "0",
                    content = "${data.username} joined the chat",
                    type = "system",
                    file = null,
                    timestamp = System.currentTimeMillis()
                )

                server.broadcastToRoom("chat_room", "message", message)
            }
        }

        server.subscribe("get_initial_participant") { client ->
            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "initial_participant", clientParticipants.values.toList())
            }
        }

        server.subscribe<MessageSendDto>("message") { client, messsageSend ->
            var fileMessage: FileMessage? = null

            if (messsageSend.type == "file" || messsageSend.type == "image") {
                messsageSend.file?.let { file ->
                    val safeFileName = "${UUID.randomUUID()}_${file.filename}"
                    val outputPath = "uploads/$safeFileName"
                    FileUtils.base64ToFile(file.filedata, outputPath)
                    fileMessage = FileMessage(
                        id = UUID.randomUUID().toString(),
                        fileName = file.filename,
                        fileUrl = outputPath,
                        fileSize = file.filesize
                    )
                }
            }

            val newMessage = Message(
                id = UUID.randomUUID().toString(),
                sender = messsageSend.sender,
                content = messsageSend.content,
                type = messsageSend.type,
                file = fileMessage,
                timestamp = System.currentTimeMillis()
            )

            messages.add(newMessage)

            CoroutineScope(Dispatchers.IO).launch {
                server.broadcastToRoom("chat_room", "message", newMessage)
            }
        }

        server.subscribe<DownloadFile>("download_file") { client, request ->
            CoroutineScope(Dispatchers.IO).launch {
                val file = File(request.path)
                if (file.exists()) {
                    val fileData = Base64.getEncoder().encodeToString(file.readBytes())
                    val fileResponse = FileResponse(
                        fileName = file.name,
                        fileData = fileData,
                        fileSize = file.length()
                    )
                    server.sendToClient(client, "res_download_file", fileResponse)
                } else {
                    // server.sendToClient(client, "res_download_file_error", "File not found")
                }
            }
        }
    }
}