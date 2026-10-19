package com.roozino.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roozino.app.R
import com.roozino.app.data.DataRepository
import com.roozino.app.data.NoteEntity
import com.roozino.app.data.TaskEntity
import com.roozino.app.domain.PersianText
import com.roozino.app.ui.EmptyState
import com.roozino.app.ui.SectionTitle
import com.roozino.app.ui.taskMetaText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class SearchResults(val tasks: List<TaskEntity>, val notes: List<NoteEntity>)

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel : ViewModel() {
    private val query = MutableStateFlow("")

    val results: StateFlow<SearchResults?> =
        query.flatMapLatest { q ->
            flow {
                emit(
                    if (PersianText.normalize(q).length < 2) null
                    else SearchResults(
                        DataRepository.searchTasks(q),
                        DataRepository.searchNotes(q)
                    )
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setQuery(q: String) {
        query.value = q
    }
}

@Composable
fun SearchScreen() {
    val vm: SearchViewModel = viewModel()
    val results by vm.results.collectAsState()
    var text by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                vm.setQuery(it)
            },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
        )
        Text(
            stringResource(R.string.search_normalization_info),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 6.dp)
        )

        val r = results
        when {
            r == null -> {
                Spacer(Modifier.padding(top = 24.dp))
                EmptyState(stringResource(R.string.search_min_hint))
            }
            r.tasks.isEmpty() && r.notes.isEmpty() -> {
                Spacer(Modifier.padding(top = 24.dp))
                EmptyState(stringResource(R.string.search_empty))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    val count = r.tasks.size + r.notes.size
                    item {
                        Text(
                            stringResource(R.string.search_results_count, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (r.tasks.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.search_badge_task)) }
                        items(r.tasks, key = { "t${it.id}" }) { task ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(task.title, style = MaterialTheme.typography.bodyLarge)
                                    val meta = taskMetaText(task)
                                    if (meta.isNotEmpty()) {
                                        Text(
                                            meta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (r.notes.isNotEmpty()) {
                        item { SectionTitle(stringResource(R.string.search_badge_note)) }
                        items(r.notes, key = { "n${it.id}" }) { note ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    if (note.title.isNotBlank()) {
                                        Text(
                                            note.title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                    Text(
                                        note.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
