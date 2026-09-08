package com.srlakes.tone.feature.devices

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.device.api.LogDirection
import com.srlakes.tone.model.DeviceInfo
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.StatusDot
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format

@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {

    val infos by viewModel.infos.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val audioInputs by viewModel.audioInputs.collectAsStateWithLifecycle()

    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Dispositivos", color = SR.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(12.dp))

        DeviceKind.entries.forEach { kind ->
            val info = infos.firstOrNull { it.kind == kind }
            val profile = profiles.firstOrNull { it.kind == kind }
            DeviceCard(
                kind = kind,
                info = info,
                mappedParams = profile?.mappings?.size ?: 0,
                learnedParams = profile?.mappings?.values?.count { it.learned } ?: 0,
                busy = busy == kind,
                onConnect = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        bluetoothLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                            )
                        )
                    }
                    viewModel.connect(kind)
                },
                onDisconnect = { viewModel.disconnect(kind) }
            )
            Spacer(Modifier.height(10.dp))
        }

        // ---------------- SR Lakes Studio ----------------
        StudioSyncPanel(viewModel)

        Spacer(Modifier.height(14.dp))

        // ---------------- Entrada de audio ----------------
        SectionLabel("Entrada de áudio")
        Spacer(Modifier.height(6.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Mic, null, tint = SR.TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (viewModel.unprocessedSupported) {
                        "Captura UNPROCESSED disponível"
                    } else {
                        "Sem captura UNPROCESSED neste aparelho"
                    },
                    color = if (viewModel.unprocessedSupported) SR.Green else SR.Amber,
                    fontSize = 12.sp
                )
            }
            if (!viewModel.unprocessedSupported) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "O Android vai aplicar ganho automático e redução de ruído. " +
                        "Use as diferenças entre medições, não os valores absolutos.",
                    color = SR.TextTertiary,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            audioInputs.forEach { input ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (input.isUsb) Icons.Filled.Usb else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = if (input.isUsb) SR.Electric else SR.TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(input.name, color = SR.TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(input.typeLabel, color = SR.TextTertiary, fontSize = 10.sp)
                }
            }
            if (audioInputs.none { it.isUsb }) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Nenhuma interface USB de áudio detectada. Com o cabo ocupado o " +
                        "celular também não carrega — para um show inteiro, use um hub OTG " +
                        "com alimentação.",
                    color = SR.TextTertiary,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            StageButton(
                text = "ATUALIZAR LISTA",
                onClick = { viewModel.refreshAudioInputs() },
                height = 40,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---------------- Protocol Lab ----------------
        SectionLabel("Protocol Lab")
        Spacer(Modifier.height(6.dp))
        Panel(modifier = Modifier.fillMaxWidth(), accent = SR.AmberDim) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Science, null, tint = SR.Amber, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Onde o protocolo real vai nascer", color = SR.Amber, fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "O aplicativo não inventa comandos proprietários. Nesta fase os dois " +
                    "aparelhos estão simulados: o log abaixo mostra o que TERIA sido " +
                    "enviado, o que já serve para conferir se as cenas disparam certo.",
                color = SR.TextSecondary,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Na fase 2 entram aqui o MIDI Learn (você mexe no knob do aparelho e o " +
                    "app aprende o CC) e o CC Sweep. Só mapeamentos aprendidos por você " +
                    "passam a valer de verdade.",
                color = SR.TextTertiary,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---------------- Log ----------------
        SectionLabel(
            "Log de comunicação",
            trailing = {
                Text(
                    "limpar",
                    color = SR.Electric,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { viewModel.clearLog() }
                )
            }
        )
        Spacer(Modifier.height(6.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            if (log.isEmpty()) {
                Text("Nada registrado ainda.", color = SR.TextTertiary, fontSize = 11.sp)
            } else {
                log.takeLast(40).reversed().forEach { entry ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            text = Format.time(entry.atMs),
                            color = SR.TextTertiary,
                            fontSize = 9.sp,
                            modifier = Modifier.width(42.dp)
                        )
                        Text(
                            text = when (entry.direction) {
                                LogDirection.IN -> "IN "
                                LogDirection.OUT -> "OUT"
                                LogDirection.INFO -> "..."
                                LogDirection.ERROR -> "ERR"
                            },
                            color = when (entry.direction) {
                                LogDirection.IN -> SR.Green
                                LogDirection.OUT -> SR.Electric
                                LogDirection.ERROR -> SR.Red
                                LogDirection.INFO -> SR.TextTertiary
                            },
                            fontSize = 9.sp,
                            modifier = Modifier.width(30.dp)
                        )
                        Text(entry.text, color = SR.TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(
    kind: DeviceKind,
    info: DeviceInfo?,
    mappedParams: Int,
    learnedParams: Int,
    busy: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val connected = info?.connected == true
    Panel(
        modifier = Modifier.fillMaxWidth(),
        accent = if (connected) SR.GreenDim else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SR.SurfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Bluetooth, null, tint = SR.TextSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(kind.displayName, color = SR.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(connected)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            busy -> "Conectando..."
                            connected -> "Conectado"
                            else -> "Desconectado"
                        },
                        color = if (connected) SR.Green else SR.TextTertiary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        info?.transport?.label ?: "--",
                        color = SR.TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
            info?.batteryPercent?.let { battery ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BatteryFull, null, tint = SR.Green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(battery.toString() + "%", color = SR.Green, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Parâmetros mapeados", color = SR.TextSecondary, fontSize = 11.sp)
            Text(
                text = mappedParams.toString() + " (" + learnedParams + " aprendidos)",
                color = if (learnedParams > 0) SR.Green else SR.Amber,
                fontSize = 11.sp
            )
        }
        if (learnedParams == 0) {
            Text(
                "Nenhum mapeamento verificado por você ainda — os valores atuais são " +
                    "de simulação e não representam o protocolo real.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
        }

        info?.lastError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = SR.Red, fontSize = 11.sp)
        }

        Spacer(Modifier.height(10.dp))
        StageButton(
            text = if (connected) "DESCONECTAR" else "CONECTAR",
            onClick = { if (connected) onDisconnect() else onConnect() },
            selected = connected,
            accent = if (connected) SR.Red else SR.Electric,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            height = 46
        )
    }
}
