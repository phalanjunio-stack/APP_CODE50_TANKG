package com.srlakes.tone.audio

import com.srlakes.tone.model.AudioSourceKind

/**
 * Fonte de audio para MEDICAO.
 *
 * Nada aqui fica no caminho do sinal da guitarra: o audio continua indo
 * normalmente para o amplificador. O aplicativo so escuta em paralelo,
 * e por isso nao adiciona latencia nenhuma durante uma apresentacao.
 */
interface AudioSource {

    val kind: AudioSourceKind

    /** Descreve o que realmente foi aberto (nem sempre e o que foi pedido). */
    val descriptor: SourceDescriptor

    fun open(): Result<Unit>

    /** Leitura bloqueante. @return numero de amostras lidas, ou negativo em erro. */
    fun read(buffer: ShortArray): Int

    fun close()
}

/**
 * O que o sistema efetivamente entregou.
 *
 * [unprocessed] e o campo mais importante do aplicativo inteiro:
 * quando falso, o Android esta aplicando AGC e supressao de ruido no
 * microfone, e nenhuma medicao de nivel pode ser levada a serio.
 * A interface precisa mostrar isso, nao esconder.
 */
data class SourceDescriptor(
    val kind: AudioSourceKind,
    val sampleRate: Int,
    val deviceName: String?,
    val unprocessed: Boolean,
    val androidAudioSource: Int,
    val note: String? = null
) {
    companion object {
        val UNKNOWN = SourceDescriptor(
            kind = AudioSourceKind.AMP_MIC,
            sampleRate = 48000,
            deviceName = null,
            unprocessed = false,
            androidAudioSource = -1
        )
    }
}

class AudioPermissionException : Exception(
    "Permissão de microfone não concedida."
)

class AudioSourceUnavailableException(message: String) : Exception(message)
