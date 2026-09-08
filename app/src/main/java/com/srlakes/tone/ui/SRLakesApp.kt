package com.srlakes.tone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.srlakes.tone.di.AppContainer
import com.srlakes.tone.feature.analyzer.AnalyzerScreen
import com.srlakes.tone.feature.analyzer.AnalyzerViewModel
import com.srlakes.tone.feature.devices.DevicesScreen
import com.srlakes.tone.feature.devices.DevicesViewModel
import com.srlakes.tone.feature.history.HistoryScreen
import com.srlakes.tone.feature.history.HistoryViewModel
import com.srlakes.tone.feature.performance.PerformanceScreen
import com.srlakes.tone.feature.performance.PerformanceViewModel
import com.srlakes.tone.feature.presets.PresetsScreen
import com.srlakes.tone.feature.presets.PresetsViewModel
import com.srlakes.tone.feature.settings.SettingsScreen
import com.srlakes.tone.feature.settings.SettingsViewModel
import com.srlakes.tone.feature.songs.SongEditorScreen
import com.srlakes.tone.feature.songs.SongsScreen
import com.srlakes.tone.feature.songs.SongsViewModel
import com.srlakes.tone.ui.theme.SR

enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    PERFORMANCE("performance", "Performance", Icons.Filled.Home),
    SONGS("songs", "Músicas", Icons.Filled.LibraryMusic),
    ANALYZER("analyzer", "Analisador", Icons.Filled.Equalizer),
    PRESETS("presets", "Presets", Icons.Filled.Tune),
    DEVICES("devices", "Dispositivos", Icons.Filled.Hub),
    SETTINGS("settings", "Ajustes", Icons.Filled.Settings)
}

@Composable
fun SRLakesApp(container: AppContainer) {

    val navController = rememberNavController()
    val factory = rememberViewModelFactory(container)

    // Escopo da Activity: a tela Performance guarda o estado do show,
    // e trocar de aba durante uma musica nao pode perder a cena atual.
    val performanceViewModel: PerformanceViewModel = viewModel(factory = factory)
    val songsViewModel: SongsViewModel = viewModel(factory = factory)
    val analyzerViewModel: AnalyzerViewModel = viewModel(factory = factory)
    val presetsViewModel: PresetsViewModel = viewModel(factory = factory)
    val devicesViewModel: DevicesViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
    val historyViewModel: HistoryViewModel = viewModel(factory = factory)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SR.Background)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = TopDestination.PERFORMANCE.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(TopDestination.PERFORMANCE.route) {
                    PerformanceScreen(performanceViewModel)
                }
                composable(TopDestination.SONGS.route) {
                    SongsScreen(
                        viewModel = songsViewModel,
                        onOpenSong = { id -> navController.navigate("songs/" + id) },
                        onPlaySong = { song ->
                            performanceViewModel.selectSong(song)
                            navController.navigateTop(TopDestination.PERFORMANCE.route)
                        }
                    )
                }
                composable(
                    route = "songs/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.LongType })
                ) { entry ->
                    SongEditorScreen(
                        viewModel = songsViewModel,
                        songId = entry.arguments?.getLong("songId") ?: 0L,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(TopDestination.ANALYZER.route) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) { AnalyzerScreen(analyzerViewModel) }
                    }
                }
                composable(TopDestination.PRESETS.route) {
                    PresetsScreen(presetsViewModel)
                }
                composable(TopDestination.DEVICES.route) {
                    DevicesScreen(devicesViewModel)
                }
                composable(TopDestination.SETTINGS.route) {
                    SettingsScreen(settingsViewModel)
                }
                composable("history") {
                    HistoryScreen(historyViewModel) { navController.popBackStack() }
                }
            }
        }

        BottomBar(navController)
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SR.Surface)
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SR.Outline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TopDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route ||
                    (currentRoute?.startsWith(destination.route + "/") == true)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { navController.navigateTop(destination.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) SR.Electric else SR.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = destination.label,
                        color = if (selected) SR.Electric else SR.TextTertiary,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Navegacao entre abas sem empilhar historico infinito. */
private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
