package com.srlakes.tone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srlakes.tone.analysis.Db
import com.srlakes.tone.model.AudioAnalysis
import com.srlakes.tone.model.LevelState
import com.srlakes.tone.model.ToneBand
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Palette
import kotlin.math.log10

/**
 * Analisador de espectro.
 *
 * Recebe o vetor ja mapeado em escala logaritmica pelo :core:analysis,
 * entao aqui so ha desenho: nada de matematica pesada dentro do Compose.
 */
@Composable
fun SpectrumView(
    analysis: AudioAnalysis,
    modifier: Modifier = Modifier,
    minDb: Float = -66f,
    maxDb: Float = 0f
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SR.SurfaceSunken)
            .border(1.dp, SR.Outline, RoundedCornerShape(10.dp))
    ) {
        val chartWidth = maxWidth
        val spectrum = analysis.spectrum

        Canvas(modifier = Modifier.fillMaxSize().padding(start = 30.dp, end = 6.dp, top = 6.dp, bottom = 16.dp)) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f || spectrum.isEmpty()) return@Canvas

            // Grade horizontal (dB)
            for (db in intArrayOf(0, -20, -40, -60)) {
                val y = h * (1f - Db.normalize(db.toFloat(), minDb, maxDb))
                drawLine(
                    color = SR.Outline.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
            }

            // Grade vertical (Hz) nas divisas das faixas
            for (band in ToneBand.ordered) {
                val x = w * logFraction(band.highHz)
                if (x in 1f..(w - 1f)) {
                    drawLine(
                        color = SR.Outline.copy(alpha = 0.8f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                }
            }

            // Curva do espectro
            val path = Path()
            val fill = Path()
            fill.moveTo(0f, h)
            for (i in spectrum.indices) {
                val x = w * i / (spectrum.size - 1).coerceAtLeast(1)
                val y = h * (1f - Db.normalize(spectrum[i], minDb, maxDb))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                fill.lineTo(x, y)
            }
            fill.lineTo(w, h)
            fill.close()

            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(SR.Electric.copy(alpha = 0.55f), SR.Electric.copy(alpha = 0.04f))
                )
            )
            drawPath(
                path = path,
                color = if (analysis.clipping) SR.Red else SR.Electric,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Marcador da frequencia dominante
            if (analysis.dominantFrequency > 20f) {
                val x = w * logFraction(analysis.dominantFrequency)
                drawLine(
                    color = SR.Amber,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1.5f
                )
            }
        }

        // Escala vertical em dB
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .padding(top = 2.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0", "-20", "-40", "-60").forEach {
                Text(it, color = SR.TextTertiary, fontSize = 8.sp)
            }
        }

        // Escala horizontal em Hz, posicionada em escala logaritmica
        val labels = listOf(20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)
        val plotWidth = chartWidth - 36.dp
        labels.forEach { hz ->
            Text(
                text = hzLabel(hz),
                color = SR.TextTertiary,
                fontSize = 8.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 28.dp + plotWidth * logFraction(hz) - 6.dp, y = (-2).dp)
            )
        }
    }
}

/** Cabecalho com as cinco faixas, com largura proporcional a escala log. */
@Composable
fun BandHeader(
    analysis: AudioAnalysis,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        ToneBand.ordered.forEach { band ->
            val weight = (log10(band.highHz / band.lowHz))
            val state = analysis.bands.state(band)
            Column(
                modifier = Modifier
                    .weight(weight)
                    .padding(horizontal = 1.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = band.label,
                    color = if (analysis.hasSignal) Palette.forLevel(state) else SR.TextTertiary,
                    fontSize = 9.sp,
                    maxLines = 1
                )
                Text(
                    text = band.rangeLabel,
                    color = SR.TextTertiary,
                    fontSize = 7.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Medidor vertical segmentado. Verde ate -12 dB, ambar ate -3, vermelho
 * acima disso - a mesma convencao de uma mesa de som.
 */
@Composable
fun VuMeter(
    label: String,
    valueDb: Float,
    modifier: Modifier = Modifier,
    minDb: Float = -60f,
    maxDb: Float = 0f,
    segments: Int = 18,
    tint: Color? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .width(18.dp)
                .height(90.dp)
        ) {
            val gap = size.height * 0.012f
            val segHeight = (size.height - gap * (segments - 1)) / segments
            val fraction = Db.normalize(valueDb, minDb, maxDb)
            val lit = (fraction * segments).toInt()
            for (i in 0 until segments) {
                val fromTop = segments - 1 - i
                val y = fromTop * (segHeight + gap)
                val segDb = minDb + (maxDb - minDb) * (i + 0.5f) / segments
                val color = when {
                    i >= lit -> SR.Outline.copy(alpha = 0.55f)
                    tint != null -> tint
                    segDb >= -3f -> SR.Red
                    segDb >= -12f -> SR.Amber
                    else -> SR.Green
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(size.width, segHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = SR.TextTertiary, fontSize = 9.sp, maxLines = 1)
    }
}

/** Barra horizontal de uma faixa, com o estado LOW / GOOD / HIGH. */
@Composable
fun BandBar(
    band: ToneBand,
    shareValue: Float,
    state: LevelState,
    modifier: Modifier = Modifier
) {
    val color = Palette.forLevel(state)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(band.label, color = SR.TextSecondary, fontSize = 10.sp)
            Text(
                text = when (state) {
                    LevelState.LOW -> "BAIXO"
                    LevelState.GOOD -> "OK"
                    LevelState.HIGH -> "ALTO"
                    LevelState.CLIP -> "CLIP"
                },
                color = color,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SR.SurfaceSunken)
        ) {
            Box(
                Modifier
                    .fillMaxWidth((shareValue * 2.2f).coerceIn(0f, 1f))
                    .fillMaxSize()
                    .background(color)
            )
        }
    }
}

private fun logFraction(hz: Float): Float {
    val min = log10(AudioAnalysis.DISPLAY_MIN_HZ)
    val max = log10(AudioAnalysis.DISPLAY_MAX_HZ)
    return ((log10(hz.coerceIn(AudioAnalysis.DISPLAY_MIN_HZ, AudioAnalysis.DISPLAY_MAX_HZ)) - min) / (max - min))
        .coerceIn(0f, 1f)
}

private fun hzLabel(hz: Float): String = when {
    hz >= 1000f -> (hz / 1000f).toInt().toString() + "k"
    else -> hz.toInt().toString()
}
