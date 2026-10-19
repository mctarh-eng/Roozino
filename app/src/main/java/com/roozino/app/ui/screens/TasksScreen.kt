package com.roozino.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roozino.app.R
import com.roozino.app.data.DataRepository
import com.roozino.app.data.TaskEntity
import com.roozino.app.ui.AddEditTaskDialog
import com.roozino.app.ui.ConfirmDialog
import com.roozino.app.ui.EmptyState
import com.roozino.app.ui.SectionTitle
import com.roozino.app.ui.TaskRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TasksViewModel : ViewModel() {
    val active: StateFlow<List<TaskEntity>> =
        DataRepository.activeTasks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val done: StateFlow<List<TaskEntity>> =
        DataRepository.doneTasks()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, date: LocalDate?, time: LocalTime?, reminder: Boolean) =
        viewModelScope.launch {
            DataRepository.addTask(title, date, time, reminder)
        }

    fun update(task: TaskEntity) = viewModelScope.launch { DataRepository.updateTask(task) }

    fun setDone(task: TaskEntity, done: Boolean) =
        viewModelScope.launch { DataRepository.setTaskDone(task, done) }

    fun delete(task: TaskEntity) = viewModelScope.launch { DataRepository.deleteTask(task) }
}

@Composable
fun TasksScreen() {
    val vm: TasksViewModel = viewModel()
    val active by vm.active.collectAsState()
    val done by vm.done.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var deleting by remember { mutableStateOf<TaskEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item { SectionTitle(stringResource(R.string.tasks_active)) }
            if (active.isEmpty() && done.isEmpty()) {
                item { EmptyState(stringResource(R.string.tasks_empty)) }
            } else if (active.isEmpty()) {
                item { EmptyState(stringResource(R.string.tasks_empty)) }
            } else {
                items(active, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { d -> vm.setDone(task, d) },
                        onDelete = { deleting = task },
                        onEdit = { editing = task }
                    )
                }
            }
            if (done.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.tasks_done)) }
                items(done, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { d -> vm.setDone(task, d) },
                        onDelete = { deleting = task },
                        onEdit = { editing = task }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(stringResource(R.string.add_task))
        }
    }

    if (showAdd) {
        AddEditTaskDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { title, date, time, reminder ->
                vm.add(title, date, time, reminder)
                showAdd = false
            }
        )
    }

    editing?.let { task ->
        AddEditTaskDialog(
            initial = task,
            onDismiss = { editing = null },
            onSave = { title, date, time, reminder ->
                vm.update(
                    task.copy(
                        title = title,
                        dueDate = date?.toString(),
                        dueTime = time?.toString(),
                        reminderAt = if (reminder && date != null && time != null) {
                            date.atTime(time).atZone(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        } else null
                    )
                )
                editing = null
            }
        )
    }

    deleting?.let { task ->
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            text = stringResource(R.string.delete_confirm_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.delete(task)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}
