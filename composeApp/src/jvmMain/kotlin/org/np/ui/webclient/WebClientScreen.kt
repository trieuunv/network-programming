package org.np.ui.webclient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen

class WebClientScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = remember { WebClientVM() }
        WebClientScreenContent(viewModel)
    }
}

@Composable
fun WebClientScreenContent(viewModel: WebClientVM) {
    val state by viewModel.uiState.collectAsState()

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 🔹 Input URL
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { viewModel.updateUrl(it) },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🔹 Quick URLs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val urls = listOf(
                        "Example" to "https://example.com",
                        "JSON" to "https://jsonplaceholder.typicode.com/posts/1",
                        "Google" to "https://www.google.com"
                    )

                    urls.forEach { (label, url) ->
                        Button(
                            onClick = { viewModel.updateUrl(url) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        ) {
                            Text(label, style = MaterialTheme.typography.caption)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Send Button
                Button(
                    onClick = { viewModel.sendRequest() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colors.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Đang xử lý...")
                        }
                    } else {
                        Text("🚀 Gửi yêu cầu GET")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔹 Tab chuyển chế độ xem (nếu là HTML)
                if (state.isHtmlResponse && !state.isLoading) {
                    TabRow(selectedTabIndex = state.viewMode.ordinal) {
                        Tab(
                            selected = state.viewMode == ViewMode.TEXT,
                            onClick = { viewModel.setViewMode(ViewMode.TEXT) }
                        ) {
                            Text("📄 Text View", modifier = Modifier.padding(12.dp))
                        }
                        Tab(
                            selected = state.viewMode == ViewMode.HTML,
                            onClick = { viewModel.setViewMode(ViewMode.HTML) }
                        ) {
                            Text("🌐 HTML View", modifier = Modifier.padding(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 🔹 Kết quả
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    elevation = 4.dp
                ) {
                    when {
                        state.isLoading -> {
                            // Loading UI
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(state.loadingMessage)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(4.dp)
                                )
                            }
                        }

                        state.viewMode == ViewMode.TEXT -> {
                            // Text view
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = state.resultText.ifEmpty { "Không có dữ liệu." },
                                    style = MaterialTheme.typography.body2.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        state.viewMode == ViewMode.HTML -> {
                            // HTML view
                            if (state.htmlContent.isNotEmpty()) {
                                HtmlViewer(
                                    html = state.htmlContent,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Không có nội dung HTML để hiển thị.")
                                }
                            }
                        }
                    }
                }
            }

            // 🔹 Overlay (tùy chọn)
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(elevation = 8.dp) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(state.loadingMessage)
                        }
                    }
                }
            }
        }
    }
}
