package com.srlakes.tone.update

/**
 * Configuracao de onde procurar novas versoes.
 *
 * O app nao esta na Play Store: e distribuido diretamente pelo GitHub
 * Releases do repositorio da banda. Isso significa apk sem loja
 * nenhuma no meio, entao e o proprio app que confere se ha versao
 * nova e cuida de baixar e pedir para instalar.
 */
data class UpdateSource(
    val owner: String,
    val repo: String
) {
    val apiUrl: String get() = "https://api.github.com/repos/$owner/$repo/releases/latest"
    val releasesPageUrl: String get() = "https://github.com/$owner/$repo/releases"
}

/** Uma versao publicada no GitHub, ja com o link direto do APK. */
data class UpdateInfo(
    val versionName: String,
    val tagName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val assetName: String,
    val assetSizeBytes: Long,
    val publishedAtIso: String
)

/**
 * Preferencias do atualizador. Guardadas junto das outras configuracoes
 * do app (AppSettings), nao aqui - este modulo nao sabe nada de
 * DataStore nem de Room, so do formato dos dados.
 */
data class UpdateSettings(
    val checkAutomatically: Boolean = true,
    val lastCheckedAtMs: Long = 0L,
    /** Versao que o usuario dispensou explicitamente ("agora nao"). */
    val skippedVersion: String? = null
)

sealed interface UpdateState {
    /** Nada verificado ainda, ou verificacao concluida e ja na versao mais nova. */
    data object UpToDate : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val bytesRead: Long, val totalBytes: Long) : UpdateState {
        val fraction: Float get() = if (totalBytes <= 0L) 0f else (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f)
    }
    data class ReadyToInstall(val info: UpdateInfo, val apkPath: String) : UpdateState
    data class Error(val message: String) : UpdateState
}
