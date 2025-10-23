package org.np

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.example.project.MailController
import java.io.File

fun main() {
    runBlocking {
        launch(Dispatchers.Default) {
            embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
                .start(wait = true)
        }

        launch(Dispatchers.IO) {
            ChatController.start()
            MailController.start()
            UDPServer().start()
        }
    }
}

fun Application.module() {
    routing {
        staticResources("/", "static") {
            defaultResource("index.html", "static")
        }

        staticFiles("/uploads", File("uploads"))
    }
}