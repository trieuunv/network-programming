package org.np.ui.webclient

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView

/**
 * Hiển thị chuỗi HTML tĩnh bằng JavaFX WebView.
 */
@Composable
fun HtmlViewer(
    html: String,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val jfxPanel = JFXPanel()
            Platform.runLater {
                val view = WebView()
                view.engine.loadContent(html, "text/html")
                jfxPanel.scene = Scene(view)
            }
            jfxPanel
        },
        update = { jfxPanel ->
            Platform.runLater {
                val webView = (jfxPanel.scene?.root as? WebView)

                if (webView != null) {
                    val currentContent = webView.engine.executeScript("document.documentElement.outerHTML").toString()
                    if (!currentContent.contains(html.take(50))) {
                        webView.engine.loadContent(html, "text/html")
                    }
                }
            }
        }
    )
}

/**
 * Biến thể cho phép load trực tiếp URL.
 */
@Composable
fun HtmlWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            val jfxPanel = JFXPanel()
            // Chỉ tạo WebView và Scene một lần.
            Platform.runLater {
                val view = WebView()
                view.engine.load(url)
                jfxPanel.scene = Scene(view)
            }
            jfxPanel
        },
        update = { jfxPanel -> // jfxPanel là JFXPanel được tạo ở factory
            // Đảm bảo cập nhật trên luồng JavaFX
            Platform.runLater {
                val webView = (jfxPanel.scene?.root as? WebView)

                // Chỉ tải lại trang nếu WebView đã sẵn sàng VÀ URL thực sự thay đổi
                if (webView != null && webView.engine.location != url) {
                    webView.engine.load(url)
                }
            }
        }
    )
}
