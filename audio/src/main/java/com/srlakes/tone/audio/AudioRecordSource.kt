package com.srlakes.tone.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.srlakes.tone.model.AudioSourceKind

/**
 * Base das duas fontes reais. Abre um AudioRecord tentando, nesta ordem:
 *
 *  1. UNPROCESSED - sem AGC, sem supressao de ruido. E o unico modo em
 *     que uma medicao de nivel significa alguma coisa.
 *  2. VOICE_RECOGNITION - o Android costuma aplicar menos processamento
 *     aqui do que em MIC.
 *  3. MIC - ultimo recurso. Nesse caso o app avisa que as medidas sao
 *     apenas indicativas.
 *
 * Taxas testadas em ordem: 48000 e depois 44100.
 */
abstract class AudioRecordSource(
    protected val context: Context,
    private val preferUnprocessed: Boolean
) : AudioSource {

    private var record: AudioRecord? = null
    private var currentDescriptor: SourceDescriptor = SourceDescriptor.UNKNOWN

    override val descriptor: SourceDescriptor get() = currentDescriptor

    /** Dispositivo de entrada a preferir, ou null para o padrao do sistema. */
    protected abstract fun pickDevice(audioManager: AudioManager): AudioDeviceInfo?

    protected abstract fun unavailableMessage(): String

    protected open fun requiresDevice(): Boolean = false

    override fun open(): Result<Unit> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure(AudioPermissionException())
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val device = pickDevice(audioManager)
        if (requiresDevice() && device == null) {
            return Result.failure(AudioSourceUnavailableException(unavailableMessage()))
        }

        for (sourceId in buildSourcePriority(audioManager)) {
            for (rate in SAMPLE_RATES) {
                val created = tryCreate(sourceId, rate, device) ?: continue
                record = created
                currentDescriptor = SourceDescriptor(
                    kind = kind,
                    sampleRate = rate,
                    deviceName = device?.productName?.toString(),
                    unprocessed = sourceId == UNPROCESSED_SOURCE,
                    androidAudioSource = sourceId,
                    note = if (sourceId == UNPROCESSED_SOURCE) null else PROCESSED_WARNING
                )
                return runCatching {
                    created.startRecording()
                    if (created.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        throw AudioSourceUnavailableException("O sistema nao iniciou a captura de audio.")
                    }
                }
            }
        }
        return Result.failure(AudioSourceUnavailableException(unavailableMessage()))
    }

    // A permissao de RECORD_AUDIO ja foi verificada em open(), que e o unico
    // chamador deste metodo. O lint nao enxerga isso entre funcoes.
    @SuppressLint("MissingPermission")
    private fun tryCreate(sourceId: Int, rate: Int, device: AudioDeviceInfo?): AudioRecord? {
        val minBuffer = AudioRecord.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return null
        val bufferSize = maxOf(minBuffer * 4, BLOCK_SAMPLES * 2 * 4)
        val recorder = try {
            AudioRecord(
                sourceId,
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: Exception) {
            return null
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return null
        }
        if (device != null) {
            recorder.preferredDevice = device
        }
        return recorder
    }

    private fun buildSourcePriority(audioManager: AudioManager): List<Int> {
        val supportsUnprocessed =
            audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val list = ArrayList<Int>(3)
        if (preferUnprocessed && supportsUnprocessed) list += UNPROCESSED_SOURCE
        list += MediaRecorder.AudioSource.VOICE_RECOGNITION
        list += MediaRecorder.AudioSource.MIC
        return list
    }

    override fun read(buffer: ShortArray): Int {
        val r = record ?: return -1
        return r.read(buffer, 0, buffer.size)
    }

    override fun close() {
        val r = record ?: return
        record = null
        runCatching { if (r.recordingState == AudioRecord.RECORDSTATE_RECORDING) r.stop() }
        runCatching { r.release() }
    }

    companion object {
        val SAMPLE_RATES = intArrayOf(48000, 44100)

        /** Tamanho do bloco lido por vez. Meio quadro de uma FFT de 2048. */
        const val BLOCK_SAMPLES = 1024

        /** Disponivel desde a API 24; o minSdk do projeto e 26. */
        const val UNPROCESSED_SOURCE: Int = MediaRecorder.AudioSource.UNPROCESSED

        const val PROCESSED_WARNING =
            "Este aparelho nao oferece captura UNPROCESSED. O Android esta aplicando ganho " +
                "automatico e reducao de ruido, entao os valores absolutos nao sao confiaveis. " +
                "Comparacoes A/B e BASE x SOLO ainda ajudam, mas com margem de erro."
    }
}

/** Microfone do celular: mede o som real saindo do Marshall na sala. */
class MicrophoneAudioSource(
    context: Context,
    preferUnprocessed: Boolean = true,
    private val preferredDeviceId: Int? = null
) : AudioRecordSource(context, preferUnprocessed) {

    override val kind: AudioSourceKind = AudioSourceKind.AMP_MIC

    override fun pickDevice(audioManager: AudioManager): AudioDeviceInfo? {
        val id = preferredDeviceId ?: return null
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull { it.id == id }
    }

    override fun unavailableMessage(): String =
        "Nao foi possivel abrir o microfone. Verifique se outro aplicativo esta usando o audio."
}

/**
 * Entrada USB (por exemplo o TANK-G ligado por USB-C).
 *
 * Caminho de melhor esforco: depende do Android enumerar o aparelho como
 * USB Audio Class. Muitos celulares fazem; alguns fabricantes bloqueiam.
 * Quando nao da, o app diz isso em vez de mostrar um grafico parado.
 *
 * Lembrete pratico: com o cabo ocupado o celular nao carrega. Para um
 * show inteiro, use um hub OTG com alimentacao.
 */
class UsbAudioSource(
    context: Context,
    preferUnprocessed: Boolean = true,
    private val preferredDeviceId: Int? = null
) : AudioRecordSource(context, preferUnprocessed) {

    override val kind: AudioSourceKind = AudioSourceKind.USB_DIRECT

    override fun requiresDevice(): Boolean = true

    override fun pickDevice(audioManager: AudioManager): AudioDeviceInfo? {
        val inputs = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        preferredDeviceId?.let { id ->
            inputs.firstOrNull { it.id == id }?.let { return it }
        }
        return inputs.firstOrNull { isUsb(it) }
    }

    override fun unavailableMessage(): String =
        "Nenhuma interface USB de audio foi encontrada. Conecte o aparelho por USB-C " +
            "(pode ser necessario um adaptador OTG) e confirme se o Android o reconhece " +
            "como dispositivo de audio."

    companion object {
        fun isUsb(device: AudioDeviceInfo): Boolean =
            device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
    }
}
