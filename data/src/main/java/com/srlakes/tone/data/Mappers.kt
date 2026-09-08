package com.srlakes.tone.data

import com.srlakes.tone.data.db.AnalysisSessionEntity
import com.srlakes.tone.data.db.AnalysisSnapshotEntity
import com.srlakes.tone.data.db.PresetEntity
import com.srlakes.tone.data.db.SceneEntity
import com.srlakes.tone.data.db.SetlistEntity
import com.srlakes.tone.data.db.SongEntity
import com.srlakes.tone.data.db.SongSectionEntity
import com.srlakes.tone.model.AmpParams
import com.srlakes.tone.model.AnalysisSession
import com.srlakes.tone.model.AnalysisSnapshotRecord
import com.srlakes.tone.model.EffectState
import com.srlakes.tone.model.MacroSet
import com.srlakes.tone.model.Preset
import com.srlakes.tone.model.PresetCategory
import com.srlakes.tone.model.Scene
import com.srlakes.tone.model.SceneType
import com.srlakes.tone.model.SectionType
import com.srlakes.tone.model.Setlist
import com.srlakes.tone.model.SnapshotRole
import com.srlakes.tone.model.Song
import com.srlakes.tone.model.SongSection
import com.srlakes.tone.model.ToneSnapshot

internal fun SongEntity.toModel() = Song(
    id = id,
    title = title,
    artist = artist,
    key = musicalKey,
    bpm = bpm,
    timeSignature = timeSignature,
    notes = notes,
    studioSongId = studioSongId
)

internal fun Song.toEntity(position: Int = 0) = SongEntity(
    id = id,
    title = title,
    artist = artist,
    musicalKey = key,
    bpm = bpm,
    timeSignature = timeSignature,
    notes = notes,
    position = position,
    studioSongId = studioSongId
)

internal fun SceneEntity.toModel() = Scene(
    id = id,
    songId = songId,
    name = name,
    sceneType = enumOrDefault(sceneType, SceneType.BASE),
    presetId = presetId,
    position = position
)

internal fun Scene.toEntity() = SceneEntity(
    id = id,
    songId = songId,
    name = name,
    sceneType = sceneType.name,
    presetId = presetId,
    position = position
)

internal fun SongSectionEntity.toModel() = SongSection(
    id = id,
    songId = songId,
    type = enumOrDefault(type, SectionType.VERSO),
    customLabel = customLabel,
    sceneId = sceneId,
    bars = bars,
    position = position
)

internal fun SongSection.toEntity() = SongSectionEntity(
    id = id,
    songId = songId,
    type = type.name,
    customLabel = customLabel,
    sceneId = sceneId,
    bars = bars,
    position = position
)

internal fun PresetEntity.toModel() = Preset(
    id = id,
    name = name,
    category = enumOrDefault(category, PresetCategory.CUSTOM),
    macros = MacroSet(macroDrive, macroWarmth, macroBody, macroPresence, macroVolume),
    amp = AmpParams(ampGain, ampBass, ampMiddle, ampTreble, ampPresence, ampResonance, ampVolume),
    effects = EffectState(fxDelay, fxReverb, fxBoost, fxGate, fxModulation),
    tankGProgram = tankGProgram,
    codeProgram = codeProgram,
    notes = notes,
    builtIn = builtIn
)

internal fun Preset.toEntity() = PresetEntity(
    id = id,
    name = name,
    category = category.name,
    macroDrive = macros.drive,
    macroWarmth = macros.warmth,
    macroBody = macros.body,
    macroPresence = macros.presence,
    macroVolume = macros.volume,
    ampGain = amp.gain,
    ampBass = amp.bass,
    ampMiddle = amp.middle,
    ampTreble = amp.treble,
    ampPresence = amp.presence,
    ampResonance = amp.resonance,
    ampVolume = amp.volume,
    fxDelay = effects.delay,
    fxReverb = effects.reverb,
    fxBoost = effects.boost,
    fxGate = effects.gate,
    fxModulation = effects.modulation,
    tankGProgram = tankGProgram,
    codeProgram = codeProgram,
    notes = notes,
    builtIn = builtIn
)

internal fun SetlistEntity.toModel() = Setlist(id = id, name = name, dateLabel = dateLabel)

internal fun Setlist.toEntity() = SetlistEntity(id = id, name = name, dateLabel = dateLabel)

internal fun AnalysisSessionEntity.toModel() = AnalysisSession(
    id = id,
    songTitle = songTitle,
    context = context,
    dateLabel = dateLabel,
    createdAtMs = createdAtMs,
    note = note
)

internal fun AnalysisSession.toEntity() = AnalysisSessionEntity(
    id = id,
    songTitle = songTitle,
    context = context,
    dateLabel = dateLabel,
    createdAtMs = createdAtMs,
    note = note
)

internal fun AnalysisSnapshotEntity.toModel() = AnalysisSnapshotRecord(
    id = id,
    sessionId = sessionId,
    role = enumOrDefault(role, SnapshotRole.SINGLE),
    snapshot = ToneSnapshot(
        label = label,
        capturedAtMs = capturedAtMs,
        durationMs = durationMs,
        rmsDb = rmsDb,
        peakDb = peakDb,
        noiseFloorDb = noiseFloorDb,
        crestFactorDb = crestFactorDb,
        bandDb = bandDb,
        bandShare = bandShare,
        averageSpectrum = averageSpectrum,
        clipped = clipped
    )
)

internal fun AnalysisSnapshotRecord.toEntity(pcmPath: String? = null) = AnalysisSnapshotEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    label = snapshot.label,
    capturedAtMs = snapshot.capturedAtMs,
    durationMs = snapshot.durationMs,
    rmsDb = snapshot.rmsDb,
    peakDb = snapshot.peakDb,
    noiseFloorDb = snapshot.noiseFloorDb,
    crestFactorDb = snapshot.crestFactorDb,
    bandDb = snapshot.bandDb,
    bandShare = snapshot.bandShare,
    averageSpectrum = snapshot.averageSpectrum,
    clipped = snapshot.clipped,
    pcmPath = pcmPath
)

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback
