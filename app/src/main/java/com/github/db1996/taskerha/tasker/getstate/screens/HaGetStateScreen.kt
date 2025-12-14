package com.github.db1996.taskerha.tasker.getstate.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.tasker.base.BaseTaskerConfigScaffold
import com.github.db1996.taskerha.tasker.getstate.data.HaGetStateBuiltForm
import com.github.db1996.taskerha.tasker.getstate.view.HaGetStateViewModel

@Composable
fun HaGetStateScreen(
    viewModel: HaGetStateViewModel,
    onSave: (HaGetStateBuiltForm) -> Unit
) {
    var entitySearching by remember { mutableStateOf(false) }

    // Load entities on first composition
    LaunchedEffect(Unit) {
        viewModel.loadEntities()
    }

    val form = viewModel.form

    BaseTaskerConfigScaffold(
        title = "Get Home Assistant State",
        onSave = {
            val built = viewModel.buildForm()
            onSave(built)
        },
        onTest = { viewModel.testForm() },
        showTestButton = true
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error message
            if (viewModel.clientError.isNotEmpty()) {
                Text(
                    text = viewModel.clientError,
                    color = MaterialTheme.colorScheme.error
                )
                Text("Please check your connection settings in the main app outside of tasker")
            }

            // Domain search filter
            if (entitySearching) {
                TextField(
                    value = viewModel.currentDomainSearch,
                    onValueChange = { viewModel.currentDomainSearch = it },
                    label = { Text("Filter domain") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Entity selector
            EntitySelector(
                entities = viewModel.entities,
                serviceDomain = viewModel.currentDomainSearch,
                currentEntityId = form.entityId,
                searching = entitySearching,
                onSearchChanged = { entitySearching = it },
                onEntitySelected = { viewModel.pickEntity(it) },
                onEntityIdChanged = { viewModel.updateEntityId( it)}
            )
        }
    }
}

