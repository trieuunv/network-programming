package org.np.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateFileRequestDto(
    val filename: String,
    val fileSize: Long,
    val fileData: String?,
    val mimeType: String?,
    val storagePath: String,
    val isFolder: Boolean,
    val parentId: Long,
)