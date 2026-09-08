package com.srlakes.tone.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.feature.analyzer.LabeledField
import com.srlakes.tone.model.SyncState
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.StatusDot
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format

/**
 * Ponte com o SR Lakes Studio, o aplicativo do notebook que conduz o show.
 *
 * O SR Lakes Tone entra na rede do Studio como RECEPTOR — o mesmo papel
 * dos celulares que mostram letra e cifra. Ele só escuta: nunca publica
 * estado, nunca escreve no acervo da banda.
 */
@Composable
fun StudioSyncPanel(viewModel: DevicesViewModel) {

    val sync = viewModel.stageSync
    val settings by sync.settings.collectAsStateWithLifecycle()
    val status by sync.status.collectAsStateWithLifecycle()
    val discovered by sync.discovered.collectAsStateWithLifecycle()
    val links by sync.links.collectAsStateWithLifecycle()
    val localSongs by sync.localSongs.collectAsStateWithLifecycle()
    val stage by sync.stage.collectAsStateWithLifecycle()
    val message by sync.message.collectAsStateWithLifecycle()

    var manualUrl by remember(settings.manualUrl) { mutableStateOf(settings.manualUrl.orEmpty()) }
    var editingStudioId by remember { mutableStateOf<Int?>(null) }

    val accent = when (status.state) {
        SyncState.CONNECTED -> SR.Green
        SyncState.ERROR -> SR.Red
        SyncState.OFF -> SR.Outline
        else -> SR.Electric
    }

    SectionLabel("SR Lakes Studio")
    Spacer(Modifier.height(6.dp))

    Panel(
        modifier = Modifier.fillMaxWidth(),
        accent = if (status.connected) SR.GreenDim else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SR.SurfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Cast, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Sincronia de palco",
                    color = SR.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(status.connected)
                    Spacer(Modifier.width(6.dp))
                    Text(status.state.label, color = accent, fontSize = 11.sp)
                    status.serverUrl?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it.removePrefix("http://"),
                            color = SR.TextTertiary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Trocou de música no notebook, o timbre desta música vem junto — " +
                "macros, equalização e efeitos (reverb, delay, boost, gate).",
            color = SR.TextTertiary,
            fontSize = 10.sp
        )

        status.lastError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = SR.Red, fontSize = 11.sp)
        }

        Spacer(Modifier.height(10.dp))
        StageButton(
            text = if (settings.enabled) "DESLIGAR SINCRONIA" else "LIGAR SINCRONIA",
            onClick = { sync.setEnabled(!settings.enabled) },
            selected = settings.enabled,
            accent = if (settings.enabled) SR.Red else SR.Green,
            modifier = Modifier.fillMaxWidth(),
            height = 46
        )

        if (settings.enabled) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StageButton(
                    text = "TROCA DE MÚSICA",
                    onClick = { sync.setFollowSong(!settings.followSong) },
                    selected = settings.followSong,
                    accent = SR.Green,
                    height = 40,
                    modifier = Modifier.weight(1f)
                )
                StageButton(
                    text = "SEÇÕES / CONTROLADORA",
                    onClick = { sync.setFollowSections(!settings.followSections) },
                    selected = settings.followSections,
                    accent = SR.Green,
                    height = 40,
                    modifier = Modifier.weight(1f)
                )
            }

            stage?.let { live ->
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SR.SurfaceSunken)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Studio: música " + live.studioSongId +
                            "  ·  compasso " + Format.oneDecimal(live.timelinePos),
                        color = SR.TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        if (live.playing) live.bpm.toString() + " BPM" else "parado",
                        color = if (live.playing) SR.Green else SR.TextTertiary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    "última atualização " + Format.time(live.receivedAtMs),
                    color = SR.TextTertiary,
                    fontSize = 9.sp
                )
            }

            // ---------- Descoberta ----------
            Spacer(Modifier.height(12.dp))
            SectionLabel("Notebooks encontrados na rede")
            Spacer(Modifier.height(4.dp))
            if (discovered.isEmpty()) {
                Text(
                    "Procurando... Se não aparecer, o Wi-Fi da casa de show pode estar " +
                        "isolando os aparelhos. Nesse caso digite o IP abaixo.",
                    color = SR.TextTertiary,
                    fontSize = 10.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    discovered.forEach { server ->
                        StageButton(
                            text = server.host,
                            onClick = { sync.connect(server.url) },
                            selected = status.serverUrl == server.url,
                            height = 36,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }

            // ---------- IP manual ----------
            Spacer(Modifier.height(10.dp))
            LabeledField("Endereço do notebook", manualUrl) { manualUrl = it }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StageButton(
                    text = "CONECTAR",
                    onClick = {
                        sync.setManualUrl(manualUrl)
                        sync.connect(manualUrl)
                    },
                    accent = SR.Electric,
                    selected = true,
                    height = 42,
                    modifier = Modifier.weight(1f)
                )
                StageButton(
                    text = "SINCRONIZAR MÚSICAS",
                    onClick = { sync.refreshLibrary() },
                    height = 42,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Aceita \"192.168.0.10\" ou a URL inteira. A porta padrão é 7575.",
                color = SR.TextTertiary,
                fontSize = 9.sp
            )

            // ---------- Associação de músicas ----------
            Spacer(Modifier.height(14.dp))
            SectionLabel(
                "Músicas associadas",
                trailing = {
                    Text(
                        links.count { it.localSong != null }.toString() + " / " + links.size,
                        color = SR.TextTertiary,
                        fontSize = 10.sp
                    )
                }
            )
            Spacer(Modifier.height(4.dp))
            if (links.isEmpty()) {
                Text(
                    "Toque em SINCRONIZAR MÚSICAS com o Studio conectado. O app casa os " +
                        "títulos sozinho e mostra aqui o que sobrou.",
                    color = SR.TextTertiary,
                    fontSize = 10.sp
                )
            }
            links.forEach { link ->
                val open = editingStudioId == link.studioSong.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SR.Surface)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editingStudioId = if (open) null else link.studioSong.id
                                }
                        ) {
                            Text(
                                link.studioSong.title,
                                color = SR.TextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = link.studioSong.sections.size.toString() + " seções" +
                                    (if (link.studioSong.controls.isEmpty()) {
                                        "  ·  sem trilha Controladora"
                                    } else {
                                        "  ·  " + link.studioSong.controls.size + " blocos de controle"
                                    }),
                                color = if (link.studioSong.controls.isEmpty()) SR.TextTertiary else SR.Electric,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                            Text(
                                text = link.localSong?.let { "→ " + it.title }
                                    ?: "não associada — toque para escolher",
                                color = if (link.localSong != null) SR.Green else SR.Amber,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        link.localSong?.let { local ->
                            IconButton(onClick = { sync.unlinkSong(local) }) {
                                Icon(
                                    Icons.Filled.LinkOff,
                                    "Desassociar",
                                    tint = SR.TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (open) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            localSongs.forEach { song ->
                                StageButton(
                                    text = song.title,
                                    onClick = {
                                        sync.linkSong(link.studioSong.id, song)
                                        editingStudioId = null
                                    },
                                    selected = link.localSong?.id == song.id,
                                    height = 34,
                                    modifier = Modifier.width(140.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        message?.let {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clickable { sync.clearMessage() }) {
                Text(it, color = SR.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
