package org.np

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform