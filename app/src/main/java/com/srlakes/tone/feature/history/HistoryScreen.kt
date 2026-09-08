package com.srlakes.tone.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.data.AnalysisRepository
import com.srlakes.tone.model.AnalysisSession
import com.srlakes.tone.model.AnalysisSnapshotRecord
import com.srlakes.tone.model.SnapshotRole
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.components.ValueRow
import com.srlakes.tone.ui.theme.SR
import com.srlakes.tone.ui.util.Format
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: AnalysisRepository
) : ViewModel() {

    val sessions: StateFlow<List<AnalysisSession>> = repository.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val openSessionId = MutableStateFlow<Long?>(null)
    val opened: StateFlow<Long?> = openSessionId

    val snapshots: StateFlow<List<AnalysisSnapshotRecord>> = openSessionId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeSnapshots(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(id: Long) {
        openSessionId.value = if (openSessionId.value == id) null else id
    }

    fun updateNote(session: AnalysisSession, note: String) {
        viewModelScope.launch { repository.updateNote(session, note) }
    }

    fun delete(session: AnalysisSession) {
        viewModelScope.launch { repository.deleteSession(session) }
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {

    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val opened by viewModel.opened.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()

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
                "Histórico",
                color = SR.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            StageButton("VOLTAR", onBack, height = 38, modifier = Modifier.width(96.dp))
        }

        Spacer(Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Nenhuma medição salva ainda. Faça um A/B ou um BASE x SOLO no " +
                        "analisador e salve para comparar entre ensaios.",
                    color = SR.TextTertiary,
                    fontSize = 12.sp
                )
            }
        }

        sessions.forEach { session ->
            val isOpen = opened == session.id
            Panel(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                accent = if (isOpen) SR.Electric else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.toggle(session.id) }
                    ) {
                        Text(session.songTitle, color = SR.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            session.context + "  ·  " + session.dateLabel + "  ·  " + Format.time(session.createdAtMs),
                            color = SR.TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { viewModel.delete(session) }) {
                        Icon(Icons.Filled.Delete, "Apagar", tint = SR.TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }

                if (session.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(session.note, color = SR.TextSecondary, fontSize = 11.sp)
                }

                if (isOpen) {
                    Spacer(Modifier.height(10.dp))
                    val byRole = snapshots.associateBy { it.role }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(SnapshotRole.BASE, SnapshotRole.SOLO, SnapshotRole.A, SnapshotRole.B)
                            .mapNotNull { byRole[it] }
                            .forEach { record ->
                                Column(modifier = Modifier.weight(1f)) {
                                    SectionLabel(record.role.name, color = SR.Electric)
                                    Spacer(Modifier.height(4.dp))
                                    ValueRow("RMS", Format.db(record.snapshot.rmsDb))
                                    ValueRow("Pico", Format.db(record.snapshot.peakDb))
                                    ValueRow("Ruído", Format.db(record.snapshot.noiseFloorDb))
                                }
                            }
                    }

                    val base = byRole[SnapshotRole.BASE]?.snapshot
                    val solo = byRole[SnapshotRole.SOLO]?.snapshot
                    if (base != null && solo != null) {
                        Spacer(Modifier.height(8.dp))
                        ValueRow(
                            "Diferença SOLO - BASE",
                            Format.delta(solo.rmsDb - base.rmsDb),
                            valueColor = SR.Amber
                        )
                    }

                    val a = byRole[SnapshotRole.A]?.snapshot
                    val b = byRole[SnapshotRole.B]?.snapshot
                    if (a != null && b != null) {
                        Spacer(Modifier.height(8.dp))
                        ValueRow(
                            "Diferença B - A",
                            Format.delta(b.rmsDb - a.rmsDb),
                            valueColor = SR.Amber
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    NoteEditor(session.note) { viewModel.updateNote(session, it) }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(initial: String, onSave: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    Column {
        com.srlakes.tone.feature.analyzer.LabeledField("Observação", text) { text = it }
        Spacer(Modifier.height(6.dp))
        StageButton(
            text = "SALVAR OBSERVAÇÃO",
            onClick = { onSave(text) },
            accent = SR.Green,
            height = 40,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
