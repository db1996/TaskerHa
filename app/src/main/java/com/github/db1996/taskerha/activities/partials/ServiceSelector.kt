package com.github.db1996.taskerha.activities.partials

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.datamodels.ActualService
import com.github.db1996.taskerha.util.ServiceRecents

@Composable
fun ServiceSelector(
    services: List<ActualService>,
    onSelect: (ActualService) -> Unit,
    currentServiceSearch: String,
    currentDomainSearch: String,
    onServiceSearch: (String) -> Unit,
    onDomainSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val recentKeys by ServiceRecents.recents.collectAsState()
    val recentSet = remember(recentKeys) { recentKeys.toSet() }

    // Filter services by both domain and service queries
    val filteredServices = remember(currentServiceSearch, currentDomainSearch, services) {
        services.filter { service ->
            service.domain.contains(currentDomainSearch, ignoreCase = true) &&
                    (
                            (service.name?.contains(currentServiceSearch, ignoreCase = true) ?: false) ||
                                    service.id.contains(currentServiceSearch, ignoreCase = true)
                            )
        }
    }

    // Recents shown only when both searches are empty (same behavior as your entity picker)
    val recentServices = remember(services, recentKeys, currentDomainSearch, currentServiceSearch) {
        if (recentKeys.isEmpty() || currentDomainSearch.isNotBlank() || currentServiceSearch.isNotBlank()) {
            emptyList()
        } else {
            val map = services.associateBy { "${it.domain}.${it.id}" }
            recentKeys.mapNotNull { map[it] }
        }
    }

    fun keyOf(s: ActualService) = "${s.domain}.${s.id}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        // --- Search fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentDomainSearch,
                onValueChange = onDomainSearch,
                label = { Text("Domain") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = currentServiceSearch,
                onValueChange = onServiceSearch,
                label = { Text("Service") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (recentServices.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                items(recentServices, key = { "recent:${keyOf(it)}" }) { service ->
                    ServiceRowCard(
                        service = service,
                        isRecent = true,
                        onClick = {
                            onSelect(service)
                        },
                        showRemoveRecent = true,
                        onRemoveRecent = { ServiceRecents.remove(keyOf(service)) }
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

            items(filteredServices, key = { "all:${keyOf(it)}" }) { service ->
                ServiceRowCard(
                    service = service,
                    isRecent = recentSet.contains(keyOf(service)),
                    onClick = {
                        ServiceRecents.add(keyOf(service))
                        onSelect(service)
                    }
                )
            }

            if (filteredServices.isEmpty() && recentServices.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No services found",
                        subtitle = "Try a different domain/service search."
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceRowCard(
    service: ActualService,
    isRecent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRemoveRecent: Boolean = false,
    onRemoveRecent: (() -> Unit)? = null
) {
    val title = service.name ?: service.id
    val full = "${service.domain}.${service.id}"

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
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = full,
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

            if (showRemoveRecent && onRemoveRecent != null) {
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
