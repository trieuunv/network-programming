package org.np

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun startWebServer(port: Int = 8080) {
    embeddedServer(Netty, port = port) {
        routing {
            static("/") {
                resources("static")
                defaultResource("static/index.html")
            }
        }
    }.start(wait = false)
}
