package com.srlakes.tone.protocol

import com.srlakes.tone.model.DeviceKind

/**
 * Todo parametro que o app sabe pedir a um aparelho.
 * A lista e a uniao do que TANK-G e CODE50 expoem.
 */
enum class ParamId(val label: String) {
    PREAMP("Preamp"),
    GAIN("Gain"),
    BASS("Bass"),
    MIDDLE("Middle"),
    TREBLE("Treble"),
    PRESENCE("Presence"),
    RESONANCE("Resonance"),
    VOLUME("Volume"),
    POWER_AMP("Power Amp"),
    CABINET("Cabinet"),
    IR("IR"),
    PRE_FX("Pre FX"),
    MODULATION("Modulation"),
    DELAY("Delay"),
    DELAY_TIME("Delay Time"),
    DELAY_LEVEL("Delay Level"),
    REVERB("Reverb"),
    REVERB_LEVEL("Reverb Level"),
    GATE("Gate"),
    GATE_THRESHOLD("Gate Threshold"),
    BOOST("Boost")
}

/**
 * Ligacao entre um parametro e um Control Change.
 *
 * [learned] = descoberto pelo usuario na tela Protocol Lab (MIDI Learn),
 * e nao chutado por nos. Um mapeamento aprendido tem prioridade e e
 * persistido. Enquanto nao houver nenhum, o servico opera em modo
 * simulado e nao envia nada para o aparelho.
 */
data class ParamMapping(
    val param: ParamId,
    val controller: Int,
    val channel: Int = 0,
    val minValue: Int = 0,
    val maxValue: Int = 127,
    val learned: Boolean = false
) {
    /** Converte 0.0..1.0 no valor MIDI correspondente. */
    fun toMidiValue(normalized: Float): Int {
        val v = normalized.coerceIn(0f, 1f)
        return (minValue + v * (maxValue - minValue)).toInt().coerceIn(0, 127)
    }

    fun fromMidiValue(value: Int): Float {
        val span = (maxValue - minValue).coerceAtLeast(1)
        return ((value - minValue).toFloat() / span).coerceIn(0f, 1f)
    }
}

/**
 * Perfil de um aparelho: canal MIDI, mapeamentos conhecidos e a lista
 * de programas (presets internos) que ele expoe.
 *
 * NADA aqui e adivinhado. Um perfil novo comeca VAZIO. Os mapeamentos
 * chegam de duas formas:
 *   1. MIDI Learn - o usuario mexe no knob do aparelho e o app aprende;
 *   2. CC Sweep - o app varre CCs e o usuario marca o que reconheceu.
 *
 * Ver o README, secao "Onde entra o protocolo real".
 */
data class DeviceProfile(
    val kind: DeviceKind,
    val name: String,
    val channel: Int = 0,
    val mappings: Map<ParamId, ParamMapping> = emptyMap(),
    val programCount: Int = 0,
    val programBaseIndex: Int = 0
) {
    val isMapped: Boolean get() = mappings.isNotEmpty()

    fun mappingFor(param: ParamId): ParamMapping? = mappings[param]

    fun supports(param: ParamId): Boolean = mappings.containsKey(param)

    /**
     * @return a mensagem a enviar, ou null se o parametro ainda nao foi mapeado.
     * Retornar null e proposital: melhor nao enviar nada do que enviar
     * um CC aleatorio para um aparelho de palco.
     */
    fun messageFor(param: ParamId, normalized: Float): MidiMessage.ControlChange? {
        val mapping = mappings[param] ?: return null
        return MidiMessage.ControlChange(
            channel = mapping.channel,
            controller = mapping.controller,
            value = mapping.toMidiValue(normalized)
        )
    }

    fun programMessage(program: Int): MidiMessage.ProgramChange =
        MidiMessage.ProgramChange(channel, (programBaseIndex + program).coerceIn(0, 127))

    fun withMapping(mapping: ParamMapping): DeviceProfile =
        copy(mappings = mappings + (mapping.param to mapping))

    fun withoutMapping(param: ParamId): DeviceProfile =
        copy(mappings = mappings - param)

    companion object {
        /**
         * Perfis iniciais: vazios de proposito.
         * Preencher via Protocol Lab depois de verificar o aparelho.
         */
        fun empty(kind: DeviceKind, name: String = kind.displayName) = DeviceProfile(kind, name)
    }
}

/**
 * Estado do MIDI Learn: o usuario escolhe um parametro, mexe no knob
 * fisico, e a primeira mensagem CC estavel que chegar vira o mapeamento.
 */
class MidiLearnSession(val target: ParamId) {

    private val counts = HashMap<Int, Int>()
    private var lastChannel = 0

    var result: ParamMapping? = null
        private set

    /** @return true quando o aprendizado terminou. */
    fun observe(message: MidiMessage): Boolean {
        if (result != null) return true
        val cc = message as? MidiMessage.ControlChange ?: return false
        lastChannel = cc.channel
        val n = (counts[cc.controller] ?: 0) + 1
        counts[cc.controller] = n
        if (n >= MESSAGES_TO_CONFIRM) {
            result = ParamMapping(
                param = target,
                controller = cc.controller,
                channel = lastChannel,
                learned = true
            )
            return true
        }
        return false
    }

    fun progress(): Float {
        val best = counts.values.maxOrNull() ?: 0
        return (best.toFloat() / MESSAGES_TO_CONFIRM).coerceIn(0f, 1f)
    }

    companion object {
        /** Girar um knob gera varias mensagens; exigimos algumas para ter certeza. */
        const val MESSAGES_TO_CONFIRM = 4
    }
}
