package com.srlakes.tone.ui.util

import androidx.compose.ui.graphics.Color
import com.srlakes.tone.model.InsightSeverity
import com.srlakes.tone.model.LevelState
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.ui.theme.SR
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {

    // Criados a cada chamada de proposito: guardar um SimpleDateFormat com
    // o Locale padrao num objeto quebra se o usuario trocar o idioma com o
    // aplicativo aberto - e SimpleDateFormat nao e thread-safe.
    private fun formatter(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

    fun oneDecimal(value: Float): String = String.format(Locale.US, "%.1f", value)

    /** Valores em dBFS. Sempre com o sufixo, para ninguem confundir com dB SPL. */
    fun db(value: Float): String =
        if (value <= -119f) "--" else String.format(Locale.US, "%.1f dB", value)

    /** Diferencas sempre com sinal explicito: e o numero que importa no BASE x SOLO. */
    fun delta(value: Float): String = String.format(Locale.US, "%+.1f dB", value)

    fun hz(value: Float): String = when {
        value <= 0f -> "--"
        value >= 1000f -> String.format(Locale.US, "%.1f kHz", value / 1000f)
        else -> String.format(Locale.US, "%.0f Hz", value)
    }

    fun percent(value: Float): String = String.format(Locale.US, "%.0f%%", value * 100f)

    fun date(millis: Long): String = formatter("dd/MM/yyyy").format(Date(millis))

    fun time(millis: Long): String = formatter("HH:mm").format(Date(millis))

    fun today(): String = formatter("dd/MM/yyyy").format(Date())

    fun seconds(millis: Long): String = String.format(Locale.US, "%.1f s", millis / 1000f)
}

object Palette {

    fun forScene(type: SceneType): Color = when (type) {
        SceneType.CLEAN -> SR.Electric
        SceneType.BASE -> SR.Amber
        SceneType.SOLO -> Color(0xFFE07A5F)
    }

    fun forLevel(state: LevelState): Color = when (state) {
        LevelState.LOW -> SR.Electric
        LevelState.GOOD -> SR.Green
        LevelState.HIGH -> SR.Amber
        LevelState.CLIP -> SR.Red
    }

    fun forSeverity(severity: InsightSeverity): Color = when (severity) {
        InsightSeverity.OK -> SR.Green
        InsightSeverity.INFO -> SR.Electric
        InsightSeverity.WARN -> SR.Amber
        InsightSeverity.ALERT -> SR.Red
    }
}
