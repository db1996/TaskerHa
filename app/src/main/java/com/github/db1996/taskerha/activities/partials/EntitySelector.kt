package com.github.db1996.taskerha.activities.partials

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.datamodels.HaEntity
import com.github.db1996.taskerha.util.EntityRecents

@Composable
fun EntitySelector(
    entities: List<HaEntity>,
    serviceDomain: String,
    currentEntityId: String,
    searching: Boolean,
    onSearchChanged: (Boolean) -> Unit,
    onEntityIdChanged: (String) -> Unit = {},
    onEntitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val recentEntityIds by EntityRecents.recents.collectAsState()

    val serviceDomainLower = remember(serviceDomain) { serviceDomain.lowercase() }

    val recentSet = remember(recentEntityIds) { recentEntityIds.toSet() }

    val domainMatches: (HaEntity) -> Boolean = remember(serviceDomainLower) {
        { e ->
            val id = e.entity_id
            id.startsWith("$serviceDomainLower.", ignoreCase = true) ||
                    id.substringBefore('.').lowercase().contains(serviceDomainLower)
        }
    }

    val filteredEntities = remember(entities, serviceDomainLower, searchQuery, searching) {
        if (!searching) emptyList()
        else {
            entities
                .asSequence()
                .filter(domainMatches)
                .filter { it.entity_id.contains(searchQuery, ignoreCase = true) }
                .toList()
        }
    }

    val recentEntities = remember(entities, recentEntityIds, searching, searchQuery, serviceDomainLower) {
        if (!searching || searchQuery.isNotBlank() || recentEntityIds.isEmpty()) emptyList()
        else {
            val map = entities.associateBy { it.entity_id }
            recentEntityIds.mapNotNull { map[it] }.filter(domainMatches)
        }
    }

    fun select(entityId: String) {
        onEntitySelected(entityId)
        onSearchChanged(false)
        searchQuery = ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Search row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (searching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    label = { Text("Search ID") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
                OutlinedTextField(
                    value = currentEntityId,
                    onValueChange = { onEntityIdChanged(it) },
                    label = { Text("Entity ID") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!searching) {
                Button(onClick = { onSearchChanged(true) }) { Text("Search") }
            } else {
                IconButton(
                    onClick = {
                        onSearchChanged(false)
                        searchQuery = ""
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancel Search")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Entity list
        if (searching) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (recentEntities.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    items(recentEntities, key = { it.entity_id + "_recent" }) { entity ->
                        EntityRowCard(
                            entityId = entity.entity_id,
                            isRecent = true,
                            onClick = { select(entity.entity_id) },
                            showRemoveRecent = true,
                            onRemoveRecent = { EntityRecents.remove(entity.entity_id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All results",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                items(filteredEntities, key = { it.entity_id + "_all" }) { entity ->
                    EntityRowCard(
                        entityId = entity.entity_id,
                        isRecent = recentSet.contains(entity.entity_id),
                        onClick = { select(entity.entity_id) },
                        showRemoveRecent = true,
                        onRemoveRecent = { EntityRecents.remove(entity.entity_id) }
                    )
                }

                if (filteredEntities.isEmpty() && recentEntities.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "No entities found",
                            subtitle = if (serviceDomain.isNotBlank())
                                "Try a different search or check your domain filter."
                            else
                                "Try a different search."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityRowCard(
    entityId: String,
    isRecent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRemoveRecent: Boolean = false,
    onRemoveRecent: (() -> Unit)? = null
) {
    val domain = remember(entityId) { entityId.substringBefore('.', missingDelimiterValue = entityId) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entityId,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Domain: $domain",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(10.dp))

            if (isRecent) {
                RecentBadge()
            }

            if (showRemoveRecent && onRemoveRecent != null && isRecent) {
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onRemoveRecent,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove from recents",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = "RECENT",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
