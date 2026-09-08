package com.srlakes.tone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.srlakes.tone.di.AppContainer
import com.srlakes.tone.feature.analyzer.AnalyzerViewModel
import com.srlakes.tone.feature.devices.DevicesViewModel
import com.srlakes.tone.feature.history.HistoryViewModel
import com.srlakes.tone.feature.performance.PerformanceViewModel
import com.srlakes.tone.feature.presets.PresetsViewModel
import com.srlakes.tone.feature.settings.SettingsViewModel
import com.srlakes.tone.feature.songs.SongsViewModel

/**
 * Fabrica unica de ViewModels. Sem geracao de codigo, sem anotacoes:
 * cada linha diz exatamente de onde vem cada dependencia.
 */
@Composable
fun rememberViewModelFactory(container: AppContainer): ViewModelProvider.Factory =
    remember(container) {
        viewModelFactory {
            initializer {
                PerformanceViewModel(
                    songRepository = container.songRepository,
                    presetRepository = container.presetRepository,
                    setlistRepository = container.setlistRepository,
                    settingsRepository = container.settingsRepository,
                    deviceManager = container.deviceManager,
                    stageSync = container.stageSync
                )
            }
            initializer {
                SongsViewModel(
                    songRepository = container.songRepository,
                    presetRepository = container.presetRepository,
                    setlistRepository = container.setlistRepository,
                    settingsRepository = container.settingsRepository
                )
            }
            initializer {
                AnalyzerViewModel(
                    audio = container.audioInputManager,
                    settingsRepository = container.settingsRepository,
                    analysisRepository = container.analysisRepository,
                    captureDir = container.captureDir
                )
            }
            initializer {
                PresetsViewModel(
                    presetRepository = container.presetRepository,
                    deviceManager = container.deviceManager
                )
            }
            initializer {
                DevicesViewModel(
                    audioProbe = container.audioDeviceProbe,
                    deviceManager = container.deviceManager,
                    deviceRepository = container.deviceRepository,
                    stageSync = container.stageSync
                )
            }
            initializer {
                SettingsViewModel(
                    audioProbe = container.audioDeviceProbe,
                    settingsRepository = container.settingsRepository,
                    setlistRepository = container.setlistRepository,
                    updateCoordinator = container.updateCoordinator
                )
            }
            initializer {
                HistoryViewModel(repository = container.analysisRepository)
            }
        }
    }
