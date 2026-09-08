package com.srlakes.tone.feature.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.model.AnalyzerSettings
import com.srlakes.tone.model.AudioSourceKind
import com.srlakes.tone.ui.components.MacroKnob
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.ValueRow
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val analyzer = settings.analyzer
    val setlists by viewModel.setlists.collectAsStateWithLifecycle()
    val inputs by viewModel.audioInputs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Configurações", color = SR.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(14.dp))
        SectionLabel("Tema")
        Spacer(Modifier.height(6.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Text(
                "O tema é sempre escuro. O aplicativo foi feito para viver preso a um " +
                    "pedestal, em palco, com pouca luz — um tema claro só atrapalharia.",
                color = SR.TextTertiary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                label = "Manter a tela ligada",
                on = settings.keepScreenOn,
                onToggle = { viewModel.setKeepScreenOn(!settings.keepScreenOn) }
            )
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Setlist ativo na tela Performance")
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StageButton(
                text = "TODAS AS MÚSICAS",
                onClick = { viewModel.setActiveSetlist(null) },
                selected = settings.activeSetlistId == null,
                height = 36,
                modifier = Modifier.width(150.dp)
            )
            setlists.forEach { setlist ->
                StageButton(
                    text = setlist.name,
                    onClick = { viewModel.setActiveSetlist(setlist.id) },
                    selected = settings.activeSetlistId == setlist.id,
                    height = 36,
                    modifier = Modifier.width(140.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Fonte de medição")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AudioSourceKind.entries.forEach { kind ->
                StageButton(
                    text = kind.label,
                    onClick = { viewModel.updateAnalyzer { it.copy(source = kind) } },
                    selected = analyzer.source == kind,
                    modifier = Modifier.weight(1f),
                    height = 42
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            ToggleRow(
                label = "Preferir captura UNPROCESSED",
                on = analyzer.preferUnprocessedMic,
                onToggle = {
                    viewModel.updateAnalyzer { it.copy(preferUnprocessedMic = !it.preferUnprocessedMic) }
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Desligar isto faz o Android aplicar ganho automático e redução de " +
                    "ruído, o que estraga qualquer medição de nível. Só desligue se a " +
                    "captura não abrir de jeito nenhum.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(10.dp))
            SectionLabel("Entradas detectadas")
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StageButton(
                    text = "AUTOMÁTICO",
                    onClick = { viewModel.updateAnalyzer { it.copy(preferredMicId = null, preferredUsbDeviceId = null) } },
                    selected = analyzer.preferredMicId == null && analyzer.preferredUsbDeviceId == null,
                    height = 34,
                    modifier = Modifier.width(120.dp)
                )
                inputs.forEach { input ->
                    val selected = if (input.isUsb) {
                        analyzer.preferredUsbDeviceId == input.id
                    } else {
                        analyzer.preferredMicId == input.id
                    }
                    StageButton(
                        text = input.name,
                        onClick = {
                            viewModel.updateAnalyzer {
                                if (input.isUsb) it.copy(preferredUsbDeviceId = input.id)
                                else it.copy(preferredMicId = input.id)
                            }
                        },
                        selected = selected,
                        height = 34,
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Medição")
        Spacer(Modifier.height(6.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroKnob(
                    label = "Ganho med.",
                    value = analyzer.inputGainDb,
                    valueRange = -24f..24f,
                    onValueChange = { v -> viewModel.updateAnalyzer { it.copy(inputGainDb = v) } },
                    modifier = Modifier.weight(1f)
                )
                MacroKnob(
                    label = "Sensib.",
                    value = analyzer.sensitivityDb,
                    valueRange = 3f..24f,
                    onValueChange = { v -> viewModel.updateAnalyzer { it.copy(sensitivityDb = v) } },
                    modifier = Modifier.weight(1f)
                )
                MacroKnob(
                    label = "Suavização",
                    value = analyzer.spectrumSmoothing * 10f,
                    valueRange = 0f..9.5f,
                    onValueChange = { v -> viewModel.updateAnalyzer { it.copy(spectrumSmoothing = v / 10f) } },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "O ganho de medição afeta APENAS a análise. Nada do que está aqui toca " +
                    "no som que vai para o amplificador.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            StepperRow(
                label = "Duração do A/B",
                value = analyzer.abCaptureSeconds.toString() + " s",
                onLess = { viewModel.updateAnalyzer { it.copy(abCaptureSeconds = (it.abCaptureSeconds - 1).coerceAtLeast(3)) } },
                onMore = { viewModel.updateAnalyzer { it.copy(abCaptureSeconds = (it.abCaptureSeconds + 1).coerceAtMost(30)) } }
            )
            StepperRow(
                label = "Duração do Base/Solo",
                value = analyzer.baseSoloCaptureSeconds.toString() + " s",
                onLess = { viewModel.updateAnalyzer { it.copy(baseSoloCaptureSeconds = (it.baseSoloCaptureSeconds - 1).coerceAtLeast(3)) } },
                onMore = { viewModel.updateAnalyzer { it.copy(baseSoloCaptureSeconds = (it.baseSoloCaptureSeconds + 1).coerceAtMost(30)) } }
            )
            StepperRow(
                label = "Média de espectro",
                value = analyzer.spectrumAveraging.toString() + " quadros",
                onLess = { viewModel.updateAnalyzer { it.copy(spectrumAveraging = (it.spectrumAveraging - 1).coerceAtLeast(1)) } },
                onMore = { viewModel.updateAnalyzer { it.copy(spectrumAveraging = (it.spectrumAveraging + 1).coerceAtMost(8)) } }
            )
            StepperRow(
                label = "Atualização do gráfico",
                value = analyzer.refreshRateFps.toString() + " fps",
                onLess = { viewModel.updateAnalyzer { it.copy(refreshRateFps = (it.refreshRateFps - 2).coerceAtLeast(8)) } },
                onMore = { viewModel.updateAnalyzer { it.copy(refreshRateFps = (it.refreshRateFps + 2).coerceAtMost(48)) } }
            )
        }

        Spacer(Modifier.height(10.dp))
        SectionLabel("Tamanho da FFT")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyzerSettings.FFT_SIZES.forEach { size ->
                StageButton(
                    text = size.toString(),
                    onClick = { viewModel.updateAnalyzer { it.copy(fftSize = size) } },
                    selected = analyzer.fftSize == size,
                    modifier = Modifier.weight(1f),
                    height = 40
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            ValueRow(
                "Resolução por bin",
                Format.hz(48000f / analyzer.fftSize),
                valueColor = SR.TextSecondary
            )
            Text(
                "FFT maior = mais detalhe nos graves, resposta um pouco mais lenta.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        UpdatePanel(viewModel)

        Spacer(Modifier.height(16.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Sobre")
            Spacer(Modifier.height(6.dp))
            Text(
                "SR Lakes Tone - versão " + viewModel.updateCoordinator.currentVersionName,
                color = SR.TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Análise 100% local: FFT, VU, pico, RMS, ruído, espectro, A/B e " +
                    "Base x Solo funcionam sem internet e sem IA. Nenhum dado sai do aparelho.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
internal fun ToggleRow(label: String, on: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = SR.TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        StageButton(
            text = if (on) "LIGADO" else "DESLIGADO",
            onClick = onToggle,
            selected = on,
            accent = if (on) SR.Green else SR.Outline,
            height = 34,
            modifier = Modifier.width(110.dp)
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onLess: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = SR.TextSecondary, fontSize = 13.sp)
            Text(value, color = SR.TextPrimary, fontSize = 12.sp)
        }
        StageButton("-", onLess, height = 34, modifier = Modifier.width(48.dp))
        Spacer(Modifier.width(6.dp))
        StageButton("+", onMore, height = 34, modifier = Modifier.width(48.dp))
    }
}
