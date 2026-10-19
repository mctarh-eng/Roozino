package com.roozino.app.ui.nav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roozino.app.AppViewModel
import com.roozino.app.UiSettings
import com.roozino.app.ui.screens.BackupScreen
import com.roozino.app.ui.screens.CalendarScreen
import com.roozino.app.ui.screens.NoteEditScreen
import com.roozino.app.ui.screens.NotesScreen
import com.roozino.app.ui.screens.RemindersScreen
import com.roozino.app.ui.screens.SearchScreen
import com.roozino.app.ui.screens.SettingsScreen
import com.roozino.app.ui.screens.TaskEditScreen
import com.roozino.app.ui.screens.TasksScreen
import com.roozino.app.ui.screens.TodayScreen
import com.roozino.app.ui.screens.WelcomeScreen

@Composable
fun RoozinoNav(vm: AppViewModel, settings: UiSettings) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "today"

    // خودترمیمی یادآوری‌ها هنگام باز شدن برنامه
    LaunchedEffect(Unit) { vm.rescheduleAllNow() }

    // درخواست مجوز اعلان (اندروید ۱۳+) یک‌بار بعد از خوش‌آمد
    var notifRequested by rememberSaveable { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(settings.welcomeDone) {
        if (settings.welcomeDone && !notifRequested && Build.VERSION.SDK_INT >= 33) {
            notifRequested = true
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (!settings.loaded) {
        Box(Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (settings.welcomeDone) "today" else "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(onStart = { vm.setSetting("welcome_done", "1") })
        }
        composable("today") { TodayScreen(vm, settings, navController) }
        composable("tasks") { TasksScreen(vm, settings, navController) }
        composable("calendar") { CalendarScreen(vm, settings, navController) }
        composable("notes") { NotesScreen(vm, settings, navController) }
        composable("reminders") { RemindersScreen(vm, settings, navController) }
        composable("search") { SearchScreen(vm, settings, navController) }
        composable("settings") { SettingsScreen(vm, settings, navController) }
        composable("backup") { BackupScreen(vm, navController) }
        composable("taskEdit/{taskId}") { entry ->
            TaskEditScreen(
                vm = vm,
                settings = settings,
                nav = navController,
                taskId = entry.arguments?.getString("taskId")?.toLongOrNull() ?: -1L
            )
        }
        composable("noteEdit/{noteId}") { entry ->
            NoteEditScreen(
                vm = vm,
                nav = navController,
                noteId = entry.arguments?.getString("noteId")?.toLongOrNull() ?: -1L
            )
        }
    }
}
