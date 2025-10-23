package org.np.ui.webclient

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WebClientUiState(
    val url: String = "https://example.com",
    val resultText: String = "Kết quả sẽ hiển thị ở đây...",
    val htmlContent: String = "",
    val isLoading: Boolean = false,
    val isHtmlResponse: Boolean = false,
    val viewMode: ViewMode = ViewMode.TEXT,
    val loadingMessage: String = "" // ✅ Thêm loading message
)

enum class ViewMode {
    TEXT,
    HTML
}

class WebClientVM {
    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    private val _uiState = MutableStateFlow(WebClientUiState())
    val uiState: StateFlow<WebClientUiState> = _uiState.asStateFlow()

    fun updateUrl(newUrl: String) {
        _uiState.value = _uiState.value.copy(url = newUrl)
    }

    fun setViewMode(mode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun sendRequest() {
        val currentUrl = _uiState.value.url

        // ✅ Cập nhật UI NGAY LẬP TỨC trước khi gửi request
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            loadingMessage = "Đang kết nối đến $currentUrl...",
            resultText = "Đang gửi yêu cầu...",
            htmlContent = "",
            isHtmlResponse = false
        )

        // ✅ Chạy request trên background thread
        viewModelScope.launch {
            try {
                // Update loading message
                updateLoadingMessage("Đang gửi yêu cầu GET...")
                delay(100) // Small delay để UI render loading state

                val response: HttpResponse = httpClient.get(currentUrl)

                updateLoadingMessage("Đang nhận dữ liệu...")
                val body: String = response.bodyAsText()
                val contentType = response.contentType()

                updateLoadingMessage("Đang xử lý response...")

                // Kiểm tra xem response có phải HTML không
                val isHtml = contentType?.match(ContentType.Text.Html) == true

                val responseInfo = buildString {
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("📊 RESPONSE INFO")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("Status: ${response.status.value} ${response.status.description}")
                    appendLine("Content Type: ${contentType}")
                    appendLine("Content Length: ${response.contentLength() ?: "Unknown"} bytes")
                    appendLine("Is HTML: ${if (isHtml) "✅ Yes" else "❌ No"}")
                    appendLine()

                    appendLine("📋 RESPONSE HEADERS:")
                    response.headers.entries().forEach { (key, values) ->
                        appendLine("  $key: ${values.joinToString(", ")}")
                    }
                    appendLine()

                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    appendLine("📦 RESPONSE BODY:")
                    appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    if (isHtml) {
                        appendLine("(HTML content - Switch to HTML view to render)")
                        appendLine()
                        appendLine("Raw HTML:")
                        appendLine(body.take(1000))
                        if (body.length > 1000) {
                            appendLine("\n... (${body.length - 1000} more characters)")
                        }
                    } else {
                        // Format JSON nếu có thể
                        if (contentType?.match(ContentType.Application.Json) == true) {
                            try {
                                appendLine(formatJson(body))
                            } catch (e: Exception) {
                                appendLine(body)
                            }
                        } else {
                            appendLine(body)
                        }
                    }
                }

                // ✅ Cập nhật kết quả trên Main thread
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        resultText = responseInfo,
                        htmlContent = body,
                        isHtmlResponse = isHtml,
                        viewMode = if (isHtml) ViewMode.HTML else ViewMode.TEXT,
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
            } catch (e: Exception) {
                // ✅ Hiển thị lỗi trên Main thread
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        resultText = buildString {
                            appendLine("❌ LỖI YÊU CẦU")
                            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                            appendLine("URL: $currentUrl")
                            appendLine("Loại lỗi: ${e::class.simpleName}")
                            appendLine("Chi tiết: ${e.message}")
                            appendLine()
                            appendLine("Stack trace:")
                            appendLine(e.stackTraceToString().take(500))
                        },
                        htmlContent = "",
                        isHtmlResponse = false,
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
            }
        }
    }

    private suspend fun updateLoadingMessage(message: String) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(loadingMessage = message)
        }
    }

    private fun formatJson(json: String): String {
        var indent = 0
        val result = StringBuilder()
        var inString = false

        json.forEach { char ->
            when {
                char == '"' && (result.isEmpty() || result.last() != '\\') -> {
                    inString = !inString
                    result.append(char)
                }
                !inString && (char == '{' || char == '[') -> {
                    result.append(char)
                    result.append('\n')
                    indent++
                    result.append("  ".repeat(indent))
                }
                !inString && (char == '}' || char == ']') -> {
                    result.append('\n')
                    indent--
                    result.append("  ".repeat(indent))
                    result.append(char)
                }
                !inString && char == ',' -> {
                    result.append(char)
                    result.append('\n')
                    result.append("  ".repeat(indent))
                }
                !inString && char == ':' -> {
                    result.append(char)
                    result.append(' ')
                }
                !inString && char.isWhitespace() -> {
                    // Skip whitespace outside strings
                }
                else -> result.append(char)
            }
        }

        return result.toString()
    }

    fun closeClient() {
        httpClient.close()
    }
}