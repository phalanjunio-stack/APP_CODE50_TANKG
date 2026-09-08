package com.srlakes.tone.feature.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.model.AmpParam
import com.srlakes.tone.model.DeviceKind
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.Macro
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.ui.components.EffectToggle
import com.srlakes.tone.ui.components.MacroKnob
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SceneButton
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.StatusPill
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Palette

@Composable
fun PerformanceScreen(viewModel: PerformanceViewModel) {

    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val nextSong by viewModel.nextSong.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val scene by viewModel.activeScene.collectAsStateWithLifecycle()
    val activeSectionId by viewModel.activeSectionId.collectAsStateWithLifecycle()
    val macros by viewModel.macros.collectAsStateWithLifecycle()
    val amp by viewModel.amp.collectAsStateWithLifecycle()
    val effects by viewModel.effects.collectAsStateWithLifecycle()
    val devices by viewModel.deviceInfos.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val setlistName by viewModel.setlistName.collectAsStateWithLifecycle()
    val followState by viewModel.followState.collectAsStateWithLifecycle()
    val studioNowPlaying by viewModel.studioNowPlaying.collectAsStateWithLifecycle()

    var advancedOpen by remember { mutableStateOf(false) }

    val sceneAccent = scene?.let { Palette.forScene(it.sceneType) } ?: SR.Electric

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 20.dp)
    ) {
        BrandHeader(setlistName)

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val tank = devices.firstOrNull { it.kind == DeviceKind.TANK_G }
            val code = devices.firstOrNull { it.kind == DeviceKind.CODE50 }
            StatusPill(
                label = "TANK-G",
                value = if (tank?.connected == true) "Conectado" else "Desconectado",
                connected = tank?.connected == true,
                modifier = Modifier.weight(1f)
            )
            StatusPill(
                label = "CODE50",
                value = if (code?.connected == true) "Conectado" else "Desconectado",
                connected = code?.connected == true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))
        StudioFollowBar(
            followState = followState,
            nowPlaying = studioNowPlaying,
            onToggle = { viewModel.setFollowEnabled(it) },
            onResume = { viewModel.resumeFollowing() }
        )

        Spacer(Modifier.height(14.dp))
        ModeHeader("PERFORMANCE MODE", "TOCAR - CONTROLAR - SENTIR")
        Spacer(Modifier.height(12.dp))

        // ---------------- Musica atual ----------------
        Panel(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Música atual")
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.goToPreviousSong() }) {
                    Icon(Icons.Filled.ChevronLeft, "Música anterior", tint = SR.TextSecondary)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song?.title ?: "Nenhuma música",
                        color = SR.TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = song?.let {
                        listOfNotNull(
                            it.artist.takeIf { a -> a.isNotBlank() },
                            it.key.takeIf { k -> k.isNotBlank() }?.let { k -> "Tom " + k },
                            it.bpm.toString() + " BPM"
                        ).joinToString("  ·  ")
                    }
                    if (meta != null) {
                        Text(meta, color = SR.TextTertiary, fontSize = 11.sp, maxLines = 1)
                    }
                }
                IconButton(onClick = { viewModel.goToNextSong() }) {
                    Icon(Icons.Filled.ChevronRight, "Próxima música", tint = SR.TextSecondary)
                }
            }
            if (nextSong != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Próxima: " + nextSong!!.title,
                    color = SR.TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------------- Cena atual ----------------
        Text(
            text = "CENA ATUAL",
            color = SR.TextTertiary,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = scene?.name ?: "--",
            color = sceneAccent,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // ---------------- Tres botoes gigantes ----------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SceneTypeButton(SceneType.CLEAN, Icons.Filled.Waves, scene?.sceneType, viewModel, Modifier.weight(1f))
            SceneTypeButton(SceneType.BASE, Icons.Filled.Speaker, scene?.sceneType, viewModel, Modifier.weight(1f))
            SceneTypeButton(SceneType.SOLO, Icons.Filled.MusicNote, scene?.sceneType, viewModel, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Secoes da musica ----------------
        SectionLabel("Seção da música")
        Spacer(Modifier.height(6.dp))
        val sections = detail?.sections.orEmpty()
        if (sections.isEmpty()) {
            Text(
                "Esta música ainda não tem seções. Cadastre em Músicas.",
                color = SR.TextTertiary,
                fontSize = 12.sp
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sections.forEach { section ->
                    val target = detail?.sceneFor(section)
                    StageButton(
                        text = section.label,
                        onClick = { viewModel.selectSection(section) },
                        selected = section.id == activeSectionId,
                        accent = target?.let { Palette.forScene(it.sceneType) } ?: SR.Electric,
                        height = 40,
                        modifier = Modifier.width(96.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Macros ----------------
        SectionLabel(
            "Macros de timbre",
            trailing = {
                Text(
                    text = if (advancedOpen) "Fechar avançado" else "Avançado",
                    color = SR.Electric,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { advancedOpen = !advancedOpen }
                )
            }
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Macro.ordered.forEach { macro ->
                MacroKnob(
                    label = macro.label,
                    value = macros.get(macro),
                    onValueChange = { viewModel.setMacro(macro, it) },
                    accent = if (macro == Macro.DRIVE || macro == Macro.VOLUME) SR.Amber else SR.Electric,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (advancedOpen) {
            Spacer(Modifier.height(12.dp))
            Panel(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Parâmetros individuais")
                Text(
                    "Mexer aqui reposiciona os macros por aproximação.",
                    color = SR.TextTertiary,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(10.dp))
                AmpParam.ordered.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { param ->
                            MacroKnob(
                                label = param.label,
                                value = amp.get(param),
                                onValueChange = { viewModel.setAmpParams(amp.with(param, it)) },
                                accent = SR.Electric,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Efeitos rapidos ----------------
        SectionLabel("Efeitos rápidos")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            effectIcons.forEach { (slot, icon) ->
                EffectToggle(
                    label = slot.label,
                    icon = icon,
                    on = effects.isOn(slot),
                    accent = if (slot == EffectSlot.BOOST) SR.Amber else SR.Electric,
                    onToggle = { viewModel.toggleEffect(slot) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StageButton(
                text = "SALVAR NA CENA",
                onClick = { viewModel.storeCurrentIntoScenePreset() },
                accent = SR.Electric,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SR.RedDim.copy(alpha = 0.35f))
                    .border(1.dp, SR.Red.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .clickable { viewModel.panic() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = SR.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PANIC", color = SR.Red, fontSize = 14.sp, letterSpacing = 1.sp)
                }
            }
        }

        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Panel(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.clearMessage() },
                accent = SR.OutlineStrong
            ) {
                Text(message!!, color = SR.TextSecondary, fontSize = 12.sp)
                Text("Toque para dispensar", color = SR.TextTertiary, fontSize = 9.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "O aplicativo nunca troca a cena sozinho. Quem decide é você.",
            color = SR.TextTertiary,
            fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val effectIcons: List<Pair<EffectSlot, ImageVector>> = listOf(
    EffectSlot.DELAY to Icons.Filled.Repeat,
    EffectSlot.REVERB to Icons.Filled.Waves,
    EffectSlot.BOOST to Icons.Filled.Bolt,
    EffectSlot.GATE to Icons.Filled.Block,
    EffectSlot.MODULATION to Icons.Filled.Vibration
)

@Composable
private fun SceneTypeButton(
    type: SceneType,
    icon: ImageVector,
    current: SceneType?,
    viewModel: PerformanceViewModel,
    modifier: Modifier = Modifier
) {
    SceneButton(
        label = type.label,
        icon = icon,
        selected = current == type,
        accent = Palette.forScene(type),
        onClick = { viewModel.selectSceneType(type) },
        modifier = modifier
    )
}

@Composable
private fun BrandHeader(setlistName: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SR Lakes Tone",
                color = SR.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
            Text(
                text = "MUSIC LIVES LOUDER",
                color = SR.TextTertiary,
                fontSize = 8.sp,
                letterSpacing = 2.5.sp
            )
        }
        if (setlistName != null) {
            Column(horizontalAlignment = Alignment.End) {
                Text("SETLIST", color = SR.TextTertiary, fontSize = 8.sp, letterSpacing = 2.sp)
                Text(setlistName, color = SR.Electric, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ModeHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Divider(Modifier.weight(1f))
            Text(
                text = title,
                color = SR.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Divider(Modifier.weight(1f))
        }
        Text(subtitle, color = SR.TextTertiary, fontSize = 8.sp, letterSpacing = 3.sp)
    }
}

@Composable
private fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).background(SR.Outline))
}
