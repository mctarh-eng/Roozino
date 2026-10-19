package com.roozino.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.roozino.app.AppViewModel
import com.roozino.app.QuickType
import com.roozino.app.UiSettings
import com.roozino.app.util.Fa
import com.roozino.app.util.TimeMath

/**
 * ثبت سریع — فقط نوشتن کافی است؛ هیچ فیلدی اجباری نیست.
 * برای «یادآوری»، زمان به‌صورت پیش‌فرض پر می‌شود ولی قابل ویرایش است (تأیید پیش از ساخت).
 */
@Composable
fun QuickAddDialog(
    vm: AppViewModel,
    settings: UiSettings,
    initialType: QuickType = QuickType.TASK,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initialType) }
    var text by remember { mutableStateOf("") }
    // پیش‌فرض زمان یادآوری = همین حالا + ۱ ساعت (قابل ویرایش پیش از ثبت)
    var reminderTime by remember {
        mutableStateOf(System.currentTimeMillis() + 3_600_000L)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val results = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!results.isNullOrEmpty() && results[0].isNotBlank()) {
            text = if (text.isBlank()) results[0] else "$text ${results[0]}"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("ثبت سریع", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = type == QuickType.TASK,
                        onClick = { type = QuickType.TASK },
                        label = { Text("کار") }
                    )
                    FilterChip(
                        selected = type == QuickType.NOTE,
                        onClick = { type = QuickType.NOTE },
                        label = { Text("یادداشت") }
                    )
                    FilterChip(
                        selected = type == QuickType.REMINDER,
                        onClick = { type = QuickType.REMINDER },
                        label = { Text("یادآوری") }
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = {
                        Text(
                            when (type) {
                                QuickType.TASK -> "مثلاً: تماس با بانک"
                                QuickType.NOTE -> "مثلاً: ایده برای پروژه"
                                QuickType.REMINDER -> "مثلاً: پرداخت قبض برق"
                            }
                        )
                    },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    trailing = {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "بگو چی یادت باشه…")
                                }
                                speechLauncher.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                android.widget.Toast.makeText(
                                    context,
                                    "گفتار به متن روی این گوشی در دسترس نیست",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = "ورودی صوتی")
                        }
                    }
                )
                if (type == QuickType.REMINDER) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Text("تاریخ: ${Fa.date(reminderTime, settings.persianDigits)}")
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text("ساعت: ${Fa.time(reminderTime, settings.persianDigits)}")
                        }
                    }
                    Text(
                        "صوتی‌ات با سرویس گفتار خود گوشی پردازش می‌شود؛ متن قابل ویرایش است و پیش از ثبت تأیید می‌کنی.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                } else {
                    Text(
                        "برای ثبت، همین نوشتن کافی است — بقیه را می‌توانی بعداً کامل کنی.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onDismiss) { Text("انصراف") }
                    Button(
                        onClick = {
                            vm.addQuick(text, type, reminderTime)
                            onDismiss()
                        },
                        enabled = text.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.height(0.dp))
                        Text(" ثبت")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = reminderTime,
            persian = settings.persianDigits,
            onDismiss = { showDatePicker = false },
            onPick = { picked ->
                val timePart = reminderTime - TimeMath.startOfDay(reminderTime)
                reminderTime = TimeMath.startOfDay(picked) + timePart
            }
        )
    }
    if (showTimePicker) {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = reminderTime }
        TimePickerDialog(
            initialHour = c.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = c.get(java.util.Calendar.MINUTE),
            persian = settings.persianDigits,
            onDismiss = { showTimePicker = false },
            onPick = { h, m ->
                reminderTime = TimeMath.startOfDay(reminderTime) + h * 3_600_000L + m * 60_000L
            }
        )
    }
}

