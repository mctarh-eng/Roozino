package com.roozino.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.UiSettings
import com.roozino.app.jalali.JalaliCalendar
import com.roozino.app.ui.components.EmptyHint
import com.roozino.app.ui.components.EventDialog
import com.roozino.app.ui.components.MainScaffold
import com.roozino.app.ui.components.MonthGrid
import com.roozino.app.ui.components.SectionTitle
import com.roozino.app.ui.components.TaskRow
import com.roozino.app.util.Fa
import com.roozino.app.util.TimeMath

@Composable
fun CalendarScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    val tasks by vm.tasks.collectAsState()
    val events by vm.events.collectAsState()

    val today = remember { JalaliCalendar.today() }
    var jy by remember { mutableIntStateOf(today.first) }
    var jm by remember { mutableIntStateOf(today.second) }
    var selDay by remember { mutableIntStateOf(today.third) }
    var showEventDialog by remember { mutableStateOf(false) }

    val persian = settings.persianDigits
    val selMillis = JalaliCalendar.jalaliToMillis(jy, jm, selDay)
    val monthLen = JalaliCalendar.monthLength(jy, jm)

    fun dayDots(day: Int): Int {
        val d0 = TimeMath.startOfDay(JalaliCalendar.jalaliToMillis(jy, jm, day))
        val t = tasks.count {
            !it.deleted && !it.archived && it.dueMillis != null &&
                    TimeMath.startOfDay(it.dueMillis!!) == d0
        }
        val e = events.count { !it.deleted && TimeMath.startOfDay(it.startMillis) == d0 }
        return t + e
    }
    val dots = remember(jy, jm, tasks, events) {
        (1..monthLen).associateWith { dayDots(it) }
    }

    val dayTasks = tasks.filter {
        !it.deleted && !it.archived && it.dueMillis != null &&
                TimeMath.startOfDay(it.dueMillis!!) == TimeMath.startOfDay(selMillis)
    }.sortedBy { it.dueMillis }
    val dayEvents = events.filter {
        !it.deleted && TimeMath.startOfDay(it.startMillis) == TimeMath.startOfDay(selMillis)
    }.sortedBy { it.startMillis }

    MainScaffold(
        title = "تقویم شمسی",
        selectedRoute = "calendar",
        nav = nav,
        fab = {
            ExtendedFloatingActionButton(
                onClick = { showEventDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("رویداد") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Card {
                Column(Modifier.padding(8.dp)) {
                    MonthGrid(
                        jy = jy, jm = jm, selectedDay = selDay,
                        today = today, dots = dots, persian = persian,
                        onSelect = { selDay = it },
                        onChangeMonth = { a, b -> jy = a; jm = b }
                    )
                    Text(
                        "روزی که نقطه دارد، کار یا رویداد دارد",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp, start = 8.dp)
                    )
                }
            }

            SectionTitle("برنامه ${JalaliCalendar.weekdayName(selMillis)} ${Fa.date(selMillis, persian)}")
            if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                EmptyHint("این روز خالی است.")
            }
            dayEvents.forEach { e ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                Fa.time(e.startMillis, persian) +
                                        (e.endMillis?.let { " تا ${Fa.time(it, persian)}" } ?: ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { vm.deleteEvent(e) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف رویداد")
                        }
                    }
                }
            }
            dayTasks.forEach { t ->
                TaskRow(
                    task = t,
                    persian = persian,
                    onToggle = { if (t.isDone) vm.reopenTask(t) else vm.toggleTaskDone(t) },
                    onClick = { nav.navigate("taskEdit/${t.id}") }
                )
            }
            Spacer(Modifier.height(90.dp))
        }
    }

    if (showEventDialog) {
        EventDialog(
            settings = settings,
            dayMillis = TimeMath.startOfDay(selMillis),
            onDismiss = { showEventDialog = false },
            onSave = { title, start, end ->
                vm.saveEvent(
                    com.roozino.app.data.Event(
                        title = title,
                        startMillis = start,
                        endMillis = end,
                        hasTime = true
                    )
                )
            }
        )
    }
}
