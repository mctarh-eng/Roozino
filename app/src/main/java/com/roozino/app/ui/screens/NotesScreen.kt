package com.roozino.app.ui.screens

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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.AppViewModel
import com.roozino.app.UiSettings
import com.roozino.app.data.Note
import com.roozino.app.ui.components.EmptyHint
import com.roozino.app.ui.components.MainScaffold
import com.roozino.app.util.Fa
import com.roozino.app.util.PersianNormalizer

@Composable
fun NotesScreen(vm: AppViewModel, settings: UiSettings, nav: NavController) {
    val notes by vm.notes.collectAsState()
    var query by remember { mutableStateOf("") }

    val persian = settings.persianDigits
    val active = notes.filter { !it.deleted && !it.archived }
    val filtered = if (query.isBlank()) active else active.filter {
        PersianNormalizer.contains(it.title + " " + it.body, query)
    }
    val pinned = filtered.filter { it.pinned }
    val others = filtered.filter { !it.pinned }

    MainScaffold(
        title = "یادداشت‌ها",
        selectedRoute = "notes",
        nav = nav,
        topActions = {
            IconButton(onClick = { nav.navigate("search") }) {
                Icon(Icons.Filled.Search, contentDescription = "جست‌وجو")
            }
        },
        fab = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("noteEdit/-1") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("یادداشت جدید") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("جست‌وجو در یادداشت‌ها…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyHint(
                            if (query.isBlank()) "هنوز یادداشتی نداری — با دکمه پایین اولین یادداشتت را بنویس."
                            else "چیزی پیدا نشد."
                        )
                    }
                }
                if (pinned.isNotEmpty()) {
                    item {
                        Text(
                            "سنجاق‌شده",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    items(pinned, key = { it.id }) { note ->
                        NoteCard(note, persian, vm) { nav.navigate("noteEdit/${note.id}") }
                    }
                }
                if (others.isNotEmpty()) {
                    if (pinned.isNotEmpty()) {
                        item {
                            Text(
                                "بقیه",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(others, key = { it.id }) { note ->
                        NoteCard(note, persian, vm) { nav.navigate("noteEdit/${note.id}") }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, persian: Boolean, vm: AppViewModel, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    note.title.ifBlank { "بی‌عنوان" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.toggleNotePin(note) }) {
                    Icon(
                        if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.pinned) "برداشتن سنجاق" else "سنجاق‌کردن",
                        modifier = Modifier.size(18.dp),
                        tint = if (note.pinned) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (note.body.isNotBlank()) {
                Text(
                    note.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            Text(
                Fa.date(note.updatedAt, persian),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
