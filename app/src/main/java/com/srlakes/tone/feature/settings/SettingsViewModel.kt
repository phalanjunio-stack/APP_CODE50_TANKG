package com.srlakes.tone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srlakes.tone.audio.AudioDeviceProbe
import com.srlakes.tone.audio.InputDeviceOption
import com.srlakes.tone.data.SettingsRepository
import com.srlakes.tone.data.SetlistRepository
import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AppSettings
import com.srlakes.tone.model.Setlist
import com.srlakes.tone.update.UpdateCoordinator
import com.srlakes.tone.update.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val audioProbe: AudioDeviceProbe,
    private val settingsRepository: SettingsRepository,
    setlistRepository: SetlistRepository,
    val updateCoordinator: UpdateCoordinator
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings.DEFAULT)

    val updateState: StateFlow<UpdateState> = updateCoordinator.state

    fun checkForUpdate() = updateCoordinator.checkNow(viewModelScope)

    fun downloadUpdate() = updateCoordinator.downloadAndPrepareInstall(viewModelScope)

    fun skipUpdate(versionName: String) = updateCoordinator.skipVersion(versionName)

    fun dismissUpdateError() = updateCoordinator.dismissError()

    fun setCheckUpdatesAutomatically(value: Boolean) = updateCoordinator.setCheckAutomatically(value)

    val setlists: StateFlow<List<Setlist>> = setlistRepository.observeSetlists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val inputsState = MutableStateFlow(audioProbe.listInputs())
    val audioInputs: StateFlow<List<InputDeviceOption>> = inputsState.asStateFlow()

    fun refreshInputs() {
        inputsState.value = audioProbe.listInputs()
    }

    fun updateAnalyzer(transform: (AnalyzerSettings) -> AnalyzerSettings) {
        viewModelScope.launch { settingsRepository.updateAnalyzer(transform) }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(value) }
    }

    fun setActiveSetlist(id: Long?) {
        viewModelScope.launch { settingsRepository.setActiveSetlist(id) }
    }

}
