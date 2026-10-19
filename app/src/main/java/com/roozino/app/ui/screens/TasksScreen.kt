package com.roozino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.UiSettings
import com.roozino.app.data.Task
import com.roozino.app.ui.components.EmptyHint
import com.roozino.app.ui.components.MainScaffold
import com.roozino.app.ui.components.TaskRow
import com.roozino.app.util.Fa
import com.roozino.app.util.TimeMath

@Composable
fun TasksScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    val tasks by vm.tasks.collectAsState()
    var filter by rememberSaveable { mutableStateOf("all") }
    var menuFor by remember { mutableStateOf<Task?>(null) }

    val persian = settings.persianDigits
    val todayStart = remember { TimeMath.startOfDay(System.currentTimeMillis()) }

    val all = tasks
    val active = all.filter { !it.deleted }
    val list = when (filter) {
        "today" -> active.filter {
            !it.archived && it.dueMillis != null &&
                    it.dueMillis!! >= todayStart && it.dueMillis!! < todayStart + TimeMath.DAY_MILLIS
        }
        "done" -> active.filter { !it.archived && it.isDone }
        "archived" -> active.filter { it.archived }
        "trash" -> all.filter { it.deleted }
        else -> active.filter { !it.archived }
    }.sortedWith(
        compareBy<Task> { it.isDone }
            .thenBy { it.dueMillis ?: Long.MAX_VALUE }
            .thenByDescending { it.priority }
            .thenByDescending { it.updatedAt }
    )

    MainScaffold(
        title = "کارها",
        selectedRoute = "tasks",
        nav = nav,
        topActions = {
            IconButton(onClick = { nav.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "جست‌وجو")
            }
        },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("taskEdit/-1") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("کار جدید") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                listOf(
                    "all" to "همه",
                    "today" to "امروز",
                    "done" to "انجام‌شده",
                    "archived" to "بایگانی",
                    "trash" to "سطل بازیافت"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (list.isEmpty()) {
                    EmptyHint(
                        when (filter) {
                            "today" -> "برای امروز کاری نداری."
                            "done" -> "هنوز کاری را کامل نکرده‌ای."
                            "archived" -> "بایگانی خالی است."
                            "trash" -> "سطل بازیافت خالی است."
                            else -> "هنوز کاری ثبت نکرده‌ای — با دکمه «کار جدید» شروع کن."
                        }
                    )
                }
                list.forEach { t ->
                    val overdue = !t.isDone && t.dueMillis != null && t.dueMillis!! < todayStart
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TaskRow(
                            task = t,
                            persian = persian,
                            onToggle = { if (t.isDone) vm.reopenTask(t) else vm.toggleTaskDone(t) },
                            onClick = { nav.navigate("taskEdit/${t.id}") },
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(onClick = { menuFor = t }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "گزینه‌ها")
                            }
                            if (menuFor?.id == t.id) {
                                TaskMenu(
                                    task = t,
                                    onDismiss = { menuFor = null },
                                    vm = vm
                                )
                            }
                        }
                    }
                    if (overdue) {
                        Text(
                            "★ عقب‌افتاده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(start = 56.dp)
                        )
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TaskMenu(task: Task, vm: AppViewModel, onDismiss: () -> Unit) {
    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        if (task.deleted) {
            DropdownMenuItem(
                text = { Text("بازیابی") },
                leadingIcon = { Icon(Icons.Filled.Unarchive, null) },
                onClick = { vm.restoreTask(task); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("حذف همیشگی") },
                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                onClick = { vm.purgeTask(task); onDismiss() }
            )
        } else if (task.archived) {
            DropdownMenuItem(
                text = { Text("خروج از بایگانی") },
                leadingIcon = { Icon(Icons.Filled.Unarchive, null) },
                onClick = { vm.unarchiveTask(task); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("انتقال به سطل بازیافت") },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) },
                onClick = { vm.trashTask(task); onDismiss() }
            )
        } else {
            DropdownMenuItem(
                text = { Text("بایگانی") },
                leadingIcon = { Icon(Icons.Filled.Archive, null) },
                onClick = { vm.archiveTask(task); onDismiss() }
            )
            DropdownMenuItem(
                text = { Text("انتقال به سطل بازیافت") },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) },
                onClick = { vm.trashTask(task); onDismiss() }
            )
        }
    }
}
