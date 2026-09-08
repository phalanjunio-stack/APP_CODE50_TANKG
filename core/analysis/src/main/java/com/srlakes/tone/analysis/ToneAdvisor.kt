package com.srlakes.tone.analysis

import com.srlakes.tone.model.AudioAnalysis
import com.srlakes.tone.model.InsightSeverity
import com.srlakes.tone.model.ToneBand
import com.srlakes.tone.model.ToneInsight

/**
 * Interpretacao do que foi medido. Deliberadamente uma INTERFACE:
 * a versao com IA, se um dia existir, entra aqui sem tocar em mais nada.
 * A implementacao padrao e 100% offline e baseada em regras.
 */
interface ToneAdvisor {
    fun evaluate(analysis: AudioAnalysis): List<ToneInsight>
    fun reset()
}

/**
 * Regras simples sobre a distribuicao de energia. Nada aqui altera
 * parametro nenhum - sao apenas sugestoes para o musico decidir.
 *
 * Cada regra passa por uma histerese: precisa se manter verdadeira por
 * alguns quadros para aparecer, e falsa por varios para sumir. Sem isso
 * a lista fica piscando e vira ruido visual no palco.
 */
class RuleBasedToneAdvisor(
    private val framesToShow: Int = 8,
    private val framesToHide: Int = 20
) : ToneAdvisor {

    private val hits = HashMap<String, Int>()
    private val visible = HashSet<String>()

    override fun reset() {
        hits.clear()
        visible.clear()
    }

    override fun evaluate(analysis: AudioAnalysis): List<ToneInsight> {
        val candidates = ArrayList<ToneInsight>(6)

        if (analysis.clipping) {
            candidates += ToneInsight(
                id = "clipping",
                title = "Entrada saturando",
                message = "O nível de entrada está estourando. Reduza o volume do amp ou afaste o celular.",
                severity = InsightSeverity.ALERT,
                excess = true
            )
        }

        if (analysis.noiseFloorDb > -45f) {
            candidates += ToneInsight(
                id = "noise",
                title = "Ruído elevado",
                message = "O piso de ruído está alto (${fmt(analysis.noiseFloorDb)} dB). Verifique gate, cabo ou ambiente.",
                severity = InsightSeverity.WARN,
                excess = true
            )
        }

        if (!analysis.hasSignal) {
            candidates += ToneInsight(
                id = "nosignal",
                title = "Sem sinal",
                message = "Toque alguma coisa. As medições precisam de sinal acima do ruído.",
                severity = InsightSeverity.INFO
            )
            return stabilize(candidates)
        }

        val mud = analysis.diagnostics[AudioAnalysis.DIAG_MUD]
        val harsh = analysis.diagnostics[AudioAnalysis.DIAG_HARSH]
        val fizz = analysis.diagnostics[AudioAnalysis.DIAG_FIZZ]
        val midShare = analysis.bands.share(ToneBand.MID)
        val lowMidShare = analysis.bands.share(ToneBand.LOW_MID)

        if (mud > 0.30f || lowMidShare > 0.44f) {
            candidates += ToneInsight(
                id = "mud",
                title = "250 Hz embolado",
                message = "Muita energia entre 150 e 300 Hz. Som pode estar embolado — reduza um pouco nessa região.",
                severity = InsightSeverity.WARN,
                band = ToneBand.LOW_MID,
                excess = true
            )
        }

        if (harsh > 0.20f) {
            candidates += ToneInsight(
                id = "harsh",
                title = "3.8 kHz áspero",
                message = "Concentração entre 3 e 5 kHz. Som pode estar áspero — suavize Presence ou Treble.",
                severity = InsightSeverity.WARN,
                band = ToneBand.HIGH_MID,
                excess = true
            )
        }

        if (fizz > 0.14f) {
            candidates += ToneInsight(
                id = "fizz",
                title = "Excesso de brilho",
                message = "Muita energia de 6 a 10 kHz. Fizz típico de gain alto — considere um IR mais escuro.",
                severity = InsightSeverity.WARN,
                band = ToneBand.HIGH,
                excess = true
            )
        }

        if (midShare < 0.14f) {
            candidates += ToneInsight(
                id = "scooped",
                title = "Médios escavados",
                message = "Pouca energia entre 800 Hz e 2 kHz. O som pode desaparecer na mistura da banda.",
                severity = InsightSeverity.WARN,
                band = ToneBand.MID,
                excess = false
            )
        }

        if (analysis.peakDb < -24f) {
            candidates += ToneInsight(
                id = "low_input",
                title = "Sinal fraco",
                message = "Pico em ${fmt(analysis.peakDb)} dB. Aproxime o celular do amp para medir melhor.",
                severity = InsightSeverity.INFO,
                excess = false
            )
        }

        if (candidates.none { it.severity == InsightSeverity.WARN || it.severity == InsightSeverity.ALERT }) {
            candidates += ToneInsight(
                id = "clean_signal",
                title = "Sem clipping",
                message = "Nível seguro e limpo.",
                severity = InsightSeverity.OK
            )
        }

        if (analysis.crestFactorDb in 8f..20f) {
            candidates += ToneInsight(
                id = "dynamics",
                title = "Dinâmica boa",
                message = "Crest factor de ${fmt(analysis.crestFactorDb)} dB — boa variação entre limpo e forte.",
                severity = InsightSeverity.OK
            )
        }

        return stabilize(candidates)
    }

    private fun stabilize(candidates: List<ToneInsight>): List<ToneInsight> {
        val present = candidates.associateBy { it.id }
        val keys = HashSet<String>(hits.keys)
        keys.addAll(present.keys)

        for (key in keys) {
            val current = hits[key] ?: 0
            if (present.containsKey(key)) {
                hits[key] = (current + 1).coerceAtMost(framesToShow + framesToHide)
                if (hits[key]!! >= framesToShow) visible.add(key)
            } else {
                val next = current - 1
                if (next <= -framesToHide) {
                    hits.remove(key)
                    visible.remove(key)
                } else {
                    hits[key] = next
                }
            }
        }

        return candidates
            .filter { visible.contains(it.id) }
            .sortedByDescending { it.severity.ordinal }
    }

    private fun fmt(v: Float): String = String.format("%.1f", v)
}
