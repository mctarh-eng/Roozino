package com.roozino.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.roozino.app.data.Task
import com.roozino.app.ui.components.SectionTitle
import com.roozino.app.ui.components.SubScaffold
import com.roozino.app.ui.components.priorityLabel
import com.roozino.app.ui.components.priorityColor
import com.roozino.app.ui.theme.PriorityHigh
import com.roozino.app.ui.theme.PriorityMedium
import com.roozino.app.util.Fa
import com.roozino.app.util.TimeMath
import androidx.compose.material3.FilterChipDefaults

@Composable
fun TaskEditScreen(
    vm: AppViewModel,
    settings: UiSettings,
    nav: NavController,
    taskId: Long
) {
    val context = LocalContext.current
    var original by remember { mutableStateOf<Task?>(null) }
    var loaded by remember { mutableStateOf(taskId <= 0L) }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf<Long?>(null) }
    var hasTime by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    var priority by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf("") }
    var repeatType by remember { mutableIntStateOf(0) }
    var reminderOn by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId > 0) {
            val t = vm.getTask(taskId)
            if (t != null) {
                original = t
                title = t.title
                desc = t.note ?: ""
                dateMillis = t.dueMillis?.let { TimeMath.startOfDay(it) }
                hasTime = t.hasTime
                t.dueMillis?.let { m ->
                    val c = java.util.Calendar.getInstance().apply { timeInMillis = m }
                    hour = c.get(java.util.Calendar.HOUR_OF_DAY)
                    minute = c.get(java.util.Calendar.MINUTE)
                }
                priority = t.priority
                category = t.category ?: ""
                repeatType = t.repeatType
                reminderOn = t.reminderEnabled
            }
        }
        loaded = true
    }

    SubScaffold(
        title = if (taskId > 0) "ویرایش کار" else "کار جدید",
        nav = nav,
        actions = {
            if (taskId > 0) {
                IconButton(onClick = {
                    original?.let { vm.trashTask(it) }
                    nav.popBackStack()
                }) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = "انتقال به سطل بازیافت")
                }
            }
            IconButton(onClick = {
                if (title.isBlank()) {
                    android.widget.Toast.makeText(context, "عنوان کار را بنویس", android.widget.Toast.LENGTH_SHORT).show()
                    return@IconButton
                }
                val effectiveDue: Long? = dateMillis?.let { d ->
                    if (hasTime) d + hour * 3_600_000L + minute * 60_000L else d
                }
                val base = original ?: Task()
                vm.saveTask(
                    base.copy(
                        title = title.trim(),
                        note = desc.ifBlank { null },
                        dueMillis = effectiveDue,
                        hasTime = effectiveDue != null && hasTime,
                        priority = priority,
                        category = category.ifBlank { null },
                        repeatType = repeatType,
                        reminderEnabled = reminderOn && effectiveDue != null
                    )
                )
                nav.popBackStack()
            }) {
                Icon(Icons.Filled.Check, contentDescription = "ذخیره")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان کار") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("توضیحات (اختیاری)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("زمان‌بندی")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(
                        if (dateMillis != null) Fa.date(dateMillis!!, settings.persianDigits)
                        else "تاریخ: اختیاری"
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    enabled = dateMillis != null
                ) {
                    Text(if (hasTime) "ساعت: ${Fa.num(hour, settings.persianDigits)}:${Fa.num(minute, settings.persianDigits)}" else "ساعت: بدون")
                }
                if (dateMillis != null) {
                    OutlinedButton(onClick = { dateMillis = null; hasTime = false }) { Text("حذف تاریخ") }
                }
            }

            SectionTitle("اولویت")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(0 to "کم", 1 to "متوسط", 2 to "زیاد").forEach { (v, label) ->
                    FilterChip(
                        selected = priority == v,
                        onClick = { priority = v },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (v) {
                                2 -> PriorityHigh.copy(alpha = 0.2f)
                                1 -> PriorityMedium.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    )
                }
            }

            SectionTitle("تکرار")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                listOf(0 to "بدون تکرار", 1 to "روزانه", 2 to "هفتگی", 3 to "ماهانه", 4 to "سالانه")
                    .forEach { (v, label) ->
                        FilterChip(
                            selected = repeatType == v,
                            onClick = { repeatType = v },
                            label = { Text(label) }
                        )
                    }
            }

            SectionTitle("دسته‌بندی")
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("مثلاً: خانه، کار، شخصی (اختیاری)") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("یادآوری")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("یادآوری سرِ وقت", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (dateMillis == null) "ابتدا تاریخ انتخاب کن"
                        else if (reminderOn) "نوتیفیکیشن در زمان مقرر می‌آید"
                        else "فعلاً خاموش است",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderOn && dateMillis != null,
                    onCheckedChange = { reminderOn = it },
                    enabled = dateMillis != null
                )
            }

            if (priority > 0) {
                Text(
                    priorityLabel(priority),
                    color = priorityColor(priority),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (title.isBlank()) {
                        android.widget.Toast.makeText(context, "عنوان کار را بنویس", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val effectiveDue: Long? = dateMillis?.let { d ->
                        if (hasTime) d + hour * 3_600_000L + minute * 60_000L else d
                    }
                    val base = original ?: Task()
                    vm.saveTask(
                        base.copy(
                            title = title.trim(),
                            note = desc.ifBlank { null },
                            dueMillis = effectiveDue,
                            hasTime = effectiveDue != null && hasTime,
                            priority = priority,
                            category = category.ifBlank { null },
                            repeatType = repeatType,
                            reminderEnabled = reminderOn && effectiveDue != null
                        )
                    )
                    nav.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) { Text("ذخیره کار", style = MaterialTheme.typography.titleMedium) }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        com.roozino.app.ui.components.JalaliDatePickerDialog(
            initial = dateMillis ?: System.currentTimeMillis(),
            persian = settings.persianDigits,
            onDismiss = { showDatePicker = false },
            onPick = { picked -> dateMillis = TimeMath.startOfDay(picked) }
        )
    }
    if (showTimePicker) {
        com.roozino.app.ui.components.TimePickerDialog(
            initialHour = if (hasTime) hour else -1,
            initialMinute = if (hasTime) minute else -1,
            persian = settings.persianDigits,
            onDismiss = { showTimePicker = false },
            onPick = { h, m -> hour = h; minute = m; hasTime = true }
        )
    }
}
