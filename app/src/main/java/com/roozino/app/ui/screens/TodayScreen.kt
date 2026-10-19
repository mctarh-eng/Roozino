package com.roozino.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.QuickType
import com.roozino.app.UiSettings
import com.roozino.app.ui.components.EmptyHint
import com.roozino.app.ui.components.MainScaffold
import com.roozino.app.ui.components.QuickAddDialog
import com.roozino.app.ui.components.SectionTitle
import com.roozino.app.ui.components.TaskRow
import com.roozino.app.util.Fa
import com.roozino.app.util.TimeMath
import com.roozino.app.jalali.JalaliCalendar
import java.util.Calendar

@Composable
fun TodayScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    val tasks by vm.tasks.collectAsState()
    val notes by vm.notes.collectAsState()
    val reminders by vm.reminders.collectAsState()
    var showQuick by remember { mutableStateOf(false) }
    var quickType by remember { mutableStateOf(QuickType.TASK) }

    val now = remember { System.currentTimeMillis() }
    val todayStart = remember { TimeMath.startOfDay(now) }
    val persian = settings.persianDigits

    MainScaffold(
        title = "امروز من",
        selectedRoute = "today",
        nav = nav,
        topActions = {
            IconButton(onClick = { nav.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "جست‌وجو")
            }
            IconButton(onClick = { nav.navigate("settings") }) {
                Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
            }
        },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { quickType = QuickType.TASK; showQuick = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("ثبت سریع") }
            )
        }
    ) { padding ->
        val activeTasks = tasks.filter { !it.deleted && !it.archived }
        val todayTasks = activeTasks.filter {
            it.dueMillis != null && it.dueMillis!! >= todayStart && it.dueMillis!! < todayStart + TimeMath.DAY_MILLIS
        }.sortedBy { it.dueMillis }
        val overdue = activeTasks.filter {
            it.dueMillis != null && it.dueMillis!! < todayStart && !it.isDone
        }.sortedBy { it.dueMillis }
        val doneToday = activeTasks.count { it.isDone && it.doneAt != null && it.doneAt!! >= todayStart }
        val pendingToday = todayTasks.count { !it.isDone }
        val upcomingReminders = reminders.filter { it.enabled && !it.deleted && it.timeMillis >= now }
            .sortedBy { it.timeMillis }
            .take(5)
        val recentNotes = notes.filter { !it.deleted && !it.archived }
            .sortedByDescending { it.updatedAt }
            .take(3)

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // کارت تاریخ و سلام
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            "${JalaliCalendar.weekdayName(now)}، ${Fa.date(now, persian)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val cal = Calendar.getInstance().apply { timeInMillis = now }
                        Text(
                            Fa.greeting(cal.get(Calendar.HOUR_OF_DAY)) + " — روز خوبی داشته باشی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // آمار امروز
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                StatCard("انجام‌شده امروز", Fa.num(doneToday, persian), Modifier.weight(1f))
                StatCard("باقی‌مانده امروز", Fa.num(pendingToday, persian), Modifier.weight(1f))
                StatCard("عقب‌افتاده", Fa.num(overdue.size, persian), Modifier.weight(1f))
            }

            // میان‌برها
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                QuickButton("کار", Modifier.weight(1f)) { quickType = QuickType.TASK; showQuick = true }
                QuickButton("یادداشت", Modifier.weight(1f)) { quickType = QuickType.NOTE; showQuick = true }
                QuickButton("یادآوری", Modifier.weight(1f)) { quickType = QuickType.REMINDER; showQuick = true }
            }

            if (overdue.isNotEmpty()) {
                SectionTitle("عقب‌افتاده")
                overdue.forEach { t ->
                    TaskRow(
                        task = t,
                        persian = persian,
                        onToggle = { vm.toggleTaskDone(t) },
                        onClick = { nav.navigate("taskEdit/${t.id}") }
                    )
                }
            }

            SectionTitle("کارهای امروز")
            if (todayTasks.isEmpty()) {
                EmptyHint("برای امروز کاری ثبت نشده — با دکمه بالا اضافه کن.")
            } else {
                todayTasks.forEach { t ->
                    TaskRow(
                        task = t,
                        persian = persian,
                        onToggle = { if (t.isDone) vm.reopenTask(t) else vm.toggleTaskDone(t) },
                        onClick = { nav.navigate("taskEdit/${t.id}") }
                    )
                }
            }

            SectionTitle("یادآوری‌های پیش‌رو")
            if (upcomingReminders.isEmpty()) {
                EmptyHint("یادآوری فعالی نداری.")
            } else {
                upcomingReminders.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate("reminders") }
                            .padding(vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(r.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            if (Fa.relativeDay(r.timeMillis, persian) == "امروز")
                                Fa.time(r.timeMillis, persian)
                            else Fa.dateTime(r.timeMillis, persian),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionTitle("یادداشت‌های اخیر")
            if (recentNotes.isEmpty()) {
                EmptyHint("هنوز یادداشتی ننوشته‌ای.")
            } else {
                recentNotes.forEach { n ->
                    Card(
                        onClick = { nav.navigate("noteEdit/${n.id}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                n.title.ifBlank { "بی‌عنوان" },
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (n.body.isNotBlank()) {
                                Text(
                                    n.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQuick) {
        QuickAddDialog(vm = vm, settings = settings, initialType = quickType, onDismiss = { showQuick = false })
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.NotificationImportant,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
