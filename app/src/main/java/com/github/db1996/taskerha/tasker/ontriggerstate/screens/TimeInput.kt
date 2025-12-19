package com.github.db1996.taskerha.tasker.ontriggerstate.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun DurationHmsStringField(
    label: String = "For",
    value: String, // "h:m:s" or "" when not set
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    fun splitHms(v: String): Triple<String, String, String> {
        val parts = v.split(":")
        val h = parts.getOrNull(0).orEmpty()
        val m = parts.getOrNull(1).orEmpty()
        val s = parts.getOrNull(2).orEmpty()
        return Triple(h, m, s)
    }

    fun digitsOnly(s: String) = s.filter(Char::isDigit)

    var h by remember(value) { mutableStateOf(splitHms(value).first) }
    var m by remember(value) { mutableStateOf(splitHms(value).second) }
    var s by remember(value) { mutableStateOf(splitHms(value).third) }

    fun emit() {
        val hh = h.trim()
        val mm = m.trim()
        val ss = s.trim()

        // If user cleared everything, treat as "not set"
        if (hh.isEmpty() && mm.isEmpty() && ss.isEmpty()) {
            onValueChange("")
            return
        }

        // Otherwise always produce a full h:m:s string
        onValueChange("${hh.ifEmpty { "" }}:${mm.ifEmpty { "" }}:${ss.ifEmpty { "" }}")
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = h,
                onValueChange = { h = digitsOnly(it); emit() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                placeholder = { Text("hh", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            Text(":", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = m,
                onValueChange = { m = digitsOnly(it); emit() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                placeholder = { Text("mm", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            Text(":", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = s,
                onValueChange = { s = digitsOnly(it); emit() },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                placeholder = { Text("ss", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )

            IconButton(
                onClick = {
                    h = ""; m = ""; s = ""
                    onValueChange("")
                    focusManager.clearFocus()
                }
            ) {
                Icon(Icons.Default.Close, contentDescription = "Clear duration")
            }
        }
    }
}
