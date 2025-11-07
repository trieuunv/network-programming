package org.np

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities

fun main() = application {
    // Khởi tạo JavaFX một lần duy nhất
    initializeJavaFX()

    Window(
        onCloseRequest = ::exitApplication,
        title = "CloudBox",
    ) {
        App()
    }
}

/**
 * Khởi tạo JavaFX Platform một lần duy nhất và giữ nó sống suốt lifecycle
 */
private fun initializeJavaFX() {
    val latch = CountDownLatch(1)

    SwingUtilities.invokeLater {
        try {
            // Trick để init JavaFX toolkit
            JFXPanel()
            latch.countDown()
        } catch (e: Exception) {
            println("⚠️ JavaFX already initialized or error: ${e.message}")
            latch.countDown()
        }
    }

    try {
        // Đợi JavaFX khởi tạo xong (timeout 5 giây)
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)

        // QUAN TRỌNG: Đặt implicit exit = false để JavaFX không tự động thoát
        Platform.setImplicitExit(false)

        println("✅ JavaFX Platform initialized successfully")
    } catch (e: Exception) {
        println("❌ Failed to initialize JavaFX: ${e.message}")
    }
}