package com.roozino.app.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.roozino.app.R
import com.roozino.app.data.BackupManager
import com.roozino.app.data.DataRepository
import com.roozino.app.notify.NotificationHelper
import com.roozino.app.ui.SettingsStore
import com.roozino.app.ui.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onThemeModeChange: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentMode by remember { mutableStateOf(SettingsStore.themeMode(context)) }

    var restoreCounts by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var restoreData by remember { mutableStateOf<Pair<List<com.roozino.app.data.TaskEntity>, List<com.roozino.app.data.NoteEntity>>?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = BackupManager.buildJson()
                val ok = BackupManager.write(context, uri, json)
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.backup_saved else R.string.backup_invalid),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = BackupManager.read(context, uri)
                val parsed = json?.let { BackupManager.parse(it) }
                if (parsed == null) {
                    Toast.makeText(context, context.getString(R.string.backup_invalid), Toast.LENGTH_SHORT).show()
                } else {
                    restoreData = parsed
                    restoreCounts = parsed.first.size to parsed.second.size
                }
            }
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    val exactAllowed = Build.VERSION.SDK_INT < 31 || alarmManager?.canScheduleExactAlarms() == true
    val notifOn = NotificationHelper.canNotify(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.appearance_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark)
                    ).forEach { (mode, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = currentMode == mode,
                                onClick = {
                                    currentMode = mode
                                    onThemeModeChange(mode)
                                }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.backup_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { exportLauncher.launch("roozino-backup.json") }) {
                            Text(stringResource(R.string.backup_export))
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                            Text(stringResource(R.string.backup_import))
                        }
                    }
                    Text(
                        stringResource(R.string.backup_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.reminders_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(if (exactAllowed) R.string.exact_alarm_ok else R.string.exact_alarm_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (exactAllowed) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.error
                    )
                    if (!exactAllowed) {
                        Button(onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        }) { Text(stringResource(R.string.grant_exact_alarm)) }
                    }
                    Text(
                        stringResource(if (notifOn) R.string.notifications_on else R.string.notifications_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.about_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.version_label) + " " + stringResource(R.string.version_value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.privacy_text),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        stringResource(R.string.limitations_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // تأیید بازیابی
    val counts = restoreCounts
    val data = restoreData
    if (counts != null && data != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                restoreCounts = null
                restoreData = null
            },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.restore_confirm_text,
                        counts.first,
                        counts.second
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        DataRepository.replaceAll(data.first, data.second)
                        Toast.makeText(context, context.getString(R.string.restored), Toast.LENGTH_SHORT).show()
                    }
                    restoreCounts = null
                    restoreData = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    restoreCounts = null
                    restoreData = null
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
