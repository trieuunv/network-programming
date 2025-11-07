package org.np.dto

import kotlinx.serialization.Serializable

@Serializable
data class MailAuthDto(
    val username: String,
    val password: String,
    val email: String? = null,
    val fullName: String? = null,
    val avatar: String? = null
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