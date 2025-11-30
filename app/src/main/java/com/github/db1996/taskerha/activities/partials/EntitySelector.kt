package com.github.db1996.taskerha.activities.partials
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.db1996.taskerha.datamodels.HaEntity

@Composable
fun EntitySelector(
    entities: List<HaEntity>,
    serviceDomain: String,
    currentEntityId: String,
    searching: Boolean,
    onSearchChanged: (Boolean) -> Unit,
    onEntitySelected: (String) -> Unit,
    modifier: Modifier = Modifier // allow parent to control size if needed
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredEntities = remember(entities, serviceDomain, searchQuery, searching) {
        if (!searching) emptyList()
        else {
            entities
                .asSequence()
                .filter { it.entity_id.startsWith("$serviceDomain.") }
                .filter { it.entity_id.contains(searchQuery, ignoreCase = true) }
                .toList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Search row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = if (searching) searchQuery else currentEntityId,
                onValueChange = { searchQuery = it },
                label = { Text("Entity ID") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            if (!searching) {
                Button(onClick = { onSearchChanged(true) }) { Text("Search") }
            } else {
                IconButton(onClick = {
                    onSearchChanged(false)
                    searchQuery = ""
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancel Search")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Filtered entity list
        if (searching) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredEntities) { entity ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp))
                            .clickable {
                                onEntitySelected(entity.entity_id)
                                onSearchChanged(false)
                                searchQuery = ""
                            }
                            .padding(12.dp)
                    ) {
                        Text(text = entity.entity_id, fontSize = 14.sp)
                    }
                }

                if (filteredEntities.isEmpty()) {
                    item {
                        Text(
                            text = "No entities found",
                            modifier = Modifier.padding(12.dp),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
