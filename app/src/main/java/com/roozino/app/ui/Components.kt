@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.roozino.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.roozino.app.R
import com.roozino.app.data.NoteEntity
import com.roozino.app.data.TaskEntity
import com.roozino.app.domain.Jalali
import com.roozino.app.domain.JalaliDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// ---------- قالب‌بندی متادیتا ----------

fun taskMetaText(task: TaskEntity): String {
    val parts = mutableListOf<String>()
    task.dueDate?.let { iso ->
        runCatching { Jalali.formatShort(LocalDate.parse(iso)) }.getOrNull()?.let { parts.add(it) }
    }
    task.dueTime?.let { parts.add(Jalali.toPersianDigits(it)) }
    return if (parts.isEmpty()) "" else parts.joinToString("، ")
}

fun reminderTimeText(epochMillis: Long): String {
    val t = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    return Jalali.toPersianDigits(t.toString())
}

fun noteDateText(createdAt: Long): String {
    val d = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return Jalali.formatShort(d)
}

// ---------- حالت خالی و عنوان بخش ----------

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.WbSunny,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 14.dp, bottom = 6.dp)
    )
}

// ---------- ردیف کار ----------

@Composable
fun TaskRow(
    task: TaskEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = onToggle)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = taskMetaText(task)
                if (meta.isNotEmpty() || task.reminderAt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (meta.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        task.reminderAt?.let {
                            Icon(
                                Icons.Filled.Alarm,
                                contentDescription = stringResource(R.string.set_reminder),
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                reminderTimeText(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- کارت یادداشت ----------

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(14.dp)
        ) {
            if (note.title.isNotBlank()) {
                Text(
                    note.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    noteDateText(note.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------- دیالوگ تأیید حذف ----------

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ---------- تقویم شمسی (مشترک بین صفحه تقویم و انتخاب تاریخ) ----------

@Composable
fun JalaliMonthGrid(
    jy: Int,
    jm: Int,
    marks: Set<Int> = emptySet(),
    selectedDay: Int? = null,
    today: JalaliDate = Jalali.today(),
    onDayClick: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = stringResource(R.string.previous)
                )
            }
            Text(
                text = "${Jalali.monthName(jm)} ${Jalali.toPersianDigits(jy.toString())}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.next)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Jalali.weekdayHeaders.forEach { header ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        header,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        val length = Jalali.monthLength(jy, jm)
        val offset = Jalali.firstWeekdayOffset(jy, jm)
        val cells: List<Int?> = List(offset) { null } + (1..length).toList()
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val isToday = today.year == jy && today.month == jm && today.day == day
                            val isSelected = selectedDay == day
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDayClick(day) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    Jalali.toPersianDigits(day.toString()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (day in marks) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 1.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---------- دیالوگ انتخاب تاریخ شمسی ----------

@Composable
fun JalaliDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val todayJ = remember { Jalali.today() }
    var jy by remember { mutableStateOf(initial?.let { Jalali.fromGregorian(it).year } ?: todayJ.year) }
    var jm by remember { mutableStateOf(initial?.let { Jalali.fromGregorian(it).month } ?: todayJ.month) }
    var selected by remember {
        mutableStateOf(initial?.let { Jalali.fromGregorian(it) } ?: todayJ)
    }
    val selectedVisible = selected.year == jy && selected.month == jm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_date)) },
        text = {
            JalaliMonthGrid(
                jy = jy,
                jm = jm,
                selectedDay = if (selectedVisible) selected.day else null,
                onDayClick = { day -> selected = JalaliDate(jy, jm, day) },
                onPrev = { if (jm == 1) { jm = 12; jy -= 1 } else jm -= 1 },
                onNext = { if (jm == 12) { jm = 1; jy += 1 } else jm += 1 }
            )
        },
        confirmButton = {
            TextButton(onClick = { onPick(Jalali.toGregorian(jy, jm, selected.day)) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ---------- دیالوگ افزودن/ویرایش کار ----------

@Composable
fun AddEditTaskDialog(
    initial: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, date: LocalDate?, time: LocalTime?, reminder: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var date by remember {
        mutableStateOf(
            initial?.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        )
    }
    var time by remember {
        mutableStateOf(
            initial?.dueTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        )
    }
    var reminder by remember { mutableStateOf(initial?.reminderAt != null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_task else R.string.edit_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDate = true }) {
                        Text(
                            date?.let { Jalali.formatShort(it) }
                                ?: stringResource(R.string.pick_date)
                        )
                    }
                    TextButton(onClick = { showTime = true }) {
                        Text(
                            time?.let { Jalali.toPersianDigits(it.toString()) }
                                ?: stringResource(R.string.pick_time)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.set_reminder),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = reminder,
                        onCheckedChange = { reminder = it },
                        enabled = date != null && time != null
                    )
                }
                if (date != null && time != null && reminder) {
                    Text(
                        stringResource(R.string.reminder_will_schedule),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), date, time, reminder) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )

    if (showDate) {
        JalaliDatePickerDialog(
            initial = date,
            onDismiss = { showDate = false },
            onPick = { date = it; showDate = false }
        )
    }
    if (showTime) {
        val state = rememberTimePickerState(
            initialHour = time?.hour ?: 9,
            initialMinute = time?.minute ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    time = LocalTime.of(state.hour, state.minute)
                    showTime = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ---------- دیالوگ یادداشت ----------

@Composable
fun NoteEditorDialog(
    initial: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_note else R.string.edit_note)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.note_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.note_content)) },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank() || content.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
