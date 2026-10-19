package com.roozino.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.SearchResults
import com.roozino.app.UiSettings
import com.roozino.app.ui.components.EmptyHint
import com.roozino.app.ui.components.SectionTitle
import com.roozino.app.ui.components.SubScaffold
import com.roozino.app.ui.components.TaskRow
import com.roozino.app.util.Fa
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(SearchResults()) }
    var searching by remember { mutableStateOf(false) }
    val persian = settings.persianDigits

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = SearchResults()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(300)
        results = vm.search(query)
        searching = false
    }

    SubScaffold(title = "جست‌وجوی سراسری", nav = nav) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("در کارها، یادداشت‌ها، رویدادها و یادآوری‌ها…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Text(
                "جست‌وجو با نرمال‌سازی فارسی انجام می‌شود: «ي/ی»، «ك/ک» و نیم‌فاصله تفاوتی ایجاد نمی‌کنند.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (query.isBlank()) {
                EmptyHint("چیزی بنویس تا همه‌جا دنبالش بگردیم.")
            } else if (searching) {
                EmptyHint("در حال جست‌وجو…")
            } else if (results.totalCount() == 0) {
                EmptyHint("چیزی پیدا نشد.")
            } else {
                Text(
                    "${Fa.num(results.totalCount(), persian)} نتیجه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (results.tasks.isNotEmpty()) {
                    SectionTitle("کارها (${Fa.num(results.tasks.size, persian)})")
                    results.tasks.forEach { t ->
                        TaskRow(
                            task = t,
                            persian = persian,
                            onToggle = { if (t.isDone) vm.reopenTask(t) else vm.toggleTaskDone(t) },
                            onClick = { nav.navigate("taskEdit/${t.id}") }
                        )
                    }
                }
                if (results.events.isNotEmpty()) {
                    SectionTitle("رویدادها (${Fa.num(results.events.size, persian)})")
                    results.events.forEach { e ->
                        ResultRow(
                            icon = { Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            title = e.title,
                            subtitle = Fa.dateTime(e.startMillis, persian)
                        )
                    }
                }
                if (results.reminders.isNotEmpty()) {
                    SectionTitle("یادآوری‌ها (${Fa.num(results.reminders.size, persian)})")
                    results.reminders.forEach { r ->
                        ResultRow(
                            icon = { Icon(Icons.Filled.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            title = r.title,
                            subtitle = Fa.dateTime(r.timeMillis, persian)
                        )
                    }
                }
                if (results.notes.isNotEmpty()) {
                    SectionTitle("یادداشت‌ها (${Fa.num(results.notes.size, persian)})")
                    results.notes.forEach { n ->
                        ResultRow(
                            icon = { Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            title = n.title.ifBlank { "بی‌عنوان" },
                            subtitle = n.body.take(80),
                            onClick = { nav.navigate("noteEdit/${n.id}") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        icon()
        Spacer(Modifier.padding(4.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
