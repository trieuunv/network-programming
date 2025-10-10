package org.np.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocketMessage(
    val event: String,
    val data: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)