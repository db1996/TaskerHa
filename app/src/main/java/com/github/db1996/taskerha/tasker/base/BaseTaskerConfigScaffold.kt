package com.github.db1996.taskerha.tasker.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Base scaffold for Tasker configuration screens
 *
 * Provides a consistent UI with:
 * - Top app bar with title
 * - Optional test button
 * - Save button
 * - Content area
 *
 * Usage example:
 * ```
 * @Composable
 * fun MyConfigScreen(
 *     viewModel: MyViewModel,
 *     onSave: (MyBuiltForm) -> Unit
 * ) {
 *     BaseTaskerConfigScaffold(
 *         title = "Configure My Action",
 *         onTest = { viewModel.testAction() },
 *         onSave = {
 *             val built = viewModel.buildForm()
 *             onSave(built)
 *         }
 *     ) { padding ->
 *         Column(
 *             modifier = Modifier.padding(padding).fillMaxWidth(),
 *             verticalArrangement = Arrangement.spacedBy(12.dp)
 *         ) {
 *             // Your UI content here
 *         }
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseTaskerConfigScaffold(
    title: String,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onTest: (() -> Unit)? = null,
    showTestButton: Boolean = true,
    topBarActions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    // Custom actions
                    topBarActions()

                    // Test button (optional)
                    if (showTestButton && onTest != null) {
                        IconButton(onClick = onTest) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Test action"
                            )
                        }
                    }

                    // Save button
                    FilledIconButton(onClick = onSave) {
                        Icon(
                            Icons.Rounded.Save,
                            contentDescription = "Save action"
                        )
                    }
                }
            )
        }
    ) { padding ->
        content(padding)
    }
}

/**
 * Standard content column for Tasker config screens
 */
@Composable
fun TaskerConfigColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

