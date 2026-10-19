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
import com.roozino.app.data.NoteEntity
import com.roozino.app.ui.ConfirmDialog
import com.roozino.app.ui.EmptyState
import com.roozino.app.ui.NoteCard
import com.roozino.app.ui.NoteEditorDialog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel : ViewModel() {
    val notes: StateFlow<List<NoteEntity>> =
        DataRepository.allNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, content: String) =
        viewModelScope.launch { DataRepository.addNote(title, content) }

    fun update(note: NoteEntity) = viewModelScope.launch { DataRepository.updateNote(note) }

    fun delete(note: NoteEntity) = viewModelScope.launch { DataRepository.deleteNote(note) }
}

@Composable
fun NotesScreen() {
    val vm: NotesViewModel = viewModel()
    val context = LocalContext.current
    val notes by vm.notes.collectAsState()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<NoteEntity?>(null) }
    var deleting by remember { mutableStateOf<NoteEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (notes.isEmpty()) {
                item { EmptyState(stringResource(R.string.notes_empty)) }
            } else {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { editing = note },
                        onDelete = { deleting = note },
                        onShare = { shareNoteText(context, note.title, note.content) }
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
            Text(stringResource(R.string.add_note))
        }
    }

    if (showAdd) {
        NoteEditorDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { title, content ->
                vm.add(title, content)
                showAdd = false
            }
        )
    }

    editing?.let { note ->
        NoteEditorDialog(
            initial = note,
            onDismiss = { editing = null },
            onSave = { title, content ->
                vm.update(note.copy(title = title, content = content))
                editing = null
            }
        )
    }

    deleting?.let { note ->
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            text = stringResource(R.string.delete_confirm_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.delete(note)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}
