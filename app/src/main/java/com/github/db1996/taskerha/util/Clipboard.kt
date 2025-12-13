package com.github.db1996.taskerha.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast


fun copyToClipboard(
    context: Context,
    text: String,
    label: String = "Copied to clipboard"
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
}
