package com.github.db1996.taskerha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.db1996.taskerha.datamodels.ActualService


@Composable
fun ServiceSelectorBlocks(
    services: List<ActualService>,
    onSelect: (ActualService) -> Unit,
    currentServiceSearch: String,
    currentDomainSearch: String,
    onServiceSearch: (String) -> Unit,
    onDomainSearch: (String) -> Unit,
) {

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- Search fields side by side ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = currentDomainSearch,
                onValueChange = onDomainSearch ,
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

        // --- Scrollable service list ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // <-- THIS is what enables scrolling in remaining space
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredServices) { service ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            color = Color(0xFFF5F5F5),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelect(service) }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = service.name ?: service.id,
                            fontSize = 16.sp,
                            color = Color.Black
                        )

                        Text(
                            text = "${service.domain}.${service.id}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
