package org.np.dto

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ChatRegisterDto(
    val username: String
)

@Serializable
data class ParticipantDto(
    val id: String,
    val username: String
)

@Serializable
data class MessageSendDto(
    val sender: String,
    val content: String? = null,
    val type: String,
    val file: FileSend? = null
)

@Serializable
data class FileSend(
    val filename: String,
    val filesize: Long,
    val filedata: String
)

@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val content: String? = null,
    val type: String,
    val file: FileMessage?,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class FileMessage(
    val id: String,
    val fileName: String,
    val fileUrl: String,
    val fileSize: Long,
)

@Serializable
data class DownloadFile(
    val path: String
)

@Serializable
data class FileResponse(
    val fileName: String,
    val fileSize: Long,
    val fileData: String
)