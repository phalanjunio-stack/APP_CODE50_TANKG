package com.srlakes.tone.model

/**
 * Integração com o SR Lakes Studio (o aplicativo do notebook que roda o show).
 *
 * O Studio já transmite o estado ao vivo por Socket.IO na porta 7575. O
 * SR Lakes Tone entra nessa rede como mais um RECEPTOR: ele só escuta,
 * nunca publica. O papel de transmissor continua sendo do notebook.
 */

/** Estado ao vivo recebido do Studio. Espelha o payload do evento "state". */
data class StageState(
    val studioSongId: Int,
    val partIndex: Int,
    val playing: Boolean,
    val bpm: Int,
    val bar: Int,
    val beat: Int,
    val activeChord: String = "",
    val activeLyric: String = "",
    /**
     * Posicao na timeline, em compassos (pode ser fracionaria).
     * E o que permite achar o bloco de secao e o bloco da controladora.
     */
    val timelinePos: Float = 0f,
    val receivedAtMs: Long = System.currentTimeMillis()
)

/**
 * Um bloco de uma trilha da timeline do Studio.
 *
 * Vale tanto para a trilha de secoes (`kind: "section"`, que ja existe)
 * quanto para a trilha Controladora (`kind: "control"`). As duas tem a
 * mesma forma - e o resolvedor de bloco ativo do Studio ja e generico
 * pelo `kind`, entao a segunda entra sem motor novo.
 */
data class StudioClip(
    /** Compasso onde o bloco comeca. Fracionario, como no Studio. */
    val startBar: Float,
    val lengthBars: Float,
    /** O `type` do bloco: intro, verso, prerefrao, refrao, ponte, solo... */
    val type: String,
    /** O `content`: o rotulo que aparece escrito no bloco. */
    val label: String,
    val control: ControlCommand? = null
) {
    fun contains(pos: Float): Boolean = pos >= startBar && pos < startBar + lengthBars

    val sectionType: SectionType? get() = SectionType.fromStudioType(type)

    /** Chave estavel do bloco, para o app so agir quando ele muda de fato. */
    val key: String get() = type + "@" + startBar
}

/**
 * O que um bloco da Controladora manda fazer.
 *
 * Repare no que NAO esta aqui: gain, bass, mid, treble. O bloco diz
 * O QUE ("aqui e SOLO, com boost"), e o SR Lakes Tone continua dono do
 * QUANTO. Uma fonte de verdade so para os valores - e eles precisam ser
 * regulados com o analisador ligado, ouvindo o amplificador, coisa que
 * so existe deste lado.
 */
data class ControlCommand(
    /** Nome da cena, do tipo de cena, ou do preset. Casado sem acento e sem caixa. */
    val scene: String? = null,
    val effects: Map<EffectSlot, Boolean> = emptyMap(),
    /** Volume absoluto na mesma escala dos knobs (0..10), quando presente. */
    val volume: Float? = null
) {
    val isEmpty: Boolean get() = scene == null && effects.isEmpty() && volume == null
}

/** Uma música da biblioteca do Studio, lida de GET /api/songs. */
data class StudioSong(
    val id: Int,
    val title: String,
    val artist: String = "",
    val key: String = "",
    val bpm: Int = 0,
    /** Blocos da trilha `tr-section`, que o Studio ja tem hoje. */
    val sections: List<StudioClip> = emptyList(),
    /** Blocos da trilha Controladora, quando ela existir. */
    val controls: List<StudioClip> = emptyList()
) {
    val sectionCount: Int get() = sections.size

    /** O bloco que esta tocando agora, pela mesma regra do Studio. */
    fun sectionAt(pos: Float): StudioClip? = sections.firstOrNull { it.contains(pos) }

    fun controlAt(pos: Float): StudioClip? = controls.firstOrNull { it.contains(pos) }
}

enum class SyncState(val label: String) {
    OFF("Desligado"),
    SEARCHING("Procurando"),
    CONNECTING("Conectando"),
    CONNECTED("Conectado"),
    ERROR("Erro")
}

data class SyncStatus(
    val state: SyncState = SyncState.OFF,
    val serverUrl: String? = null,
    val serverName: String? = null,
    val lastError: String? = null,
    val lastStateAtMs: Long = 0L
) {
    val connected: Boolean get() = state == SyncState.CONNECTED
}

/**
 * Por que o timbre está (ou não está) seguindo o Studio agora.
 *
 * O estado MANUAL existe por segurança de palco: se o guitarrista mexeu
 * numa cena ou num knob com a mão, o app solta o piloto automático até a
 * próxima troca de música. Ninguém perde o timbre no meio de um solo
 * porque alguém encostou no notebook.
 */
enum class FollowState(val label: String) {
    DISABLED("Desligado"),
    WAITING("Aguardando o Studio"),
    FOLLOWING("Seguindo"),
    MANUAL("Manual"),
    UNMAPPED("Música não associada")
}

data class StageSyncSettings(
    val enabled: Boolean = false,
    /** Muda a cena sozinho quando o Studio troca de música. */
    val followSong: Boolean = true,
    /**
     * Segue também os blocos da timeline: a trilha de seções e, quando
     * existir, a trilha Controladora. A Controladora tem prioridade —
     * ela é o que você desenhou de propósito.
     */
    val followSections: Boolean = true,
    /** Endereço fixo, quando a descoberta automática não funciona. */
    val manualUrl: String? = null,
    /** Último servidor usado, para reconectar sem procurar de novo. */
    val lastUrl: String? = null,
    /** Nome que aparece na lista de aparelhos do Studio. */
    val deviceName: String = "SR Lakes Tone"
) {
    companion object {
        val DEFAULT = StageSyncSettings()
        const val DEFAULT_PORT = 7575
    }
}

/**
 * Casa uma música do Studio com uma música daqui.
 * A primeira associação é automática, por título; o usuário corrige.
 */
data class SongLink(
    val studioSong: StudioSong,
    val localSong: Song?,
    val automatic: Boolean
)

/**
 * Normaliza um título para comparação: sem acento, sem pontuação,
 * sem caixa e sem espaço duplicado. "Só Por Meu Prazer" e
 * "SO POR MEU PRAZER" viram a mesma coisa.
 */
fun normalizeTitle(raw: String): String {
    val decomposed = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
    val sb = StringBuilder(decomposed.length)
    var lastWasSpace = true
    for (ch in decomposed) {
        when {
            ch.code in 0x0300..0x036F -> Unit // marca de acento: descarta
            ch.isLetterOrDigit() -> {
                sb.append(ch.lowercaseChar())
                lastWasSpace = false
            }
            !lastWasSpace -> {
                sb.append(' ')
                lastWasSpace = true
            }
        }
    }
    return sb.toString().trim()
}
