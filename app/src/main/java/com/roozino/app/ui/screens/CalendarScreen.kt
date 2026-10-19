package com.roozino.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roozino.app.R
import com.roozino.app.data.DataRepository
import com.roozino.app.data.NoteEntity
import com.roozino.app.data.TaskEntity
import com.roozino.app.domain.Jalali
import com.roozino.app.domain.JalaliDate
import com.roozino.app.ui.EmptyState
import com.roozino.app.ui.JalaliMonthGrid
import com.roozino.app.ui.SectionTitle
import com.roozino.app.ui.TaskRow
import com.roozino.app.ui.noteDateText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MonthData(
    val jy: Int,
    val jm: Int,
    val marks: Set<Int>,
    val tasks: List<TaskEntity>,
    val notes: List<NoteEntity>
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel : ViewModel() {
    private val todayJ: JalaliDate = Jalali.today()

    private val _ym = MutableStateFlow(todayJ.year to todayJ.month)
    val ym: StateFlow<Pair<Int, Int>> = _ym.asStateFlow()

    val monthData: StateFlow<MonthData?> =
        _ym.flatMapLatest { (jy, jm) -> flow { emit(loadMonth(jy, jm)) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun prevMonth() {
        val (y, m) = _ym.value
        _ym.value = if (m == 1) (y - 1) to 12 else y to (m - 1)
    }

    fun nextMonth() {
        val (y, m) = _ym.value
        _ym.value = if (m == 12) (y + 1) to 1 else y to (m + 1)
    }

    fun goToday() {
        _ym.value = todayJ.year to todayJ.month
    }

    fun setDone(task: TaskEntity, done: Boolean) =
        viewModelScope.launch { DataRepository.setTaskDone(task, done) }

    fun deleteTask(task: TaskEntity) =
        viewModelScope.launch { DataRepository.deleteTask(task) }

    private suspend fun loadMonth(jy: Int, jm: Int): MonthData {
        val gStart = Jalali.toGregorian(jy, jm, 1)
        val next = if (jm == 12) JalaliDate(jy + 1, 1, 1) else JalaliDate(jy, jm + 1, 1)
        val gEndExclusive = Jalali.toGregorian(next.year, next.month, 1)
        val gEnd = gEndExclusive.minusDays(1)

        val tasks = DataRepository.tasksInRange(gStart.toString(), gEnd.toString())

        val zone = ZoneId.systemDefault()
        val from = gStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = gEndExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        val notes = DataRepository.notesCreatedBetween(from, to)

        val marks = buildSet {
            tasks.forEach { t ->
                t.dueDate?.let { iso ->
                    runCatching {
                        add(Jalali.fromGregorian(LocalDate.parse(iso)).day)
                    }
                }
            }
            notes.forEach { n ->
                val d = Instant.ofEpochMilli(n.createdAt).atZone(zone).toLocalDate()
                add(Jalali.fromGregorian(d).day)
            }
        }
        return MonthData(jy, jm, marks, tasks, notes)
    }
}

@Composable
fun CalendarScreen() {
    val vm: CalendarViewModel = viewModel()
    val ym by vm.ym.collectAsState()
    val data by vm.monthData.collectAsState()

    val todayJ = remember { Jalali.today() }
    var selectedDay by remember(ym) {
        mutableStateOf(
            if (ym.first == todayJ.year && ym.second == todayJ.month) todayJ.day else null
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    JalaliMonthGrid(
                        jy = ym.first,
                        jm = ym.second,
                        marks = data?.marks ?: emptySet(),
                        selectedDay = selectedDay,
                        onDayClick = { day -> selectedDay = if (selectedDay == day) null else day },
                        onPrev = { vm.prevMonth() },
                        onNext = { vm.nextMonth() }
                    )
                    TextButton(onClick = { vm.goToday() }) {
                        Text(stringResource(R.string.calendar_today))
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.calendar_day_items)) }

        val month = data
        if (month == null || selectedDay == null) {
            item { EmptyState(stringResource(R.string.calendar_no_items)) }
        } else {
            val isoDate = Jalali.toGregorian(ym.first, ym.second, selectedDay).toString()
            val dayTasks = month.tasks.filter { it.dueDate == isoDate }
            val zone = ZoneId.systemDefault()
            val dayNotes = month.notes.filter { n ->
                val d = Instant.ofEpochMilli(n.createdAt).atZone(zone).toLocalDate()
                Jalali.fromGregorian(d).day == selectedDay &&
                    Jalali.fromGregorian(d).month == ym.second &&
                    Jalali.fromGregorian(d).year == ym.first
            }
            if (dayTasks.isEmpty() && dayNotes.isEmpty()) {
                item { EmptyState(stringResource(R.string.calendar_no_items)) }
            } else {
                items(dayTasks, key = { "t${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { done -> vm.setDone(task, done) },
                        onDelete = { vm.deleteTask(task) },
                        onEdit = { }
                    )
                }
                items(dayNotes, key = { "n${it.id}" }) { note ->
                    Text(
                        text = buildString {
                            if (note.title.isNotBlank()) {
                                append(note.title)
                                append(" — ")
                            }
                            append(note.content)
                            append("  (")
                            append(noteDateText(note.createdAt))
                            append(")")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
