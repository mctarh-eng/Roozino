package com.roozino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roozino.app.R
import com.roozino.app.data.DataRepository
import com.roozino.app.data.NoteEntity
import com.roozino.app.data.TaskEntity
import com.roozino.app.domain.Jalali
import com.roozino.app.domain.PersianText
import com.roozino.app.ui.AddEditTaskDialog
import com.roozino.app.ui.EmptyState
import com.roozino.app.ui.NoteCard
import com.roozino.app.ui.NoteEditorDialog
import com.roozino.app.ui.SectionTitle
import com.roozino.app.ui.TaskRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class TodayViewModel : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    val dateText: String = Jalali.formatFull(today)

    val tasks: StateFlow<List<TaskEntity>> =
        DataRepository.tasksForDate(today.toString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<NoteEntity>> =
        DataRepository.recentNotes(3)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** ثبت سریع: یا کار (با تشخیص تاریخ/ساعت) یا یادداشت */
    fun quickSend(raw: String, asNote: Boolean, onDone: () -> Unit) {
        if (raw.isBlank()) return
        viewModelScope.launch {
            if (asNote) {
                val firstLine = raw.trim().lineSequence().firstOrNull() ?: ""
                DataRepository.addNote(title = firstLine.take(40), content = raw.trim())
            } else {
                val parsed = PersianText.parseQuickEntry(raw, LocalDate.now())
                if (parsed.text.isNotBlank()) {
                    DataRepository.addTask(
                        title = parsed.text,
                        date = parsed.date,
                        time = parsed.time,
                        withReminder = parsed.time != null
                    )
                }
            }
            onDone()
        }
    }

    fun setDone(task: TaskEntity, done: Boolean) =
        viewModelScope.launch { DataRepository.setTaskDone(task, done) }

    fun deleteTask(task: TaskEntity) =
        viewModelScope.launch { DataRepository.deleteTask(task) }

    fun updateTask(task: TaskEntity) =
        viewModelScope.launch { DataRepository.updateTask(task) }

    fun deleteNote(note: NoteEntity) =
        viewModelScope.launch { DataRepository.deleteNote(note) }

    fun updateNote(note: NoteEntity) =
        viewModelScope.launch { DataRepository.updateNote(note) }
}

fun greetingRes(hour: Int): Int = when (hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..16 -> R.string.greeting_afternoon
    in 17..19 -> R.string.greeting_evening
    else -> R.string.greeting_night
}

@Composable
fun QuickEntryBar(modifier: Modifier = Modifier) {
    val vm: TodayViewModel = viewModel()
    var text by remember { mutableStateOf("") }
    var asNote by remember { mutableStateOf(false) }

    val parsed = remember(text) {
        if (asNote) null else PersianText.parseQuickEntry(text, LocalDate.now())
    }

    fun send() {
        if (text.isNotBlank()) vm.quickSend(text, asNote) { text = "" }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            stringResource(if (asNote) R.string.quick_hint_note else R.string.quick_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !asNote,
                        onClick = { asNote = false },
                        label = { Text(stringResource(R.string.quick_type_task)) }
                    )
                    FilterChip(
                        selected = asNote,
                        onClick = { asNote = true },
                        label = { Text(stringResource(R.string.quick_type_note)) }
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { send() }, enabled = text.isNotBlank()) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.quick_add)
                        )
                    }
                }
                if (parsed != null && parsed.time != null) {
                    Text(
                        text = if (parsed.date != null && parsed.date != LocalDate.now()) {
                            stringResource(
                                R.string.quick_will_remind_day,
                                Jalali.formatShort(parsed.date),
                                Jalali.toPersianDigits(parsed.time.toString())
                            )
                        } else {
                            stringResource(
                                R.string.quick_will_remind,
                                Jalali.toPersianDigits(parsed.time.toString())
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TodayScreen() {
    val vm: TodayViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val tasks by vm.tasks.collectAsState()
    val notes by vm.recentNotes.collectAsState()

    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    val greeting = stringResource(greetingRes(LocalTime.now().hour))

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    vm.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
        item { QuickEntryBar() }

        item { SectionTitle(stringResource(R.string.today_tasks_title)) }
        if (tasks.isEmpty()) {
            item { EmptyState(stringResource(R.string.today_empty)) }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { done -> vm.setDone(task, done) },
                    onDelete = { vm.deleteTask(task) },
                    onEdit = { editingTask = task }
                )
            }
        }

        item { SectionTitle(stringResource(R.string.today_notes_title)) }
        if (notes.isEmpty()) {
            item { EmptyState(stringResource(R.string.notes_recent_empty)) }
        } else {
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { editingNote = note },
                    onDelete = { vm.deleteNote(note) },
                    onShare = { shareNoteText(context, note.title, note.content) }
                )
            }
        }
    }

    editingTask?.let { task ->
        AddEditTaskDialog(
            initial = task,
            onDismiss = { editingTask = null },
            onSave = { title, date, time, reminder ->
                vm.updateTask(
                    task.copy(
                        title = title,
                        dueDate = date?.toString(),
                        dueTime = time?.toString(),
                        reminderAt = if (reminder && date != null && time != null) {
                            date.atTime(time).atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        } else null
                    )
                )
                editingTask = null
            }
        )
    }

    editingNote?.let { note ->
        NoteEditorDialog(
            initial = note,
            onDismiss = { editingNote = null },
            onSave = { title, content ->
                vm.updateNote(note.copy(title = title, content = content))
                editingNote = null
            }
        )
    }
}
