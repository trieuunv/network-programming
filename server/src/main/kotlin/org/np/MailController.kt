package org.example.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction
import org.np.ClientSocket
import org.np.TCPServer
import org.np.dto.MailAuthDto
import org.np.dto.MailDto
import org.np.dto.MailSendDto
import org.np.model.Mail
import org.np.model.User
import org.np.utils.BiMap
import org.np.utils.DateUtils
import org.np.utils.MD5Utils
import java.io.File
import java.time.Instant
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
            val existed = transaction {
                User.find { org.np.model.Users.username eq data.username }.firstOrNull()
            }

            if (existed != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "register_error")
                }
                return@subscribe
            }

            transaction {
                User.new {
                    username = data.username
                    email = data.email ?: "${data.username}@example.com"
                    password = MD5Utils.md5(data.password)
                    fullName = data.username
                    avatar = null
                    createDate = Instant.now()
                }
            }

            clients.put(data.username, client)
            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "register_success")
            }
        }

        server.subscribe<MailAuthDto>("login") { client, data ->
            val user = transaction {
                User.find { org.np.model.Users.username eq data.username }.firstOrNull()
            }

            if (user == null || user.password != MD5Utils.md5(data.password)) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "login_error")
                }
                return@subscribe
            }

            clients.put(user.username, client)

            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "login_success")
            }
        }

        server.subscribe<MailSendDto>("send_mail") { client, data ->
            val senderUsername = clients.getByValue(client)
            if (senderUsername == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "send_error")
                }
                return@subscribe
            }

            val sender = transaction {
                User.find { org.np.model.Users.username eq senderUsername }.firstOrNull()
            }

            val receiver = transaction {
                User.find { org.np.model.Users.username eq data.receiver }.firstOrNull()
            }

            if (sender == null || receiver == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "send_error")
                }
                return@subscribe
            }

            val mail = transaction {
                Mail.new {
                    this.sender = sender
                    this.recipient = receiver
                    this.subject = data.title
                    this.content = data.content
                    this.sentDate = Instant.now()
                    this.isRead = false
                }
            }

            // Gửi mail real-time nếu người nhận đang online
            val receiverClient = clients.getByKey(receiver.username)
            if (receiverClient != null) {
                val mailDto = MailDto(
                    from = sender.username,
                    title = mail.subject,
                    content = mail.content,
                    sendAt = mail.sentDate.toString()
                )
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(receiverClient, "new_mail", mailDto)
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
                return@subscribe
            }

            val user = transaction {
                User.find { org.np.model.Users.username eq username }.firstOrNull()
            }

            if (user == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    server.sendToClient(client, "get_mails_error")
                }
                return@subscribe
            }

            val mailList = transaction {
                Mail.find { org.np.model.Mails.recipient eq user.id }
                    .map {
                        MailDto(
                            from = it.sender.username,
                            title = it.subject,
                            content = it.content,
                            sendAt = it.sentDate.toString()
                        )
                    }
            }

            CoroutineScope(Dispatchers.IO).launch {
                server.sendToClient(client, "get_mails_rs", mailList)
            }
        }
    }
}