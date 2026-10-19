package com.roozino.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.ui.components.SubScaffold
import kotlinx.coroutines.delay

@Composable
fun NoteEditScreen(
    vm: AppViewModel,
    nav: NavController,
    noteId: Long
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var savedId by remember { mutableStateOf(if (noteId > 0) noteId else 0L) }
    var pinned by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(noteId <= 0L) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val n = vm.getNote(noteId)
            if (n != null) {
                pinned = n.pinned
                title = n.title
                body = n.body
            }
        }
        loaded = true
    }

    // ذخیره خودکار با تأخیر ۷۰۰ میلی‌ثانیه
    LaunchedEffect(loaded, title, body) {
        if (!loaded) return@LaunchedEffect
        delay(700)
        savedId = vm.saveNoteLive(savedId, title, body, pinned)
    }

    SubScaffold(
        title = "یادداشت",
        nav = nav,
        actions = {
            IconButton(onClick = {
                pinned = !pinned
                if (savedId > 0) {
                    vm.getNote(savedId)?.let { vm.toggleNotePin(it) }
                }
            }) {
                Icon(
                    if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (pinned) "برداشتن سنجاق" else "سنجاق‌کردن"
                )
            }
            IconButton(onClick = {
                clipboard.setText(AnnotatedString("${title}\n\n${body}"))
                android.widget.Toast.makeText(context, "کپی شد", android.widget.Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "کپی")
            }
            IconButton(onClick = {
                try {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${title}\n\n${body}")
                    }
                    context.startActivity(Intent.createChooser(send, "اشتراک‌گذاری یادداشت"))
                } catch (_: Exception) {
                }
            }) {
                Icon(Icons.Filled.Share, contentDescription = "اشتراک‌گذاری")
            }
            if (savedId > 0) {
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "حذف یادداشت")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("عنوان") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("هر چی توی ذهنت است بنویس…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            Text(
                "ذخیره خودکار فعال است",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("حذف یادداشت؟") },
            text = { Text("این یادداشت برای همیشه حذف می‌شود و قابل بازیابی نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.getNote(savedId)?.let { vm.deleteNote(it) }
                    showDelete = false
                    nav.popBackStack()
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("انصراف") }
            }
        )
    }
}
