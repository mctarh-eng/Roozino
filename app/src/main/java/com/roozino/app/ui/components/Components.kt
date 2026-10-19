package com.roozino.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.roozino.app.data.Task
import com.roozino.app.ui.theme.PriorityHigh
import com.roozino.app.ui.theme.PriorityLow
import com.roozino.app.ui.theme.PriorityMedium
import com.roozino.app.util.Fa
import com.roozino.app.util.TimeMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    title: String,
    selectedRoute: String,
    nav: NavController,
    topActions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                actions = topActions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = { RoozinoBottomNav(selected, nav) },
        floatingActionButton = fab,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScaffold(
    title: String,
    nav: NavController,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        content = content
    )
}

@Composable
fun RoozinoBottomNav(selected: String, nav: NavController) {
    NavigationBar {
        NavItem(Icons.Filled.Home, Icons.Outlined.Home, "امروز", "today", selected, nav)
        NavItem(Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "کارها", "tasks", selected, nav)
        NavItem(Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "تقویم", "calendar", selected, nav)
        NavItem(Icons.Filled.Description, Icons.Outlined.Description, "یادداشت‌ها", "notes", selected, nav)
    }
}

@Composable
private fun NavItem(
    filled: ImageVector,
    outlined: ImageVector,
    label: String,
    route: String,
    selected: String,
    nav: NavController
) {
    NavigationBarItem(
        selected = selected == route,
        onClick = {
            nav.navigate(route) {
                popUpTo("today") { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(if (selected == route) filled else outlined, contentDescription = label) },
        label = { Text(label) }
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
    )
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

fun priorityColor(priority: Int): Color = when (priority) {
    2 -> PriorityHigh
    1 -> PriorityMedium
    else -> PriorityLow
}

fun priorityLabel(priority: Int): String = when (priority) {
    2 -> "اولویت زیاد"
    1 -> "اولویت متوسط"
    else -> "اولویت کم"
}

@Composable
fun TaskRow(
    task: Task,
    persian: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                task.dueMillis?.let { m ->
                    val overdue = !task.isDone && m < TimeMath.startOfDay(System.currentTimeMillis())
                    Text(
                        if (task.hasTime) "${Fa.relativeDay(m, persian)} ${Fa.time(m, persian)}"
                        else Fa.relativeDay(m, persian),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.priority > 0) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(priorityColor(task.priority))
                    )
                    Text(
                        priorityLabel(task.priority),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                task.category?.let {
                    Text(
                        "• $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (task.repeatType > 0) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "تکرارشونده",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        trailing?.invoke()
    }
}
