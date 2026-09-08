package com.srlakes.tone.feature.songs

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.feature.analyzer.LabeledField
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.model.SectionType
import com.srlakes.tone.model.Song
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Palette

@Composable
fun SongsScreen(
    viewModel: SongsViewModel,
    onOpenSong: (Long) -> Unit,
    onPlaySong: (Song) -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val setlists by viewModel.setlists.collectAsStateWithLifecycle()
    val activeSetlistId by viewModel.activeSetlistId.collectAsStateWithLifecycle()
    val editingSetlist by viewModel.editingSetlist.collectAsStateWithLifecycle()

    var newSongTitle by remember { mutableStateOf("") }
    var newSetlistName by remember { mutableStateOf("") }
    var newSetlistDate by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Biblioteca", color = SR.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StageButton("MÚSICAS", { tab = 0 }, selected = tab == 0, modifier = Modifier.weight(1f), height = 42)
            StageButton("SETLISTS", { tab = 1 }, selected = tab == 1, modifier = Modifier.weight(1f), height = 42)
        }

        Spacer(Modifier.height(14.dp))

        if (tab == 0) {
            songs.forEach { song ->
                Panel(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenSong(song.id) }
                        ) {
                            Text(song.title, color = SR.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = listOfNotNull(
                                    song.artist.takeIf { it.isNotBlank() },
                                    song.key.takeIf { it.isNotBlank() }?.let { "Tom " + it },
                                    song.bpm.toString() + " BPM",
                                    song.timeSignature
                                ).joinToString("  ·  "),
                                color = SR.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { onPlaySong(song) }) {
                            Icon(Icons.Filled.PlayArrow, "Tocar", tint = SR.Electric)
                        }
                        IconButton(onClick = { viewModel.deleteSong(song) }) {
                            Icon(Icons.Filled.Delete, "Apagar", tint = SR.TextTertiary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Panel(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Nova música")
                Spacer(Modifier.height(6.dp))
                LabeledField("Título", newSongTitle) { newSongTitle = it }
                Spacer(Modifier.height(8.dp))
                StageButton(
                    text = "CRIAR COM CENAS PADRÃO",
                    onClick = {
                        viewModel.createSong(newSongTitle)
                        newSongTitle = ""
                    },
                    accent = SR.Green,
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    height = 44
                )
            }
        } else {
            setlists.forEach { setlist ->
                val active = setlist.id == activeSetlistId
                Panel(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    accent = if (active) SR.Electric else null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.openSetlist(setlist.id) }
                        ) {
                            Text(setlist.name, color = SR.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (setlist.dateLabel.isBlank()) "Toque para editar" else setlist.dateLabel,
                                color = SR.TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        StageButton(
                            text = if (active) "ATIVA" else "ATIVAR",
                            onClick = { viewModel.setActiveSetlist(if (active) null else setlist.id) },
                            selected = active,
                            accent = SR.Electric,
                            height = 36,
                            modifier = Modifier.width(88.dp)
                        )
                        IconButton(onClick = { viewModel.deleteSetlist(setlist) }) {
                            Icon(Icons.Filled.Delete, "Apagar", tint = SR.TextTertiary)
                        }
                    }

                    if (editingSetlist?.setlist?.id == setlist.id) {
                        Spacer(Modifier.height(10.dp))
                        SectionLabel("Ordem do show")
                        Spacer(Modifier.height(6.dp))
                        editingSetlist!!.songs.forEachIndexed { index, song ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    (index + 1).toString(),
                                    color = SR.TextTertiary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(song.title, color = SR.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.moveSongInSetlist(setlist.id, song.id, -1) }) {
                                    Icon(Icons.Filled.ArrowUpward, "Subir", tint = SR.TextTertiary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.moveSongInSetlist(setlist.id, song.id, 1) }) {
                                    Icon(Icons.Filled.ArrowDownward, "Descer", tint = SR.TextTertiary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.removeSongFromSetlist(setlist.id, song.id) }) {
                                    Icon(Icons.Filled.Delete, "Remover", tint = SR.TextTertiary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        SectionLabel("Adicionar")
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            songs.filterNot { s -> editingSetlist!!.songs.any { it.id == s.id } }
                                .forEach { song ->
                                    StageButton(
                                        text = song.title,
                                        onClick = { viewModel.addSongToSetlist(setlist.id, song.id) },
                                        height = 36,
                                        modifier = Modifier.width(130.dp)
                                    )
                                }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Panel(modifier = Modifier.fillMaxWidth()) {
                SectionLabel("Novo setlist")
                Spacer(Modifier.height(6.dp))
                LabeledField("Nome", newSetlistName) { newSetlistName = it }
                Spacer(Modifier.height(6.dp))
                LabeledField("Data", newSetlistDate) { newSetlistDate = it }
                Spacer(Modifier.height(8.dp))
                StageButton(
                    text = "CRIAR SETLIST",
                    onClick = {
                        viewModel.createSetlist(newSetlistName, newSetlistDate)
                        newSetlistName = ""
                        newSetlistDate = ""
                    },
                    accent = SR.Green,
                    selected = true,
                    modifier = Modifier.fillMaxWidth(),
                    height = 44
                )
            }
        }
    }
}

@Composable
fun SongEditorScreen(viewModel: SongsViewModel, songId: Long, onBack: () -> Unit) {

    androidx.compose.runtime.LaunchedEffect(songId) { viewModel.openSong(songId) }

    val detail by viewModel.editingSong.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()

    val song = detail?.song

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = song?.title ?: "Música",
                color = SR.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            StageButton("VOLTAR", onBack, height = 38, modifier = Modifier.width(96.dp))
        }

        if (song == null) {
            Spacer(Modifier.height(20.dp))
            Text("Música não encontrada.", color = SR.TextTertiary, fontSize = 13.sp)
            return@Column
        }

        Spacer(Modifier.height(12.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Dados")
            Spacer(Modifier.height(8.dp))
            LabeledField("Título", song.title) { viewModel.updateSong(song.copy(title = it)) }
            Spacer(Modifier.height(6.dp))
            LabeledField("Artista", song.artist) { viewModel.updateSong(song.copy(artist = it)) }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledField("Tom", song.key, Modifier.weight(1f)) {
                    viewModel.updateSong(song.copy(key = it))
                }
                LabeledField("BPM", song.bpm.toString(), Modifier.weight(1f)) { text ->
                    text.toIntOrNull()?.let { viewModel.updateSong(song.copy(bpm = it)) }
                }
                LabeledField("Compasso", song.timeSignature, Modifier.weight(1f)) {
                    viewModel.updateSong(song.copy(timeSignature = it))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionLabel("Cenas")
        Spacer(Modifier.height(6.dp))
        detail!!.scenes.forEach { scene ->
            val accent = Palette.forScene(scene.sceneType)
            Panel(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), accent = accent.copy(alpha = 0.4f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(scene.sceneType.label, color = accent, fontSize = 11.sp, letterSpacing = 1.5.sp, modifier = Modifier.width(60.dp))
                    Box(Modifier.weight(1f)) {
                        LabeledField("Nome da cena", scene.name) { viewModel.updateScene(scene.copy(name = it)) }
                    }
                    IconButton(onClick = { viewModel.deleteScene(scene) }) {
                        Icon(Icons.Filled.Delete, "Apagar cena", tint = SR.TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                SectionLabel("Preset")
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        StageButton(
                            text = preset.name,
                            onClick = { viewModel.updateScene(scene.copy(presetId = preset.id)) },
                            selected = scene.presetId == preset.id,
                            accent = accent,
                            height = 34,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SceneType.entries.forEach { type ->
                StageButton(
                    text = "+ " + type.label,
                    onClick = { viewModel.addScene(song.id, type) },
                    accent = Palette.forScene(type),
                    height = 36,
                    modifier = Modifier.width(110.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Seções")
        Spacer(Modifier.height(6.dp))
        detail!!.sections.forEach { section ->
            Panel(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(section.label, color = SR.TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(section.bars.toString() + " compassos", color = SR.TextTertiary, fontSize = 10.sp)
                    IconButton(onClick = { viewModel.deleteSection(section) }) {
                        Icon(Icons.Filled.Delete, "Apagar seção", tint = SR.TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    detail!!.scenes.forEach { scene ->
                        StageButton(
                            text = scene.name,
                            onClick = { viewModel.updateSection(section.copy(sceneId = scene.id)) },
                            selected = section.sceneId == scene.id,
                            accent = Palette.forScene(scene.sceneType),
                            height = 34,
                            modifier = Modifier.width(130.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SectionType.entries.forEach { type ->
                StageButton(
                    text = "+ " + type.label,
                    onClick = { viewModel.addSection(song.id, type) },
                    height = 36,
                    modifier = Modifier.width(110.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, null, tint = SR.TextTertiary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Cada seção carrega a cena escolhida quando você toca nela, na tela Performance.",
                color = SR.TextTertiary,
                fontSize = 10.sp
            )
        }
    }
}
