package org.example.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.np.ClientSocket
import org.np.TCPServer
import org.np.dto.MailAuthDto
import org.np.dto.MailDto
import org.np.dto.MailSendDto
import org.np.utils.BiMap
import org.np.utils.DateUtils
import org.np.utils.MD5Utils
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MailController {
    val server = TCPServer
    val clients = BiMap<String, ClientSocket>()

    fun start() {
        server.start()

        server.onConnection { client ->
            val clientAddr = client.socket.remoteAddress.toString()
            println("[${DateUtils.currentTime()}] Client connected: $clientAddr")
        }

        server.onDisconnection { client ->
            clients.removeByValue(client)
        }

        server.subscribe<MailAuthDto>("register") { client, data ->
            val parentFolder = File("mail_users")
            parentFolder.mkdirs()

            val userFolder = File(parentFolder, data.username);
            if (userFolder.exists()) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "register_error")
                }
                return@subscribe
            }

            val created = userFolder.mkdirs()
            if (!created) return@subscribe

            val userFile = File(userFolder, "user.txt")
            if (!userFile.exists()) {
                userFile.createNewFile()
                val now = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                userFile.writeText(
                    """
                    Username: ${data.username}
                    Password: ${MD5Utils.md5(data.password)}
                    CreatedAt: ${now.format(formatter)}
                    """.trimIndent()
                )
            }

            clients.put(data.username, client)
            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "register_success")
            }
        }

        server.subscribe<MailAuthDto>("login") { client, data ->
            val userFolder = File("mail_users", data.username)
            val userFile = File(userFolder, "user.txt")

            if (!userFile.exists()) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "login_error")
                }
            }

            val lines = userFile.readLines()
            val savedUsername = lines.find { it.startsWith("Username:") }?.substringAfter("Username:")?.trim()
            val savedPassword = lines.find { it.startsWith("Password:") }?.substringAfter("Password:")?.trim()

            if (savedUsername == data.username && savedPassword == MD5Utils.md5(data.password)) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "login_success")
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "login_error")
                }
            }

            clients.put(data.username, client)
        }

        server.subscribe<MailSendDto>("send_mail") { client, data ->
            val sender = clients.getByValue(client)
            if (sender == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "send_error")
                }
                return@subscribe
            }

            val receiverFolder = File("mail_users", data.receiver)
            if (!receiverFolder.exists()) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "send_error")
                }
                return@subscribe
            }

            val mailsFolder = File(receiverFolder, "mails")
            mailsFolder.mkdirs()

            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            val mailFile = File(mailsFolder, "${now.format(formatter)}.txt")
            val sendAt = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            mailFile.writeText(
                """
                From   : ${sender}
                Title  : ${data.title}
                Content: ${data.content}
                SentAt : ${sendAt}
                """.trimIndent()
            )

            val receiverClient = clients.getByKey(data.receiver)

            if (receiverClient != null) {
                val mail = MailDto(
                    from = sender,
                    title = data.title,
                    content = data.content,
                    sendAt = sendAt
                )

                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(receiverClient, "new_mail", mail)
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "send_success")
            }
        }

        server.subscribe("get_mails") { client ->
            val username = clients.getByValue(client)

            if (username == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "get_mails_error")
                }
            }

            val userMailFolder = File("mail_users", username)
            if (!userMailFolder.exists()) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "get_mails_error")
                }
                return@subscribe
            }

            val mailsFolder = File(userMailFolder, "mails")
            if (!mailsFolder.exists() || !mailsFolder.isDirectory) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "get_mails_rs", emptyList<MailDto>())
                }
                return@subscribe
            }

            val mailList = mailsFolder.listFiles()
                ?.filter { it.isFile }
                ?.map { file ->
                    val lines = file.readLines()
                    val from = lines.find { it.startsWith("From") }?.substringAfter(":")?.trim() ?: ""
                    val title = lines.find { it.startsWith("Title") }?.substringAfter(":")?.trim() ?: ""
                    val content = lines.find { it.startsWith("Content") }?.substringAfter(":")?.trim() ?: ""
                    val sendAt = lines.find { it.startsWith("SentAt") }?.substringAfter(":")?.trim() ?: ""

                    MailDto(from, title, content, sendAt)
                } ?: emptyList()

            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "get_mails_rs", mailList)
            }
        }
    }
}