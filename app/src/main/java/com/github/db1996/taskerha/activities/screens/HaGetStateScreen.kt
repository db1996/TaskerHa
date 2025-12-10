package com.github.db1996.taskerha.activities.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.activities.partials.EntitySelector
import com.github.db1996.taskerha.activities.viewmodels.HaGetStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaGetStateScreen(
    viewModel: HaGetStateViewModel,
    onSave: (entityId: String) -> Unit
) {
    var entitySearching by remember { mutableStateOf(false) }

    // Load on first composition
    LaunchedEffect(Unit) {
        viewModel.loadEntities()
    }
    val form = viewModel.form


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Action") },
                actions = {
                    IconButton(onClick = {
                        viewModel.testForm()
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Test action")
                    }
                    FilledIconButton(
                        onClick = {
                            Log.d("HaGetStateScreen", "Saving action with data: ${form.entityId}")
                            val built = viewModel.buildForm()
                            onSave(built.entityId)
                        }
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save action")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (viewModel.clientError != "") {
                Text(viewModel.clientError, color = MaterialTheme.colorScheme.error)

                Text("Please check your connection settings in the main app outside of tasker")
            }

            if(entitySearching)
                TextField(
                    value = viewModel.currentDomainSearch,
                    onValueChange = { viewModel.currentDomainSearch = it },
                    label = { Text("filter domain") },
                )

            EntitySelector(
                entities = viewModel.entities,
                serviceDomain = viewModel.currentDomainSearch,
                currentEntityId = form.entityId,
                searching = entitySearching,
                onSearchChanged = { entitySearching = it },
                onEntitySelected = { viewModel.pickEntity(it) }
            )
        }
    }
}
