package org.np.ui.chat

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.np.TCPClient
import org.np.dto.*
import org.np.utils.FileUtils
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

class ChatViewModel: ViewModel() {
    val client = TCPClient

    var user by mutableStateOf<ParticipantDto?>(null)
        private set

    var pendingDownloadPath: String? = null

    val participants = mutableStateListOf<ParticipantDto>()

    val messages = mutableStateListOf<Message>()

    var inputText by mutableStateOf("")
        private set

    var selectedFile: File? by mutableStateOf(null)
        private set

    init {
        client.on<List<ParticipantDto>>("initial_participant") { initialParticipants ->
            initialParticipants?.let {
                MainScope().launch {
                    participants.clear()
                    participants.addAll(it)
                }
            }
        }

        client.emit("get_initial_participant")

        client.on<Message>("message") { message ->
            if (message != null) {
                messages.add(message)
            }
        }

        client.on<ParticipantDto>("new_participant") { participant ->
            if (participant != null) {
                participants.add(participant)
            }
        }

        client.on<FileResponse>("res_download_file") { fileResponse ->
            if (fileResponse != null && pendingDownloadPath != null) {
                FileUtils.base64ToFile(fileResponse.fileData, pendingDownloadPath!!)
                pendingDownloadPath = null
            }
        }

        client.on<ParticipantDto>("participant_left") { participant ->
            if (participant != null) {
                participants.removeIf { it.id == participant.id }
            }
        }
    }

    fun onInputChange(newText: String) {
        inputText = newText
    }

    fun updateUser(newUser: ParticipantDto) {
        user = newUser
    }

    fun pickFile() {
        val dialog = FileDialog(null as Frame?, "Select File", FileDialog.LOAD)
        dialog.isVisible = true
        val chosenFile = dialog.file?.let { File(dialog.directory, it) }
        selectedFile = chosenFile
    }

    fun sendMessage() {
        user?.let { u ->
            if (inputText.isNotBlank()) {
                val dto = MessageSendDto(
                    sender = u.username,
                    content = inputText,
                    type = "text"
                )

                client.emit("message", dto)
                inputText = ""
            }

            selectedFile?.let { file ->
                val encoded = FileUtils.fileToBase64(file)

                val ext = file.extension.lowercase()
                val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
                val fileType = if (ext in imageExtensions) "image" else "file"

                val fileSend = FileSend(
                    filename = file.name,
                    filesize = file.length(),
                    filedata = encoded
                )
                val dto = MessageSendDto(
                    sender = u.username,
                    content = null,
                    type = fileType,
                    file = fileSend
                )
                client.emit("message", dto)
                selectedFile = null
            }
        }
    }

    fun downloadFile(path: String?, fileNamePreview: String?) {
        if (path != null) {
            val dialog = FileDialog(null as Frame?, "Save File", FileDialog.SAVE)
            dialog.file = fileNamePreview ?: ""
            dialog.isVisible = true

            val chosenFile = dialog.file?.let { File(dialog.directory, it) }
            if (chosenFile != null) {
                pendingDownloadPath = chosenFile.absolutePath
                client.emit("download_file", DownloadFile(path))
            }
        }
    }
}