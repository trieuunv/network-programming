package org.np.dto

import kotlinx.serialization.Serializable

@Serializable
data class MailAuthDto(
    val username: String,
    val password: String,
)

@Serializable
data class MailSendDto(
    val receiver: String,
    val title: String,
    val content: String
)

@Serializable
data class MailDto (
    val from: String,
    val title: String,
    val content: String,
    val sendAt: String
)