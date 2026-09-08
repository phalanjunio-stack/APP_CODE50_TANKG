package com.srlakes.tone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AppSettings
import com.srlakes.tone.model.AudioSourceKind
import com.srlakes.tone.model.StageSyncSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "srlakes_settings")

/**
 * Preferencias do app. Tudo local, nada sai do aparelho.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            analyzer = readAnalyzer(prefs),
            sync = readSync(prefs),
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: true,
            activeSetlistId = prefs[KEY_ACTIVE_SETLIST]?.takeIf { it > 0 },
            useMockDevices = prefs[KEY_USE_MOCK] ?: true
        )
    }

    suspend fun updateAnalyzer(transform: (AnalyzerSettings) -> AnalyzerSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(readAnalyzer(prefs))
            prefs[KEY_SOURCE] = next.source.name
            prefs[KEY_INPUT_GAIN] = next.inputGainDb
            prefs[KEY_SENSITIVITY] = next.sensitivityDb
            prefs[KEY_AB_SECONDS] = next.abCaptureSeconds
            prefs[KEY_BASE_SOLO_SECONDS] = next.baseSoloCaptureSeconds
            prefs[KEY_SMOOTHING] = next.spectrumSmoothing
            prefs[KEY_AVERAGING] = next.spectrumAveraging
            prefs[KEY_FPS] = next.refreshRateFps
            prefs[KEY_FFT_SIZE] = next.fftSize
            prefs[KEY_UNPROCESSED] = next.preferUnprocessedMic
            prefs[KEY_MIC_ID] = next.preferredMicId ?: -1
            prefs[KEY_USB_ID] = next.preferredUsbDeviceId ?: -1
        }
    }

    private fun readAnalyzer(prefs: Preferences) = AnalyzerSettings(
        source = prefs[KEY_SOURCE]?.let { name ->
            AudioSourceKind.entries.firstOrNull { it.name == name }
        } ?: AudioSourceKind.AMP_MIC,
        inputGainDb = prefs[KEY_INPUT_GAIN] ?: 0f,
        sensitivityDb = prefs[KEY_SENSITIVITY] ?: 8f,
        abCaptureSeconds = prefs[KEY_AB_SECONDS] ?: 8,
        baseSoloCaptureSeconds = prefs[KEY_BASE_SOLO_SECONDS] ?: 8,
        spectrumSmoothing = prefs[KEY_SMOOTHING] ?: 0.72f,
        spectrumAveraging = prefs[KEY_AVERAGING] ?: 2,
        refreshRateFps = prefs[KEY_FPS] ?: 24,
        fftSize = prefs[KEY_FFT_SIZE] ?: 2048,
        preferUnprocessedMic = prefs[KEY_UNPROCESSED] ?: true,
        preferredMicId = prefs[KEY_MIC_ID]?.takeIf { it >= 0 },
        preferredUsbDeviceId = prefs[KEY_USB_ID]?.takeIf { it >= 0 }
    )

    suspend fun updateSync(transform: (StageSyncSettings) -> StageSyncSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(readSync(prefs))
            prefs[KEY_SYNC_ENABLED] = next.enabled
            prefs[KEY_SYNC_FOLLOW_SONG] = next.followSong
            prefs[KEY_SYNC_MANUAL_URL] = next.manualUrl.orEmpty()
            prefs[KEY_SYNC_LAST_URL] = next.lastUrl.orEmpty()
            prefs[KEY_SYNC_DEVICE_NAME] = next.deviceName
        }
    }

    private fun readSync(prefs: Preferences) = StageSyncSettings(
        enabled = prefs[KEY_SYNC_ENABLED] ?: false,
        followSong = prefs[KEY_SYNC_FOLLOW_SONG] ?: true,
        manualUrl = prefs[KEY_SYNC_MANUAL_URL]?.takeIf { it.isNotBlank() },
        lastUrl = prefs[KEY_SYNC_LAST_URL]?.takeIf { it.isNotBlank() },
        deviceName = prefs[KEY_SYNC_DEVICE_NAME]?.takeIf { it.isNotBlank() } ?: "SR Lakes Tone"
    )

    suspend fun setKeepScreenOn(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_KEEP_SCREEN_ON] = value }
    }

    suspend fun setActiveSetlist(id: Long?) {
        context.settingsDataStore.edit { it[KEY_ACTIVE_SETLIST] = id ?: -1L }
    }

    suspend fun setUseMockDevices(value: Boolean) {
        context.settingsDataStore.edit { it[KEY_USE_MOCK] = value }
    }

    private companion object {
        val KEY_SOURCE = stringPreferencesKey("analyzer_source")
        val KEY_INPUT_GAIN = floatPreferencesKey("analyzer_input_gain")
        val KEY_SENSITIVITY = floatPreferencesKey("analyzer_sensitivity")
        val KEY_AB_SECONDS = intPreferencesKey("analyzer_ab_seconds")
        val KEY_BASE_SOLO_SECONDS = intPreferencesKey("analyzer_base_solo_seconds")
        val KEY_SMOOTHING = floatPreferencesKey("analyzer_smoothing")
        val KEY_AVERAGING = intPreferencesKey("analyzer_averaging")
        val KEY_FPS = intPreferencesKey("analyzer_fps")
        val KEY_FFT_SIZE = intPreferencesKey("analyzer_fft_size")
        val KEY_UNPROCESSED = booleanPreferencesKey("analyzer_unprocessed")
        val KEY_MIC_ID = intPreferencesKey("analyzer_mic_id")
        val KEY_USB_ID = intPreferencesKey("analyzer_usb_id")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_ACTIVE_SETLIST = longPreferencesKey("active_setlist")
        val KEY_USE_MOCK = booleanPreferencesKey("use_mock_devices")
        val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        val KEY_SYNC_FOLLOW_SONG = booleanPreferencesKey("sync_follow_song")
        val KEY_SYNC_MANUAL_URL = stringPreferencesKey("sync_manual_url")
        val KEY_SYNC_LAST_URL = stringPreferencesKey("sync_last_url")
        val KEY_SYNC_DEVICE_NAME = stringPreferencesKey("sync_device_name")
    }
}
