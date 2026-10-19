package com.roozino.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.roozino.app.jalali.JalaliCalendar
import com.roozino.app.util.Fa

/** شبکه ماه شمسی — هم در تقویم و هم در انتخابگر تاریخ استفاده می‌شود */
@Composable
fun MonthGrid(
    jy: Int,
    jm: Int,
    selectedDay: Int?,
    today: Triple<Int, Int, Int>,
    dots: Map<Int, Int> = emptyMap(),
    persian: Boolean,
    onSelect: (Int) -> Unit,
    onChangeMonth: (Int, Int) -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton({ val p = JalaliCalendar.prevMonth(jy, jm); onChangeMonth(p.first, p.second) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "ماه قبل")
            }
            Text(
                "${JalaliCalendar.MONTHS[jm - 1]} ${Fa.num(jy, persian)}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton({ val n = JalaliCalendar.nextMonth(jy, jm); onChangeMonth(n.first, n.second) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "ماه بعد")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            JalaliCalendar.WEEKDAYS_SHORT.forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        val monthLen = JalaliCalendar.monthLength(jy, jm)
        val g = JalaliCalendar.jalaliToGregorian(jy, jm, 1)
        val cal = java.util.Calendar.getInstance()
        cal.clear()
        cal.set(g.first, g.second - 1, g.third)
        val lead = cal.get(java.util.Calendar.DAY_OF_WEEK) % 7
        val rows = (lead + monthLen + 6) / 7
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val day = r * 7 + c - lead + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..monthLen) {
                            val isToday = today.first == jy && today.second == jm && today.third == day
                            val isSelected = selectedDay == day
                            val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val fg = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(bg)
                                    .fillMaxSize()
                                    .clickable { onSelect(day) }
                            ) {
                                Text(
                                    Fa.num(day, persian),
                                    color = fg,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if ((dots[day] ?: 0) > 0) {
                                    Box(
                                        Modifier
                                            .padding(bottom = 2.dp)
                                            .height(4.dp)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiary)
                                    )
                                } else {
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** انتخابگر تاریخ شمسی */
@Composable
fun JalaliDatePickerDialog(
    initial: Long,
    persian: Boolean,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    val t = remember(initial) { JalaliCalendar.fromMillis(initial) }
    var jy by remember { mutableIntStateOf(t.first) }
    var jm by remember { mutableIntStateOf(t.second) }
    var selDay by remember { mutableIntStateOf(t.third) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(16.dp)) {
                MonthGrid(
                    jy = jy, jm = jm, selectedDay = selDay,
                    today = remember { JalaliCalendar.today() },
                    persian = persian,
                    onSelect = { selDay = it },
                    onChangeMonth = { a, b -> jy = a; jm = b }
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text("انصراف") }
                    Button({
                        onPick(JalaliCalendar.jalaliToMillis(jy, jm, selDay))
                        onDismiss()
                    }) { Text("تأیید") }
                }
            }
        }
    }
}

/** انتخابگر ساعت ساده با اعتبارسنجی */
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    persian: Boolean,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit
) {
    var hText by remember { mutableStateOf(if (initialHour in 0..23) Fa.num(initialHour, persian) else "") }
    var mText by remember { mutableStateOf(if (initialMinute in 0..59) Fa.num(initialMinute, persian) else "") }
    var error by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("انتخاب ساعت", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hText,
                        onValueChange = { hText = it },
                        label = { Text("ساعت") },
                        isError = error,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mText,
                        onValueChange = { mText = it },
                        label = { Text("دقیقه") },
                        isError = error,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                if (error) {
                    Text(
                        "ساعت ۰ تا ۲۳ و دقیقه ۰ تا ۵۹",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text("انصراف") }
                    Button({
                        val h = Fa.toEnglishDigits(hText).toIntOrNull()
                        val m = Fa.toEnglishDigits(mText).toIntOrNull()
                        if (h != null && m != null && h in 0..23 && m in 0..59) {
                            onPick(h, m); onDismiss()
                        } else {
                            error = true
                        }
                    }) { Text("تأیید") }
                }
            }
        }
    }
}
