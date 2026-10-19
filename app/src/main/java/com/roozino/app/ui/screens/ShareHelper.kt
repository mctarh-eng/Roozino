package com.roozino.app.ui.screens

import android.content.Context
import android.content.Intent
import com.roozino.app.R

/** اشتراک‌گذاری متن یادداشت/کار با اپ‌های دیگر (بدون دسترسی حساس) */
fun shareNoteText(context: Context, title: String, content: String) {
    val text = buildString {
        if (title.isNotBlank()) appendLine(title)
        append(content)
        append("\n\n— روزینو")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_via))
        )
    } catch (_: Exception) {
        // اپی برای اشتراک‌گذاری نصب نیست
    }
}
