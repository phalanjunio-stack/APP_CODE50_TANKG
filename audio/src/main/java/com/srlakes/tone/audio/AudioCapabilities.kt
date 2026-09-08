package com.srlakes.tone.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import java.io.File
import java.io.FileOutputStream

data class InputDeviceOption(
    val id: Int,
    val name: String,
    val typeLabel: String,
    val isUsb: Boolean
)

/**
 * O que este celular consegue fazer. A tela de Configuracoes mostra
 * isso literalmente, porque muda a confianca que se pode ter nas medidas.
 */
object AudioCapabilities {

    fun supportsUnprocessed(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
    }

    fun listInputs(context: Context): List<InputDeviceOption> {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_INPUTS).map { device ->
            InputDeviceOption(
                id = device.id,
                name = device.productName?.toString().orEmpty().ifBlank { "Entrada " + device.id },
                typeLabel = typeLabel(device.type),
                isUsb = UsbAudioSource.isUsb(device)
            )
        }
    }

    fun hasUsbInput(context: Context): Boolean = listInputs(context).any { it.isUsb }

    private fun typeLabel(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Microfone interno"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB (headset)"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB (acessorio)"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "P2"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        else -> "Outra (" + type + ")"
    }
}

/**
 * Grava o PCM cru de uma medicao em WAV 16 bits mono.
 *
 * Guardar o audio custa cerca de 1 MB por medicao e permite reouvir e
 * reanalisar depois com regras novas. Sem isso, um A/B feito no ensaio
 * de hoje some para sempre.
 */
object WavWriter {

    fun write(file: File, pcm: ShortArray, sampleRate: Int) {
        val dataSize = pcm.size * 2
        FileOutputStream(file).use { out ->
            out.write(header(dataSize, sampleRate))
            val bytes = ByteArray(dataSize)
            var i = 0
            for (sample in pcm) {
                bytes[i++] = (sample.toInt() and 0xFF).toByte()
                bytes[i++] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            out.write(bytes)
        }
    }

    private fun header(dataSize: Int, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val h = ByteArray(44)

        fun putAscii(offset: Int, text: String) {
            for (i in text.indices) h[offset + i] = text[i].code.toByte()
        }

        fun putInt(offset: Int, value: Int) {
            h[offset] = (value and 0xFF).toByte()
            h[offset + 1] = ((value shr 8) and 0xFF).toByte()
            h[offset + 2] = ((value shr 16) and 0xFF).toByte()
            h[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }

        fun putShort(offset: Int, value: Int) {
            h[offset] = (value and 0xFF).toByte()
            h[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putAscii(0, "RIFF")
        putInt(4, 36 + dataSize)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, channels)
        putInt(24, sampleRate)
        putInt(28, byteRate)
        putShort(32, blockAlign)
        putShort(34, bitsPerSample)
        putAscii(36, "data")
        putInt(40, dataSize)
        return h
    }
}