/** دیالوگ ساخت/ویرایش یادآوری مستقل */
@Composable
fun ReminderDialog(
    settings: UiSettings,
    initial: com.roozino.app.data.Reminder?,
    defaultTime: Long,
    onDismiss: () -> Unit,
    onSave: (com.roozino.app.data.Reminder) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var time by remember { mutableStateOf(initial?.timeMillis ?: defaultTime) }
    var repeat by remember { mutableStateOf(initial?.repeatType ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    if (initial == null) "یادآوری جدید" else "ویرایش یادآوری",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("مثلاً: قرص بخور") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text("تاریخ: ${Fa.date(time, settings.persianDigits)}")
                    }
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text("ساعت: ${Fa.time(time, settings.persianDigits)}")
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(0 to "یک‌بار", 1 to "روزانه", 2 to "هفتگی", 3 to "ماهانه").forEach { (v, label) ->
                        FilterChip(
                            selected = repeat == v,
                            onClick = { repeat = v },
                            label = { Text(label) }
                        )
                    }
                }
                if (time < System.currentTimeMillis() && repeat == 0) {
                    Text(
                        "زمان انتخاب‌شده در گذشته است؛ این یادآوری زنگ نمی‌خورد.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text("انصراف") }
                    Button(
                        onClick = {
                            onSave(
                                com.roozino.app.data.Reminder(
                                    id = initial?.id ?: 0,
                                    title = title.trim(),
                                    timeMillis = time,
                                    repeatType = repeat,
                                    enabled = true,
                                    taskRef = initial?.taskRef,
                                    createdAt = initial?.createdAt ?: 0,
                                    updatedAt = initial?.updatedAt ?: 0
                                )
                            )
                            onDismiss()
                        },
                        enabled = title.isNotBlank()
                    ) { Text("ذخیره") }
                }
            }
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = time,
            persian = settings.persianDigits,
            onDismiss = { showDatePicker = false },
            onPick = { picked ->
                val timePart = time - TimeMath.startOfDay(time)
                time = TimeMath.startOfDay(picked) + timePart
            }
        )
    }
    if (showTimePicker) {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = time }
        TimePickerDialog(
            initialHour = c.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = c.get(java.util.Calendar.MINUTE),
            persian = settings.persianDigits,
            onDismiss = { showTimePicker = false },
            onPick = { h, m -> time = TimeMath.startOfDay(time) + h * 3_600_000L + m * 60_000L }
        )
    }
}

/** دیالوگ ساخت رویداد برای روز انتخاب‌شده در تقویم */
@Composable
fun EventDialog(
    settings: UiSettings,
    dayMillis: Long,
    onDismiss: () -> Unit,
    onSave: (title: String, startMillis: Long, endMillis: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(dayMillis + 9 * 3_600_000L) }
    var end by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("رویداد جدید", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("مثلاً: جلسه با تیم") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text("روز: ${Fa.date(start, settings.persianDigits)}")
                    }
                    OutlinedButton(onClick = { showStartPicker = true }) {
                        Text("شروع: ${Fa.time(start, settings.persianDigits)}")
                    }
                    OutlinedButton(onClick = { showEndPicker = true }) {
                        Text(
                            if (end != null) "پایان: ${Fa.time(end!!, settings.persianDigits)}"
                            else "پایان: اختیاری"
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text("انصراف") }
                    Button(
                        onClick = {
                            onSave(title.trim(), start, end)
                            onDismiss()
                        },
                        enabled = title.isNotBlank()
                    ) { Text("ذخیره") }
                }
            }
        }
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initial = start,
            persian = settings.persianDigits,
            onDismiss = { showDatePicker = false },
            onPick = { picked ->
                val dayDiff = picked - TimeMath.startOfDay(start)
                start += dayDiff
                end = end?.plus(dayDiff)
            }
        )
    }
    if (showStartPicker) {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = start }
        TimePickerDialog(
            initialHour = c.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = c.get(java.util.Calendar.MINUTE),
            persian = settings.persianDigits,
            onDismiss = { showStartPicker = false },
            onPick = { h, m ->
                val day = TimeMath.startOfDay(start)
                start = day + h * 3_600_000L + m * 60_000L
                end?.let { e -> end = day + (e - TimeMath.startOfDay(e)) }
            }
        )
    }
    if (showEndPicker) {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = end ?: start }
        TimePickerDialog(
            initialHour = c.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = c.get(java.util.Calendar.MINUTE),
            persian = settings.persianDigits,
            onDismiss = { showEndPicker = false },
            onPick = { h, m ->
                end = TimeMath.startOfDay(start) + h * 3_600_000L + m * 60_000L
            }
        )
    }
}
