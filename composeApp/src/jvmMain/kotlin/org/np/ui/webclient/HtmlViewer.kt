package org.np.ui.webclient

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

@Composable
fun HtmlViewer(
    html: String,
    modifier: Modifier = Modifier
) {
    SwingPanel(
        modifier = modifier,
        factory = {
            JPanel(BorderLayout()).apply {
                val editorPane = JEditorPane().apply {
                    contentType = "text/html"
                    isEditable = false

                    // Sử dụng HTMLEditorKit để render HTML tốt hơn
                    val kit = HTMLEditorKit()
                    val styleSheet = StyleSheet()

                    // Thêm CSS cơ bản để hiển thị đẹp hơn
                    styleSheet.addRule("body { font-family: Arial, sans-serif; margin: 10px; }")
                    styleSheet.addRule("h1 { color: #333; }")
                    styleSheet.addRule("h2 { color: #666; }")
                    styleSheet.addRule("a { color: #0066cc; }")
                    styleSheet.addRule("pre { background-color: #f4f4f4; padding: 10px; }")

                    kit.styleSheet = styleSheet
                    editorKit = kit

                    text = html

                    // Scroll to top
                    caretPosition = 0
                }

                val scrollPane = JScrollPane(editorPane).apply {
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                    preferredSize = Dimension(800, 600)
                }

                add(scrollPane, BorderLayout.CENTER)
            }
        },
        update = { panel ->
            SwingUtilities.invokeLater {
                val scrollPane = panel.components[0] as JScrollPane
                val editorPane = scrollPane.viewport.view as JEditorPane

                // Chỉ update nếu content thực sự thay đổi
                if (editorPane.text != html) {
                    val scrollPosition = scrollPane.verticalScrollBar.value
                    editorPane.text = html

                    // Giữ nguyên scroll position hoặc về đầu
                    SwingUtilities.invokeLater {
                        scrollPane.verticalScrollBar.value = 0
                    }
                }
            }
        }
    )
}