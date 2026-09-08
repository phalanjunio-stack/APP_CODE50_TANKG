package com.srlakes.tone.feature.presets

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.srlakes.tone.feature.analyzer.LabeledField
import com.srlakes.tone.model.AmpParam
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.Macro
import com.srlakes.tone.model.PresetCategory
import com.srlakes.tone.ui.components.MacroKnob
import com.srlakes.tone.ui.components.Panel
import com.srlakes.tone.ui.components.SectionLabel
import com.srlakes.tone.ui.components.StageButton
import com.srlakes.tone.ui.theme.SR

@Composable
fun PresetsScreen(viewModel: PresetsViewModel) {

    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf<PresetCategory?>(null) }
    var newName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf(PresetCategory.CUSTOM) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SR.ScreenPadding)
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text("Presets", color = SR.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text(
            "Um preset pode apontar para um patch do TANK-G, do CODE50, ou dos dois.",
            color = SR.TextTertiary,
            fontSize = 10.sp
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StageButton("TODOS", { filter = null }, selected = filter == null, height = 34, modifier = Modifier.width(92.dp))
            PresetCategory.entries.forEach { category ->
                StageButton(
                    text = category.label,
                    onClick = { filter = category },
                    selected = filter == category,
                    height = 34,
                    modifier = Modifier.width(92.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        presets.filter { filter == null || it.category == filter }.forEach { preset ->
            val isOpen = editing?.id == preset.id
            Panel(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                accent = if (isOpen) SR.Electric else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.open(if (isOpen) null else preset.id) }
                    ) {
                        Text(preset.name, color = SR.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = preset.category.label +
                                (if (preset.builtIn) "  ·  de fábrica" else "") +
                                "  ·  Drive " + preset.macros.drive.toInt() +
                                "  ·  Vol " + preset.macros.volume.toInt(),
                            color = SR.TextTertiary,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = { viewModel.audition(preset) }) {
                        Icon(Icons.Filled.PlayArrow, "Enviar aos aparelhos", tint = SR.Electric, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.duplicate(preset) }) {
                        Icon(Icons.Filled.ContentCopy, "Duplicar", tint = SR.TextTertiary, modifier = Modifier.size(18.dp))
                    }
                    if (!preset.builtIn) {
                        IconButton(onClick = { viewModel.delete(preset) }) {
                            Icon(Icons.Filled.Delete, "Apagar", tint = SR.TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (isOpen) {
                    Spacer(Modifier.height(12.dp))
                    LabeledField("Nome", preset.name) { viewModel.update(preset.copy(name = it)) }

                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Categoria")
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetCategory.entries.forEach { category ->
                            StageButton(
                                text = category.label,
                                onClick = { viewModel.update(preset.copy(category = category)) },
                                selected = preset.category == category,
                                height = 32,
                                modifier = Modifier.width(88.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Macros")
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Macro.ordered.forEach { macro ->
                            MacroKnob(
                                label = macro.label,
                                value = preset.macros.get(macro),
                                onValueChange = { viewModel.setMacro(preset, macro, it) },
                                accent = if (macro == Macro.DRIVE || macro == Macro.VOLUME) SR.Amber else SR.Electric,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Parâmetros individuais")
                    Spacer(Modifier.height(6.dp))
                    AmpParam.ordered.chunked(4).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { param ->
                                MacroKnob(
                                    label = param.label,
                                    value = preset.amp.get(param),
                                    onValueChange = { viewModel.setAmp(preset, preset.amp.with(param, it)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }

                    SectionLabel("Efeitos")
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EffectSlot.entries.forEach { slot ->
                            StageButton(
                                text = slot.label,
                                onClick = { viewModel.toggleEffect(preset, slot) },
                                selected = preset.effects.isOn(slot),
                                accent = if (slot == EffectSlot.BOOST) SR.Amber else SR.Electric,
                                height = 36,
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Patches dos aparelhos")
                    Text(
                        "Preencha só depois de saber a que patch cada número corresponde " +
                            "no seu aparelho. Em branco = o preset não troca de patch.",
                        color = SR.TextTertiary,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabeledField(
                            "TANK-G",
                            preset.tankGProgram?.toString().orEmpty(),
                            Modifier.weight(1f)
                        ) { text ->
                            viewModel.update(preset.copy(tankGProgram = text.toIntOrNull()))
                        }
                        LabeledField(
                            "CODE50",
                            preset.codeProgram?.toString().orEmpty(),
                            Modifier.weight(1f)
                        ) { text ->
                            viewModel.update(preset.copy(codeProgram = text.toIntOrNull()))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    LabeledField("Observações", preset.notes) { viewModel.update(preset.copy(notes = it)) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Panel(modifier = Modifier.fillMaxWidth()) {
            SectionLabel("Novo preset")
            Spacer(Modifier.height(6.dp))
            LabeledField("Nome", newName) { newName = it }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetCategory.entries.forEach { category ->
                    StageButton(
                        text = category.label,
                        onClick = { newCategory = category },
                        selected = newCategory == category,
                        height = 32,
                        modifier = Modifier.width(88.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            StageButton(
                text = "CRIAR PRESET",
                onClick = {
                    viewModel.create(newName, newCategory)
                    newName = ""
                },
                accent = SR.Green,
                selected = true,
                modifier = Modifier.fillMaxWidth(),
                height = 44
            )
        }

        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().clickable { viewModel.clearMessage() }) {
                Text(message!!, color = SR.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
