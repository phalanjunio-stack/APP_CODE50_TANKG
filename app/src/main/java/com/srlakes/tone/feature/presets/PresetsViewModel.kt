package com.srlakes.tone.feature.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srlakes.tone.data.PresetRepository
import com.srlakes.tone.device.api.DeviceManager
import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.Macro
import com.srlakes.tone.model.Preset
import com.srlakes.tone.model.PresetCategory
import com.srlakes.tone.protocol.MacroResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PresetsViewModel(
    private val presetRepository: PresetRepository,
    private val deviceManager: DeviceManager
) : ViewModel() {

    val presets: StateFlow<List<Preset>> = presetRepository.observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val editingIdState = MutableStateFlow<Long?>(null)

    val editing: StateFlow<Preset?> = combine(presets, editingIdState) { list, id ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val messageState = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = messageState

    fun open(id: Long?) {
        editingIdState.value = id
    }

    fun create(name: String, category: PresetCategory) {
        viewModelScope.launch {
            val id = presetRepository.save(
                Preset(name = name.ifBlank { "Novo preset" }, category = category)
            )
            editingIdState.value = id
        }
    }

    fun duplicate(preset: Preset) {
        viewModelScope.launch {
            val id = presetRepository.save(
                preset.copy(id = 0L, name = preset.name + " (cópia)", builtIn = false)
            )
            editingIdState.value = id
        }
    }

    fun update(preset: Preset) {
        viewModelScope.launch { presetRepository.save(preset) }
    }

    fun setMacro(preset: Preset, macro: Macro, value: Float) {
        val macros = preset.macros.with(macro, value)
        update(preset.copy(macros = macros, amp = MacroResolver.resolve(macros)))
    }

    fun setAmp(preset: Preset, amp: AmpParams) {
        update(preset.copy(amp = amp, macros = MacroResolver.approximate(amp)))
    }

    fun toggleEffect(preset: Preset, slot: EffectSlot) {
        update(preset.copy(effects = preset.effects.toggle(slot)))
    }

    fun delete(preset: Preset) {
        viewModelScope.launch {
            presetRepository.delete(preset)
            if (editingIdState.value == preset.id) editingIdState.value = null
        }
    }

    /** Manda o preset para os aparelhos agora, para ouvir antes de decidir. */
    fun audition(preset: Preset) {
        viewModelScope.launch {
            val outcome = deviceManager.applyPreset(preset)
            messageState.value = when {
                outcome.failures.isNotEmpty() -> outcome.failures.first()
                outcome.nothingDelivered -> "Nenhum aparelho conectado."
                else -> "Preset " + preset.name + " enviado."
            }
        }
    }

    fun clearMessage() {
        messageState.value = null
    }
}
