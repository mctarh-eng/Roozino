package com.roozino.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roozino.app.R
import com.roozino.app.ui.screens.CalendarScreen
import com.roozino.app.ui.screens.NotesScreen
import com.roozino.app.ui.screens.SearchScreen
import com.roozino.app.ui.screens.SettingsScreen
import com.roozino.app.ui.screens.TasksScreen
import com.roozino.app.ui.screens.TodayScreen

object Routes {
    const val TODAY = "today"
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val CALENDAR = "calendar"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
}

private data class BottomItem(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun RoozinoRoot() {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(SettingsStore.themeMode(context)) }
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // درخواست مجوز اعلان (اندروید ۱۳ به بالا) — یک‌بار در شروع
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    RoozinoTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(color = MaterialTheme.colorScheme.background) {
                MainScaffold(
                    onThemeModeChange = { mode ->
                        SettingsStore.setThemeMode(context, mode)
                        themeMode = mode
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(onThemeModeChange: (ThemeMode) -> Unit) {
    val navController: NavHostController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val items = listOf(
        BottomItem(Routes.TODAY, R.string.tab_today, Icons.Filled.Today),
        BottomItem(Routes.TASKS, R.string.tab_tasks, Icons.Filled.TaskAlt),
        BottomItem(Routes.NOTES, R.string.tab_notes, Icons.Filled.EditNote),
        BottomItem(Routes.CALENDAR, R.string.tab_calendar, Icons.Filled.CalendarMonth),
        BottomItem(Routes.SETTINGS, R.string.tab_settings, Icons.Filled.Settings)
    )

    val titleRes = when (currentRoute) {
        Routes.TASKS -> R.string.tab_tasks
        Routes.NOTES -> R.string.tab_notes
        Routes.CALENDAR -> R.string.tab_calendar
        Routes.SETTINGS -> R.string.tab_settings
        Routes.SEARCH -> R.string.search_title
        else -> R.string.tab_today
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                actions = {
                    if (currentRoute != Routes.SEARCH) {
                        IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_title)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(Routes.TODAY) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.TODAY) { TodayScreen() }
            composable(Routes.TASKS) { TasksScreen() }
            composable(Routes.NOTES) { NotesScreen() }
            composable(Routes.CALENDAR) { CalendarScreen() }
            composable(Routes.SETTINGS) { SettingsScreen(onThemeModeChange = onThemeModeChange) }
            composable(Routes.SEARCH) { SearchScreen() }
        }
    }
}
