package org.np.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
    fun currentTime(): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return now.format(formatter)
    }
}