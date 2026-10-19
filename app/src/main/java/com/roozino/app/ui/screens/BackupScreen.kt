package com.roozino.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.ui.components.SubScaffold
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(vm: AppViewModel, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                busy = true
                try {
                    val json = vm.exportJson()
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "پشتیبان ساخته شد", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در ساخت پشتیبان", Toast.LENGTH_SHORT).show()
                } finally {
                    busy = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                pendingImport = text
            } catch (e: Exception) {
                Toast.makeText(context, "خواندن فایل ممکن نشد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    SubScaffold(title = "پشتیبان‌گیری و بازیابی", nav = nav) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("پشتیبان بگیر", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "همه کارها، یادداشت‌ها، یادآوری‌ها و رویدادها در یک فایل JSON ذخیره می‌شود. فایل را در جایی امن (مثلاً گوگل‌درایو) نگه دار.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Button(
                        onClick = { exportLauncher.launch("roozino-backup.json") },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("ساخت فایل پشتیبان") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("بازیابی", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "فایل پشتیبان را انتخاب کن؛ می‌توانی به داده‌های فعلی اضافه کنی یا همه را جایگزین کنی.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("انتخاب فایل پشتیبان") }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("بازیابی پشتیبان") },
            text = { Text("چطور بازیابی شود؟\n\n• افزودن: داده‌های پشتیبان به داده‌های فعلی اضافه می‌شوند.\n• جایگزینی کامل: همه داده‌های فعلی پاک و پشتیبان جای آن می‌نشیند!") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val n = vm.importJson(text, replace = false)
                            Toast.makeText(context, "$n آیتم اضافه شد", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "خطا در بازیابی", Toast.LENGTH_LONG).show()
                        } finally {
                            busy = false
                        }
                    }
                    pendingImport = null
                }) { Text("افزودن") }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        try {
                            val n = vm.importJson(text, replace = true)
                            Toast.makeText(context, "$n آیتم بازیابی شد", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "خطا در بازیابی", Toast.LENGTH_LONG).show()
                        } finally {
                            busy = false
                        }
                    }
                    pendingImport = null
                }) { Text("جایگزینی کامل") }
            }
        )
    }
}
