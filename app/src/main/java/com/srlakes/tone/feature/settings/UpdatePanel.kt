package com.srlakes.tone.feature.settings

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.ThinProgress
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.update.UpdateInstaller
import com.srlakes.tone.update.UpdateState

/**
 * Verificação de novas versões.
 *
 * O app não está em loja nenhuma — é publicado direto como GitHub
 * Release. Este painel é o que evita "gerar apk, mandar por WhatsApp e
 * cada um instalar na mão": o próprio app verifica, baixa, e entrega o
 * arquivo pronto para o instalador do Android.
 *
 * A instalação em si é sempre confirmada pela tela do sistema — o app
 * nunca instala nada sozinho.
 */
@Composable
fun UpdatePanel(viewModel: SettingsViewModel) {

    val context = LocalContext.current
    val coordinator = viewModel.updateCoordinator
    val state by viewModel.updateState.collectAsStateWithLifecycle()
    val settings = coordinator.settings

    SectionLabel("Atualizações")
    Spacer(Modifier.height(6.dp))

    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SystemUpdate, null, tint = SR.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Versão instalada", color = SR.TextPrimary, fontSize = 14.sp)
                Text(
                    coordinator.currentVersionName,
                    color = SR.TextTertiary,
                    fontSize = 11.sp
                )
            }
            if (state is UpdateState.Checking) {
                Text("verificando...", color = SR.Electric, fontSize = 11.sp)
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { viewModel.checkForUpdate() }
                        .padding(6.dp)
                ) {
                    Icon(Icons.Filled.Refresh, "Verificar agora", tint = SR.Electric, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ToggleRow(
            label = "Verificar automaticamente",
            on = settings.checkAutomatically,
            onToggle = { viewModel.setCheckUpdatesAutomatically(!settings.checkAutomatically) }
        )

        when (val current = state) {
            is UpdateState.UpToDate -> {
                Spacer(Modifier.height(8.dp))
                Text("Você está na versão mais recente.", color = SR.Green, fontSize = 11.sp)
            }

            is UpdateState.Checking -> Unit

            is UpdateState.Available -> {
                Spacer(Modifier.height(10.dp))
                UpdateAvailableCard(current.info.versionName, current.info.releaseNotes)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StageButton(
                        text = "BAIXAR E INSTALAR",
                        onClick = { viewModel.downloadUpdate() },
                        accent = SR.Amber,
                        selected = true,
                        modifier = Modifier.weight(1f),
                        height = 44
                    )
                    StageButton(
                        text = "AGORA NÃO",
                        onClick = { viewModel.skipUpdate(current.info.versionName) },
                        height = 44,
                        modifier = Modifier.width(110.dp)
                    )
                }
            }

            is UpdateState.Downloading -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Baixando " + current.info.versionName + "...",
                    color = SR.Amber,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(6.dp))
                ThinProgress(current.fraction, accent = SR.Amber)
                Spacer(Modifier.height(4.dp))
                Text(
                    megabytes(current.bytesRead) + " de " + megabytes(current.totalBytes),
                    color = SR.TextTertiary,
                    fontSize = 10.sp
                )
            }

            is UpdateState.ReadyToInstall -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Versão " + current.info.versionName + " pronta para instalar.",
                    color = SR.Green,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                if (!coordinator.canRequestInstall()) {
                    Text(
                        "Antes, autorize este app a instalar aplicativos: " +
                            "\"Instalar apps desconhecidos\" > SR Lakes Tone > Permitir.",
                        color = SR.Amber,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    StageButton(
                        text = "ABRIR PERMISSÃO",
                        onClick = { context.startActivity(UpdateInstaller.requestInstallPermissionIntent(context)) },
                        accent = SR.Amber,
                        selected = true,
                        modifier = Modifier.fillMaxWidth(),
                        height = 44
                    )
                } else {
                    StageButton(
                        text = "INSTALAR AGORA",
                        onClick = {
                            val intent = UpdateInstaller.installIntent(context, java.io.File(current.apkPath))
                            context.startActivity(intent)
                        },
                        accent = SR.Green,
                        selected = true,
                        modifier = Modifier.fillMaxWidth(),
                        height = 46
                    )
                }
                Text(
                    "O Android vai pedir sua confirmação antes de instalar — o app " +
                        "nunca instala nada sozinho.",
                    color = SR.TextTertiary,
                    fontSize = 9.sp
                )
            }

            is UpdateState.Error -> {
                Spacer(Modifier.height(10.dp))
                Text(current.message, color = SR.Red, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                StageButton(
                    text = "DISPENSAR",
                    onClick = { viewModel.dismissUpdateError() },
                    height = 38,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableCard(versionName: String, releaseNotes: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SR.Amber.copy(alpha = 0.10f))
            .padding(10.dp)
    ) {
        Text("VERSÃO " + versionName + " DISPONÍVEL", color = SR.Amber, fontSize = 12.sp)
        if (releaseNotes.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(releaseNotes, color = SR.TextSecondary, fontSize = 11.sp)
        }
    }
}

private fun megabytes(bytes: Long): String =
    if (bytes <= 0L) "--" else String.format(java.util.Locale.US, "%.1f MB", bytes / 1024f / 1024f)
