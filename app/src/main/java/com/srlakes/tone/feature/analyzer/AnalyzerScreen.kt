package com.srlakes.tone.feature.analyzer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.model.AudioSourceKind
import com.srlakes.tone.model.ToneBand
import com.srlakes.tone.ui.components.BandBar
import com.srlakes.tone.ui.components.BandHeader
import com.srlakes.tone.ui.components.MetricTile
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.SpectrumView
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.ThinProgress
import com.srlakes.tone.ui.components.VuMeter
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format
import com.srlakes.tone.ui.util.Palette

@Composable
fun AnalyzerScreen(viewModel: AnalyzerViewModel) {

    val context = LocalContext.current
    val analysis by viewModel.analysis.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val capture by viewModel.captureProgress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(hasPermission) {
        if (hasPermission) viewModel.start()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.stop() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Analisador de Tone",
            color = SR.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "MEDIR - ENTENDER - AJUSTAR VOCÊ MESMO",
            color = SR.TextTertiary,
            fontSize = 8.sp,
            letterSpacing = 2.5.sp
        )

        Spacer(Modifier.height(12.dp))

        // ---------------- Fonte de audio ----------------
        SectionLabel("Fonte de áudio")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceButton(
                label = "AMP MIC",
                selected = settings.source == AudioSourceKind.AMP_MIC,
                onClick = { viewModel.setSource(AudioSourceKind.AMP_MIC) },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Filled.Mic, null, tint = SR.TextSecondary, modifier = Modifier.size(16.dp)) }
            )
            SourceButton(
                label = "USB DIRECT",
                selected = settings.source == AudioSourceKind.USB_DIRECT,
                onClick = { viewModel.setSource(AudioSourceKind.USB_DIRECT) },
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Filled.Usb, null, tint = SR.TextSecondary, modifier = Modifier.size(16.dp)) }
            )
        }

        Spacer(Modifier.height(10.dp))

        // ---------------- Contrato do app com o musico ----------------
        Panel(modifier = Modifier.fillMaxWidth(), accent = SR.ElectricDim) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Tune, null, tint = SR.Electric, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Você ajusta manualmente", color = SR.TextPrimary, fontSize = 14.sp)
                    Text(
                        "Somente análise. O aplicativo não altera nada sozinho.",
                        color = SR.TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ---------------- Avisos de confiabilidade ----------------
        if (!hasPermission) {
            Spacer(Modifier.height(10.dp))
            WarningCard(
                title = "Permissão de microfone",
                text = "O analisador precisa do microfone para medir o som do amplificador.",
                actionLabel = "PERMITIR",
                onAction = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
            )
        }

        status.error?.let { error ->
            Spacer(Modifier.height(10.dp))
            WarningCard(title = "Entrada de áudio", text = error)
        }

        if (status.running && !status.trustworthy) {
            Spacer(Modifier.height(10.dp))
            WarningCard(
                title = "Medição não calibrada",
                text = status.descriptor?.note
                    ?: "O Android está processando o microfone. Use as diferenças, não os valores absolutos.",
                severe = false
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---------------- Niveis em tempo real ----------------
        SectionLabel(
            "Níveis em tempo real",
            trailing = {
                Text(
                    text = if (status.running) status.sampleRate.toString() + " Hz" else "parado",
                    color = if (status.running) SR.Green else SR.TextTertiary,
                    fontSize = 10.sp
                )
            }
        )
        Spacer(Modifier.height(8.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VuMeter("INPUT", analysis.rmsDb)
                VuMeter("PEAK", analysis.peakDb)
                VuMeter("RMS", analysis.rmsDb, tint = SR.Electric)
                VuMeter("NOISE", analysis.noiseFloorDb, tint = SR.TextTertiary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricTile(
                    "Pico",
                    Format.db(analysis.peakDb),
                    Modifier.weight(1f),
                    valueColor = if (analysis.clipping) SR.Red else SR.Amber
                )
                MetricTile("RMS", Format.db(analysis.rmsDb), Modifier.weight(1f), valueColor = SR.Electric)
                MetricTile("Ruído", Format.db(analysis.noiseFloorDb), Modifier.weight(1f))
                MetricTile("Crest", Format.oneDecimal(analysis.crestFactorDb), Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetricTile(
                    "Faixa dinâmica",
                    Format.oneDecimal(analysis.dynamicRangeDb) + " dB",
                    Modifier.weight(1f)
                )
                MetricTile("Dominante", Format.hz(analysis.dominantFrequency), Modifier.weight(1f))
                MetricTile(
                    "Clipping",
                    if (analysis.clipping) "SIM" else "não",
                    Modifier.weight(1f),
                    valueColor = if (analysis.clipping) SR.Red else SR.Green
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---------------- Espectro ----------------
        SectionLabel("Espectro de frequência")
        Spacer(Modifier.height(6.dp))
        BandHeader(analysis)
        Spacer(Modifier.height(4.dp))
        SpectrumView(
            analysis = analysis,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        )

        Spacer(Modifier.height(12.dp))

        Panel(modifier = Modifier.fillMaxWidth()) {
            ToneBand.ordered.forEachIndexed { index, band ->
                BandBar(
                    band = band,
                    shareValue = analysis.bands.share(band),
                    state = analysis.bands.state(band),
                    modifier = Modifier.fillMaxWidth()
                )
                if (index < ToneBand.ordered.size - 1) Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---------------- Tone Insights ----------------
        SectionLabel("Tone insights")
        Spacer(Modifier.height(6.dp))
        if (insights.isEmpty()) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Toque algumas notas com o celular apontado para o amplificador.",
                    color = SR.TextTertiary,
                    fontSize = 12.sp
                )
            }
        } else {
            insights.forEach { insight ->
                val color = Palette.forSeverity(insight.severity)
                Panel(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    accent = color.copy(alpha = 0.45f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(width = 3.dp, height = 26.dp)
                                .background(color)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(insight.title, color = color, fontSize = 13.sp)
                            Text(insight.message, color = SR.TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Progresso de captura ----------------
        capture?.let { progress ->
            Panel(modifier = Modifier.fillMaxWidth(), accent = SR.Amber) {
                Text(
                    "Gravando " + progress.label + " - toque agora",
                    color = SR.Amber,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                ThinProgress(progress.fraction, accent = SR.Amber)
                Spacer(Modifier.height(4.dp))
                Text(
                    progress.remainingSeconds.toString() + " s restantes",
                    color = SR.TextTertiary,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        AbComparePanel(viewModel, enabled = status.running && capture == null)

        Spacer(Modifier.height(14.dp))

        BaseSoloPanel(viewModel, enabled = status.running && capture == null)

        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Panel(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.clearMessage() }
            ) {
                Text(message!!, color = SR.TextSecondary, fontSize = 12.sp)
                Text("Toque para dispensar", color = SR.TextTertiary, fontSize = 9.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Os valores são dBFS, não dB SPL. O microfone do celular não é " +
                "calibrado, então só as DIFERENÇAS entre medições feitas na mesma " +
                "posição têm significado.",
            color = SR.TextTertiary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SourceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SR.Electric.copy(alpha = 0.12f) else SR.Surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) SR.Electric else SR.Outline,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) SR.Electric else SR.TextSecondary,
            fontSize = 13.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
internal fun WarningCard(
    title: String,
    text: String,
    severe: Boolean = true,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val accent = if (severe) SR.Red else SR.Amber
    Panel(modifier = Modifier.fillMaxWidth(), accent = accent.copy(alpha = 0.5f)) {
        Text(title, color = accent, fontSize = 13.sp)
        Spacer(Modifier.height(3.dp))
        Text(text, color = SR.TextSecondary, fontSize = 11.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(10.dp))
            StageButton(
                text = actionLabel,
                onClick = onAction,
                accent = accent,
                selected = true,
                height = 42,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
