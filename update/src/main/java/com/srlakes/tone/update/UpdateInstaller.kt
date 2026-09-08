package com.srlakes.tone.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Instala o APK baixado, usando o instalador do próprio Android.
 *
 * O app nunca instala nada sozinho: a tela final de "Instalar" é sempre
 * do sistema, e o usuário confirma ali. Isto não é um jeito de contornar
 * essa confirmação — é o caminho padrão para um app fora de loja pedir
 * para o Android instalar um arquivo que ele mesmo baixou.
 */
object UpdateInstaller {

    /** Onde os APKs baixados ficam, e o que o FileProvider expõe. */
    fun downloadDir(context: Context): File = File(context.cacheDir, "updates")

    /**
     * A partir do Android 8, o app precisa dessa permissão CONCEDIDA PELO
     * USUÁRIO (não basta declarar no manifesto) para pedir instalação de
     * um APK. Antes disso, "Fontes desconhecidas" era uma configuração
     * do sistema inteiro, não por app.
     */
    fun canRequestInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /** Abre a tela do sistema onde o usuário libera a instalação para este app. */
    fun requestInstallPermissionIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:" + context.packageName)
        }

    /**
     * Monta o Intent que abre o instalador do Android para o APK baixado.
     *
     * Usa FileProvider em vez de um Uri de arquivo direto porque, a
     * partir do Android 7, compartilhar um `file://` entre apps derruba
     * o app com FileUriExposedException. `content://` é a forma correta.
     */
    fun installIntent(context: Context, apkFile: File): Intent {
        val authority = context.packageName + ".fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Apaga APKs baixados anteriormente — não vale acumular versões velhas no cache. */
    fun clearDownloads(context: Context) {
        downloadDir(context).listFiles()?.forEach { it.delete() }
    }
}
