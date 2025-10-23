package org.np

import io.ktor.http.*
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
        staticResources("/static", "static")

        get("/") {
            val resourcePath = "static/index.html"
            // Tìm và đọc file từ classpath (thư mục resources/static)
            val resourceUrl = this::class.java.classLoader.getResource(resourcePath)

            if (resourceUrl != null) {
                val content = resourceUrl.readText()
                call.respondText(content, ContentType.Text.Html)
            } else {
                call.respondText("404: Không tìm thấy file index.html", status = HttpStatusCode.NotFound)
            }
        }

        get("/home") {
            val resourcePath = "static/home.html"
            val resourceUrl = this::class.java.classLoader.getResource(resourcePath)

            if (resourceUrl != null) {
                val content = resourceUrl.readText()
                call.respondText(content, ContentType.Text.Html)
            } else {
                call.respondText("404: Không tìm thấy file home.html", status = HttpStatusCode.NotFound)
            }
        }

        staticFiles("/uploads", File("uploads"))
    }
}