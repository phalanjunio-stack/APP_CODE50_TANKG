package com.srlakes.tone.update

import android.content.Context
import com.srlakes.tone.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Verifica, baixa e prepara a instalação de novas versões do app,
 * publicadas como GitHub Releases.
 *
 * O app não está em loja nenhuma. Sem isto, atualizar significa gerar
 * um APK, mandar por WhatsApp e cada músico instalar na mão — o que
 * você descreveu como chato o bastante para pedir isto.
 *
 * O fluxo é sempre: verificar -> baixar -> o Android pergunta se
 * instala. O app nunca instala nada sem essa confirmação do sistema.
 */
class UpdateCoordinator(
    private val context: Context,
    private val settingsStore: UpdateSettingsStore = UpdateSettingsStore(context),
    private val checker: GithubReleaseChecker = GithubReleaseChecker(SOURCE),
    private val downloader: ApkDownloader = ApkDownloader()
) {

    val currentVersionName: String = BuildConfig.VERSION_NAME

    private val stateFlow = MutableStateFlow<UpdateState>(UpdateState.UpToDate)
    val state: StateFlow<UpdateState> = stateFlow.asStateFlow()

    val settings: UpdateSettings get() = settingsStore.read()

    /**
     * Confere silenciosamente ao abrir o app, respeitando o intervalo
     * mínimo entre checagens e a versão que o músico já dispensou.
     * Nunca interrompe nada: só popula o estado, se houver algo novo.
     */
    fun checkOnStartIfDue(scope: CoroutineScope) {
        val prefs = settingsStore.read()
        if (!prefs.checkAutomatically) return
        val elapsed = System.currentTimeMillis() - prefs.lastCheckedAtMs
        if (elapsed < MIN_INTERVAL_MS) return
        scope.launch { check(silent = true) }
    }

    fun checkNow(scope: CoroutineScope) {
        scope.launch { check(silent = false) }
    }

    private suspend fun check(silent: Boolean) {
        if (!silent) stateFlow.value = UpdateState.Checking
        settingsStore.markCheckedNow()

        checker.latest()
            .onSuccess { info ->
                val skipped = settingsStore.read().skippedVersion
                val isNewer = VersionComparator.isNewer(info.versionName, currentVersionName)
                stateFlow.value = when {
                    !isNewer -> UpdateState.UpToDate
                    silent && skipped == info.versionName -> UpdateState.UpToDate
                    else -> UpdateState.Available(info)
                }
            }
            .onFailure { error ->
                // Uma checagem silenciosa que falha (sem internet, por
                // exemplo) não deve virar um erro vermelho na tela.
                if (!silent) {
                    stateFlow.value = UpdateState.Error(error.message ?: "Falha ao verificar atualizações.")
                }
            }
    }

    fun downloadAndPrepareInstall(scope: CoroutineScope) {
        val available = stateFlow.value as? UpdateState.Available ?: return
        scope.launch {
            val info = available.info
            stateFlow.value = UpdateState.Downloading(info, 0L, info.assetSizeBytes)

            val destination = File(UpdateInstaller.downloadDir(context), info.assetName)
            downloader.download(info.downloadUrl, destination) { bytesRead, totalBytes ->
                stateFlow.value = UpdateState.Downloading(
                    info,
                    bytesRead,
                    if (totalBytes > 0L) totalBytes else info.assetSizeBytes
                )
            }
                .onSuccess { file ->
                    stateFlow.value = UpdateState.ReadyToInstall(info, file.absolutePath)
                }
                .onFailure { error ->
                    stateFlow.value = UpdateState.Error(error.message ?: "Falha ao baixar a atualização.")
                }
        }
    }

    fun skipVersion(versionName: String) {
        settingsStore.skip(versionName)
        stateFlow.value = UpdateState.UpToDate
    }

    fun setCheckAutomatically(value: Boolean) {
        settingsStore.setCheckAutomatically(value)
    }

    fun dismissError() {
        stateFlow.value = UpdateState.UpToDate
    }

    fun canRequestInstall(): Boolean = UpdateInstaller.canRequestInstall(context)

    companion object {
        private const val MIN_INTERVAL_MS = 6L * 60 * 60 * 1000 // 6 horas

        /** O repositório publicado nesta conversa. */
        val SOURCE = UpdateSource(owner = "phalanjunio-stack", repo = "APP_CODE50_TANKG")
    }
}
