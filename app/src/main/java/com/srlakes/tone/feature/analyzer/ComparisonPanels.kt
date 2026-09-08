package com.srlakes.tone.feature.analyzer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.analysis.LoudnessComparator
import com.srlakes.tone.model.LoudnessVerdict
import com.srlakes.tone.model.ToneBand
import com.srlakes.tone.model.ToneSnapshot
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.ValueRow
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format

/**
 * Comparacao A/B: dois retratos do mesmo trecho com regulagens diferentes.
 */
@Composable
fun AbComparePanel(viewModel: AnalyzerViewModel, enabled: Boolean) {

    val a by viewModel.snapshotA.collectAsStateWithLifecycle()
    val b by viewModel.snapshotB.collectAsStateWithLifecycle()
    val delta by viewModel.abDelta.collectAsStateWithLifecycle()
    val warning by viewModel.abWarning.collectAsStateWithLifecycle()

    SectionLabel("Comparação A / B")
    Spacer(Modifier.height(6.dp))

    Panel(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Toque o mesmo trecho duas vezes, mudando só o que quer comparar. " +
                "Não mova o celular entre as duas medições.",
            color = SR.TextTertiary,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StageButton(
                text = if (a == null) "SALVAR A" else "REGRAVAR A",
                onClick = { viewModel.captureA() },
                selected = a != null,
                accent = SR.Electric,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            StageButton(
                text = if (b == null) "SALVAR B" else "REGRAVAR B",
                onClick = { viewModel.captureB() },
                selected = b != null,
                accent = SR.Amber,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }

        if (a != null || b != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapshotColumn("PRESET A", a, SR.Electric, Modifier.weight(1f))
                SnapshotColumn("PRESET B", b, SR.Amber, Modifier.weight(1f))
            }
        }

        delta?.let { d ->
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SR.SurfaceSunken)
                    .padding(10.dp)
            ) {
                Column {
                    Text("DIFERENÇA (B - A)", color = SR.TextTertiary, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(6.dp))
                    ValueRow("RMS", Format.delta(d.rmsDb), valueColor = deltaColor(d.rmsDb))
                    ValueRow("Pico", Format.delta(d.peakDb), valueColor = deltaColor(d.peakDb))
                    Spacer(Modifier.height(6.dp))
                    ToneBand.ordered.forEachIndexed { index, band ->
                        ValueRow(
                            band.label,
                            Format.delta(d.bandDb.getOrElse(index) { 0f }),
                            valueColor = deltaColor(d.bandDb.getOrElse(index) { 0f })
                        )
                    }
                }
            }
        }

        warning?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = SR.Amber, fontSize = 11.sp)
        }

        if (a != null && b != null) {
            Spacer(Modifier.height(12.dp))
            SaveToHistory { song, context, note -> viewModel.saveAbSession(song, context, note) }
            Spacer(Modifier.height(8.dp))
            StageButton(
                text = "LIMPAR A / B",
                onClick = { viewModel.clearAb() },
                modifier = Modifier.fillMaxWidth(),
                height = 40
            )
        }
    }
}

/**
 * BASE x SOLO: a pergunta pratica de todo guitarrista antes do show.
 */
