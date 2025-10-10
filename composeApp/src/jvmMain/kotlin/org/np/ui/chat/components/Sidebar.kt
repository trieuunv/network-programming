package org.np.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.np.dto.ParticipantDto

@Composable
fun Sidebar(participants: List<ParticipantDto>) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Text(
            text = "Messages",
            color = Color.Black,
            fontSize = 20.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search...") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            val filtered = participants.filter {
                it.username.contains(searchQuery.text, ignoreCase = true)
            }

            items(filtered.size) { index ->
                ParticipantItem(participant = filtered[index])
            }
        }
    }
}