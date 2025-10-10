package org.np.utils

import java.io.File
import java.util.Base64

object FileUtils {
    fun base64ToFile(base64: String, outputPath: String) {
        val decodedBytes = Base64.getDecoder().decode(base64)
        File(outputPath).writeBytes(decodedBytes)
    }
}