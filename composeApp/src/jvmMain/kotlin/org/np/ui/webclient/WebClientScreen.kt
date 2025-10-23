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
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // URL Input
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { viewModel.updateUrl(it) },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick URLs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.updateUrl("https://example.com") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Text("Example", style = MaterialTheme.typography.caption)
                    }
                    Button(
                        onClick = { viewModel.updateUrl("https://jsonplaceholder.typicode.com/posts/1") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Text("JSON", style = MaterialTheme.typography.caption)
                    }
                    Button(
                        onClick = { viewModel.updateUrl("https://www.google.com") },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isLoading
                    ) {
                        Text("Google", style = MaterialTheme.typography.caption)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Send Button
                Button(
                    onClick = { viewModel.sendRequest() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colors.onPrimary,
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Đang xử lý...")
                    } else {
                        Text("🚀 Gửi Yêu Cầu GET")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // View Mode Tabs (only show if HTML response)
                if (state.isHtmlResponse && !state.isLoading) {
                    TabRow(
                        selectedTabIndex = state.viewMode.ordinal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = state.viewMode == ViewMode.TEXT,
                            onClick = { viewModel.setViewMode(ViewMode.TEXT) }
                        ) {
                            Text("📄 Text View", modifier = Modifier.padding(16.dp))
                        }
                        Tab(
                            selected = state.viewMode == ViewMode.HTML,
                            onClick = { viewModel.setViewMode(ViewMode.HTML) }
                        ) {
                            Text("🌐 HTML View", modifier = Modifier.padding(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Result Display
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    elevation = 4.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when {
                            state.isLoading -> {
                                // ✅ Loading State
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(64.dp),
                                        strokeWidth = 6.dp
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Text(
                                        text = state.loadingMessage,
                                        style = MaterialTheme.typography.h6
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(4.dp)
                                    )
                                }
                            }
                            state.viewMode == ViewMode.TEXT -> {
                                // Text View
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = state.resultText,
                                        style = MaterialTheme.typography.body2.copy(
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                            }
                            state.viewMode == ViewMode.HTML -> {
                                // HTML View
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
                                        Text("No HTML content to display")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ✅ Loading Overlay (Optional - nếu muốn màn hình mờ khi loading)
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp),
                        elevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 5.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = state.loadingMessage,
                                style = MaterialTheme.typography.subtitle1
                            )
                        }
                    }
                }
            }
        }
    }
}