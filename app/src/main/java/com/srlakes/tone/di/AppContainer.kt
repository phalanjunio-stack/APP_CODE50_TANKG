package com.srlakes.tone.di

import android.content.Context
import com.srlakes.tone.audio.AndroidAudioDeviceProbe
import com.srlakes.tone.audio.AudioDeviceProbe
import com.srlakes.tone.audio.AudioInputManager
import com.srlakes.tone.data.AnalysisRepository
import com.srlakes.tone.data.DemoData
import com.srlakes.tone.data.DeviceRepository
import com.srlakes.tone.data.PresetRepository
import com.srlakes.tone.data.SettingsRepository
import com.srlakes.tone.data.SetlistRepository
import com.srlakes.tone.data.SongRepository
import com.srlakes.tone.data.db.ToneDatabase
import com.srlakes.tone.device.api.DeviceManager
import com.srlakes.tone.device.api.MarshallCodeService
import com.srlakes.tone.device.api.TankGService
import com.srlakes.tone.device.mock.MockMarshallCodeService
import com.srlakes.tone.device.mock.MockTankGService
import com.srlakes.tone.sync.StageDiscovery
import com.srlakes.tone.sync.StageSyncCoordinator
import com.srlakes.tone.update.UpdateCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/**
 * Injecao de dependencia feita a mao.
 *
 * Um app deste tamanho nao precisa de Hilt: um container explicito e
 * mais facil de ler, compila mais rapido e deixa obvio onde trocar o
 * simulado pelo real.
 *
 * ESTE E O UNICO ARQUIVO que precisa mudar quando o Bluetooth de verdade
 * ficar pronto. Veja o README, secao "Trocar os MockServices".
 */
class AppContainer(private val context: Context) {

    val applicationContext: Context = context.applicationContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: ToneDatabase by lazy { ToneDatabase.get(context) }

    val songRepository by lazy { SongRepository(database.songDao()) }
    val presetRepository by lazy { PresetRepository(database.presetDao()) }
    val setlistRepository by lazy { SetlistRepository(database.setlistDao()) }
    val deviceRepository by lazy { DeviceRepository(database.deviceDao()) }
    val analysisRepository by lazy { AnalysisRepository(database.analysisDao()) }
    val settingsRepository by lazy { SettingsRepository(context) }

    // ---------------------------------------------------------------
    // Aparelhos.
    //
    // Fase 1: simulados. Toda a interface funciona, e o log da tela
    // Dispositivos mostra o que TERIA sido enviado.
    //
    // Fase 2: trocar as duas linhas abaixo por BleMidiTankGService e
    // UsbMidiMarshallCodeService. Nada mais no app precisa mudar,
    // porque tudo depende apenas das interfaces de :device:api.
    // ---------------------------------------------------------------
    private val tankG: TankGService by lazy { MockTankGService(appScope) }
    private val code50: MarshallCodeService by lazy { MockMarshallCodeService(appScope) }

    val deviceManager: DeviceManager by lazy { DeviceManager(tankG, code50) }

    val audioInputManager: AudioInputManager by lazy { AudioInputManager(context) }

    val audioDeviceProbe: AudioDeviceProbe by lazy { AndroidAudioDeviceProbe(applicationContext) }

    /**
     * Ponte com o SR Lakes Studio (o aplicativo do notebook).
     * Fica desligada por padrao: so liga quando voce liga na tela
     * Dispositivos. Sem o Studio na rede, o app funciona igual.
     */
    val stageSync: StageSyncCoordinator by lazy {
        StageSyncCoordinator(
            scope = appScope,
            settingsRepository = settingsRepository,
            songRepository = songRepository,
            discovery = StageDiscovery(applicationContext)
        )
    }

    /**
     * Verificador de novas versoes, publicadas como GitHub Releases.
     * O app nao esta em loja nenhuma - isto substitui "gerar apk e
     * mandar por WhatsApp para cada musico instalar na mao".
     */
    val updateCoordinator: UpdateCoordinator by lazy { UpdateCoordinator(applicationContext) }

    /** Onde ficam os WAV das medicoes guardadas. */
    val captureDir: File by lazy {
        File(context.filesDir, "captures").apply { mkdirs() }
    }

    suspend fun seedDemoDataIfNeeded() {
        DemoData.seedIfEmpty(database)
    }
}
