package com.github.db1996.taskerha.tasker.onHaMessage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.db1996.taskerha.tasker.onHaMessage.view.OnHaMessageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnHaMessageScreen(
    viewModel: OnHaMessageViewModel,
    onSave: (type: String, message: String) -> Unit
) {

    val context = LocalContext.current

    val form = viewModel.form

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Action") },
                actions = {
                    FilledIconButton(
                        onClick = {
                            Log.d("OnHaMessageScreen", "Saving action with data: ${form.type} ${form.message}")
                            val built = viewModel.buildForm()
                            onSave(built.type, built.message)
                        }
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save profile")
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
            Text("Type")
            TextField(
                value = form.type,
                onValueChange = { viewModel.setType(it) },
                label = { Text("Optional, can be used for filtering")}
            )
            Text("Message")
            TextField(
                value = form.message,
                onValueChange = { viewModel.setMessage(it) },
                label = { Text("Optional, can be used for filtering")}
            )

            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Home Assistant YAML example",
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(
                            onClick = {
                                copyToClipboard(
                                    context,
                                    viewModel.yamlExample,
                                    "Home Assistant event YAML"
                                )
                            }
                        ) {
                            Icon(

                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy YAML",
                                modifier = Modifier.padding(all = 0.dp).size(16.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Create an automation -> add action -> manual event -> edit yaml and paste this in",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    SelectionContainer {
                        Text(
                            text = viewModel.yamlExample,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

}
fun copyToClipboard(
    context: Context,
    text: String,
    label: String = "Copied text"
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "YAML copied to clipboard", Toast.LENGTH_SHORT).show()
}