@Composable
fun BaseSoloPanel(viewModel: AnalyzerViewModel, enabled: Boolean) {

    val base by viewModel.baseSnapshot.collectAsStateWithLifecycle()
    val solo by viewModel.soloSnapshot.collectAsStateWithLifecycle()
    val result by viewModel.baseSoloResult.collectAsStateWithLifecycle()
    val warning by viewModel.baseSoloWarning.collectAsStateWithLifecycle()

    SectionLabel("Base x Solo")
    Spacer(Modifier.height(6.dp))

    Panel(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Grave a base, depois o solo. O solo costuma pedir de " +
                Format.oneDecimal(LoudnessComparator.MIN_HEALTHY_DELTA) + " a " +
                Format.oneDecimal(LoudnessComparator.MAX_HEALTHY_DELTA) + " dB acima da base.",
            color = SR.TextTertiary,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StageButton(
                text = if (base == null) "GRAVAR BASE" else "REGRAVAR BASE",
                onClick = { viewModel.captureBase() },
                selected = base != null,
                accent = SR.Amber,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            StageButton(
                text = if (solo == null) "GRAVAR SOLO" else "REGRAVAR SOLO",
                onClick = { viewModel.captureSolo() },
                selected = solo != null,
                accent = Color(0xFFE07A5F),
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }

        if (base != null || solo != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SnapshotColumn("BASE", base, SR.Amber, Modifier.weight(1f))
                SnapshotColumn("SOLO", solo, Color(0xFFE07A5F), Modifier.weight(1f))
            }
        }

        result?.let { r ->
            Spacer(Modifier.height(12.dp))
            val verdictColor = when (r.verdict) {
                LoudnessVerdict.BALANCED -> SR.Green
                LoudnessVerdict.SOLO_TOO_LOW -> SR.Electric
                LoudnessVerdict.SOLO_TOO_LOUD -> SR.Red
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(verdictColor.copy(alpha = 0.10f))
                    .border(1.dp, verdictColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("DIFERENÇA", color = SR.TextTertiary, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Text(
                        text = Format.delta(r.deltaRmsDb),
                        color = verdictColor,
                        fontSize = 30.sp
                    )
                    Text(r.verdict.label, color = verdictColor, fontSize = 13.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pico: " + Format.delta(r.deltaPeakDb),
                        color = SR.TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ajuste você mesmo o volume do solo no pedal ou no amp. " +
                    "O aplicativo não muda nada sozinho.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
        }

        warning?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = SR.Amber, fontSize = 11.sp)
        }

        if (base != null && solo != null) {
            Spacer(Modifier.height(12.dp))
            SaveToHistory { song, context, note -> viewModel.saveBaseSoloSession(song, context, note) }
            Spacer(Modifier.height(8.dp))
            StageButton(
                text = "LIMPAR BASE / SOLO",
                onClick = { viewModel.clearBaseSolo() },
                modifier = Modifier.fillMaxWidth(),
                height = 40
            )
        }
    }
}

@Composable
private fun SnapshotColumn(
    title: String,
    snapshot: ToneSnapshot?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SR.Surface)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(title, color = accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(6.dp))
        if (snapshot == null) {
            Text("--", color = SR.TextTertiary, fontSize = 13.sp)
        } else {
            ValueRow("RMS", Format.db(snapshot.rmsDb))
            ValueRow("Pico", Format.db(snapshot.peakDb))
            ValueRow("Ruído", Format.db(snapshot.noiseFloorDb))
            ValueRow("Crest", Format.oneDecimal(snapshot.crestFactorDb))
            Spacer(Modifier.height(4.dp))
            Text(
                Format.seconds(snapshot.durationMs) + " - " + Format.time(snapshot.capturedAtMs),
                color = SR.TextTertiary,
                fontSize = 9.sp
            )
            if (snapshot.clipped) {
                Text("clipping na captura", color = SR.Red, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SaveToHistory(onSave: (String, String, String) -> Unit) {
    var song by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("Ensaio") }
    var note by remember { mutableStateOf("") }

    Column {
        SectionLabel("Salvar no histórico")
        Spacer(Modifier.height(6.dp))
        LabeledField("Música", song) { song = it }
        Spacer(Modifier.height(6.dp))
        LabeledField("Contexto", context) { context = it }
        Spacer(Modifier.height(6.dp))
        LabeledField("Observação", note) { note = it }
        Spacer(Modifier.height(8.dp))
        StageButton(
            text = "SALVAR MEDIÇÃO",
            onClick = { onSave(song, context, note) },
            accent = SR.Green,
            selected = true,
            modifier = Modifier.fillMaxWidth(),
            height = 44
        )
    }
}

/**
 * Campo de texto proprio, para nao herdar o visual padrao do Material.
 */
@Composable
internal fun LabeledField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(label.uppercase(), color = SR.TextTertiary, fontSize = 9.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SR.SurfaceSunken)
                .border(1.dp, SR.Outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = SR.TextPrimary, fontSize = 13.sp),
                cursorBrush = SolidColor(SR.Electric),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun deltaColor(value: Float): Color = when {
    value > 1.5f -> SR.Amber
    value < -1.5f -> SR.Electric
    else -> SR.TextPrimary
}
