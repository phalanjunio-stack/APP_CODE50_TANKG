package com.srlakes.tone.model

/**
 * Cena de timbre. O musico escolhe manualmente - nada aqui e automatico.
 */
enum class SceneType(val label: String) {
    CLEAN("CLEAN"),
    BASE("BASE"),
    SOLO("SOLO")
}

/** Secoes tipicas de uma musica. */
enum class SectionType(val label: String) {
    INTRO("INTRO"),
    VERSO("VERSO"),
    PRE_REFRAO("PRE-REFRAO"),
    REFRAO("REFRAO"),
    PONTE("PONTE"),
    SOLO("SOLO"),
    OUTRO("OUTRO");

    companion object {
        /**
         * Traduz o campo `type` de um bloco da timeline do SR Lakes Studio.
         * O vocabulario de la e: intro, verso, prerefrao, refrao, ponte,
         * solo, final. Aqui "final" vira OUTRO.
         *
         * Devolve null para um tipo desconhecido - e melhor nao mudar o
         * timbre do que mudar para a coisa errada no meio do show.
         */
        fun fromStudioType(raw: String?): SectionType? = when (raw?.lowercase()?.trim()) {
            "intro", "introducao" -> INTRO
            "verso" -> VERSO
            "prerefrao", "pre-refrao", "pre_refrao" -> PRE_REFRAO
            "refrao" -> REFRAO
            "ponte" -> PONTE
            "solo" -> SOLO
            "final", "outro" -> OUTRO
            else -> null
        }
    }
}

enum class PresetCategory(val label: String) {
    CLEAN("Clean"),
    CRUNCH("Crunch"),
    BASE("Base"),
    SOLO("Solo"),
    HIGH_GAIN("High Gain"),
    ACOUSTIC("Acoustic"),
    CUSTOM("Custom")
}

enum class EffectSlot(val label: String) {
    DELAY("Delay"),
    REVERB("Reverb"),
    BOOST("Boost"),
    GATE("Gate"),
    MODULATION("Modulation")
}

enum class DeviceKind(val displayName: String) {
    TANK_G("M-VAVE TANK-G"),
    CODE50("Marshall CODE50")
}

enum class ConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Como o transporte real e feito. O app nao assume BLE para todo mundo:
 * o CODE50 provavelmente usa Bluetooth Classic e/ou USB MIDI.
 */
enum class TransportKind(val label: String) {
    MOCK("Simulado"),
    BLE_MIDI("BLE MIDI"),
    USB_MIDI("USB MIDI"),
    BT_CLASSIC("Bluetooth Classic"),
    BLE_CUSTOM("BLE proprietario")
}

enum class AudioSourceKind(val label: String) {
    AMP_MIC("AMP MIC"),
    USB_DIRECT("USB DIRECT")
}

/** Estado visual de uma banda de frequencia ou medidor. */
enum class LevelState {
    LOW,
    GOOD,
    HIGH,
    CLIP
}
