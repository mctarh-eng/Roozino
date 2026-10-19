package com.roozino.app.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.UiSettings
import com.roozino.app.data.Reminder
import com.roozino.app.ui.components.EmptyHint
import com.roozino.app.ui.components.MainScaffold
import com.roozino.app.ui.components.ReminderDialog
import com.roozino.app.util.Fa

private fun repeatLabel(repeatType: Int): String = when (repeatType) {
    1 -> "هر روز"
    2 -> "هر هفته"
    3 -> "هر ماه"
    else -> "یک‌بار"
}

@Composable
fun RemindersScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    val reminders by vm.reminders.collectAsState()
    val context = LocalContext.current

    var dialogFor by remember { mutableStateOf<Reminder?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }
    var deleteFor by remember { mutableStateOf<Reminder?>(null) }

    val persian = settings.persianDigits
    val now = remember { System.currentTimeMillis() }
    val list = reminders.filter { !it.deleted }
        .sortedWith(compareByDescending<Reminder> { it.enabled }
            .thenBy { it.timeMillis })

    MainScaffold(
        title = "یادآوری‌ها",
        selectedRoute = "reminders",
        nav = nav,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { showNewDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("یادآوری جدید") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // راهنمای آلارم دقیق (اندروید ۱۲+)
            if (Build.VERSION.SDK_INT >= 31) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!am.canScheduleExactAlarms()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.BatteryAlert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "برای دقت یادآوری‌ها، اجازه «آلارم دقیق» را بده",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "برخی گوشی‌ها با بهینه‌سازی باتری، اعلان را دیر می‌رسانند — هیچ سیستمی تحویل اعلان را ۱۰۰٪ تضمین نمی‌کند.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                            .setData(Uri.parse("package:${context.packageName}"))
                                    )
                                } catch (_: Exception) {
                                }
                            }) { Text("اجازه می‌دهم") }
                        }
                    }
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                if (list.isEmpty()) {
                    item {
                        EmptyHint("یادآوری‌ای نداری — با دکمه پایین اولین یادآوری را بساز.")
                    }
                }
                items(list, key = { it.id }) { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(r.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${Fa.dateTime(r.timeMillis, persian)} • ${repeatLabel(r.repeatType)}" +
                                        if (r.timeMillis < now && !r.enabled) " • گذشته" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = r.enabled,
                            onCheckedChange = { vm.toggleReminder(r, it) }
                        )
                        IconButton(onClick = { deleteFor = r }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف")
                        }
                        TextButton(onClick = { dialogFor = r }) { Text("ویرایش") }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showNewDialog) {
        ReminderDialog(
            settings = settings,
            initial = null,
            defaultTime = now + 3_600_000L,
            onDismiss = { showNewDialog = false },
            onSave = { vm.saveReminder(it) }
        )
    }
    dialogFor?.let { r ->
        ReminderDialog(
            settings = settings,
            initial = r,
            defaultTime = r.timeMillis,
            onDismiss = { dialogFor = null },
            onSave = { vm.saveReminder(it) }
        )
    }
    deleteFor?.let { r ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("حذف یادآوری؟") },
            text = { Text("«${r.title}» حذف می‌شود.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteReminder(r); deleteFor = null }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text("انصراف") }
            }
        )
    }
}
