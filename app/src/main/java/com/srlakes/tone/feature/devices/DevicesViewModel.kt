package com.srlakes.tone.feature.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srlakes.tone.audio.AudioDeviceProbe
import com.srlakes.tone.audio.InputDeviceOption
import com.srlakes.tone.data.DeviceRepository
import com.srlakes.tone.device.api.DeviceManager
import com.srlakes.tone.device.api.ToneDeviceService
import com.srlakes.tone.device.api.TransportLogEntry
import com.srlakes.tone.model.DeviceInfo
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.protocol.DeviceProfile
import com.srlakes.tone.sync.StageSyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevicesViewModel(
    private val audioProbe: AudioDeviceProbe,
    private val deviceManager: DeviceManager,
    private val deviceRepository: DeviceRepository,
    val stageSync: StageSyncCoordinator
) : ViewModel() {

    val infos: StateFlow<List<DeviceInfo>> = deviceManager.infos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<DeviceProfile>> =
        combine(deviceManager.tankG.profile, deviceManager.code50.profile) { a, b -> listOf(a, b) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val logState = MutableStateFlow<List<TransportLogEntry>>(emptyList())
    val log: StateFlow<List<TransportLogEntry>> = logState.asStateFlow()

    private val busyState = MutableStateFlow<DeviceKind?>(null)
    val busy: StateFlow<DeviceKind?> = busyState.asStateFlow()

    private val audioInputsState = MutableStateFlow<List<InputDeviceOption>>(emptyList())
    val audioInputs: StateFlow<List<InputDeviceOption>> = audioInputsState.asStateFlow()

    val unprocessedSupported: Boolean = audioProbe.supportsUnprocessed()

    init {
        viewModelScope.launch {
            merge(deviceManager.tankG.log, deviceManager.code50.log).collect { entry ->
                logState.value = (logState.value + entry).takeLast(120)
            }
        }
        refreshAudioInputs()
    }

    fun refreshAudioInputs() {
        audioInputsState.value = audioProbe.listInputs()
    }

    fun connect(kind: DeviceKind) {
        val service = serviceFor(kind)
        viewModelScope.launch {
            busyState.value = kind
            val found = service.scan()
            val address = found.firstOrNull()?.address
            service.connect(address)
            deviceRepository.saveProfile(service.profile.value, address, service.info.value.transport)
            busyState.value = null
        }
    }

    fun disconnect(kind: DeviceKind) {
        viewModelScope.launch { serviceFor(kind).disconnect() }
    }

    fun clearLog() {
        logState.value = emptyList()
    }

    private fun serviceFor(kind: DeviceKind): ToneDeviceService = when (kind) {
        DeviceKind.TANK_G -> deviceManager.tankG
        DeviceKind.CODE50 -> deviceManager.code50
    }
}
