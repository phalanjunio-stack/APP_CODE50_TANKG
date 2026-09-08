package com.srlakes.tone.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Baixa o APK de uma release para um arquivo local, relatando progresso.
 *
 * Escreve primeiro num arquivo temporário e só troca de nome no fim: se
 * a corrotina for cancelada (o musico saiu da tela de Configurações no
 * meio do download) ou a conexão cair, nunca sobra um .apk pela metade
 * com nome de arquivo "pronto" por engano.
 */
class ApkDownloader(private val connectTimeoutMs: Int = 10000) {

    suspend fun download(
        url: String,
        destination: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destination.parentFile?.mkdirs()
            val partFile = File(destination.parentFile, destination.name + ".part")

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = true
            try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("O download falhou (HTTP " + code + ").")
                }
                val totalBytes = connection.contentLengthLong

                connection.inputStream.use { input ->
                    partFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Long = 0
                        while (currentCoroutineContext().isActive) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            onProgress(read, totalBytes)
                        }
                        if (!currentCoroutineContext().isActive) {
                            throw java.util.concurrent.CancellationException("Download cancelado.")
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (destination.exists()) destination.delete()
            if (!partFile.renameTo(destination)) {
                throw IllegalStateException("Não foi possível salvar o arquivo baixado.")
            }
            destination
        }
    }
}
