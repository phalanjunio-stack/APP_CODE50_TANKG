package com.srlakes.tone.audio

import android.content.Context

/**
 * O que este celular consegue fazer em matéria de captura.
 *
 * Existe para que os ViewModels nao precisem segurar um Context: eles
 * dependem desta interface, e nao do Android. Isso mantem a camada de
 * apresentacao testavel e sem risco de vazar contexto.
 */
interface AudioDeviceProbe {

    /**
     * Sem captura UNPROCESSED, o Android aplica ganho automatico e
     * reducao de ruido, e nenhuma medicao de nivel pode ser levada a
     * serio. A interface mostra isso ao usuario.
     */
    fun supportsUnprocessed(): Boolean

    fun listInputs(): List<InputDeviceOption>

    fun hasUsbInput(): Boolean = listInputs().any { it.isUsb }
}

class AndroidAudioDeviceProbe(private val context: Context) : AudioDeviceProbe {

    override fun supportsUnprocessed(): Boolean = AudioCapabilities.supportsUnprocessed(context)

    override fun listInputs(): List<InputDeviceOption> = AudioCapabilities.listInputs(context)
}
