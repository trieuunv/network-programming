package org.np.utils

import java.io.File
import java.util.Base64

object FileUtils {
    fun fileToBase64(file: File): String {
        val byteArray = file.readBytes()
        return Base64.getEncoder().encodeToString(byteArray)
    }

    fun base64ToFile(base64: String, outputPath: String) {
        val decodedBytes = Base64.getDecoder().decode(base64)
        File(outputPath).writeBytes(decodedBytes)
    }
}