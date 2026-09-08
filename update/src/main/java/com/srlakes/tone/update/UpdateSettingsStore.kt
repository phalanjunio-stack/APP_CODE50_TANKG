package com.srlakes.tone.update

import android.content.Context

/**
 * Preferências do atualizador, isoladas num SharedPreferences próprio.
 *
 * São só três campos - não vale puxar DataStore/Room para isto, e não
 * vale colocar em AppSettings (core:model): o atualizador não é um
 * conceito de guitarra ou de palco, é um utilitário de distribuição.
 * Mantê-lo autônomo é o que permite este módulo, um dia, virar uma
 * biblioteca reaproveitável fora deste projeto.
 */
class UpdateSettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("srlakes_update", Context.MODE_PRIVATE)

    fun read(): UpdateSettings = UpdateSettings(
        checkAutomatically = prefs.getBoolean(KEY_AUTO_CHECK, true),
        lastCheckedAtMs = prefs.getLong(KEY_LAST_CHECKED, 0L),
        skippedVersion = prefs.getString(KEY_SKIPPED, null)
    )

    fun setCheckAutomatically(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, value).apply()
    }

    fun markCheckedNow() {
        prefs.edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()
    }

    fun skip(versionName: String) {
        prefs.edit().putString(KEY_SKIPPED, versionName).apply()
    }

    private companion object {
        const val KEY_AUTO_CHECK = "check_automatically"
        const val KEY_LAST_CHECKED = "last_checked_at_ms"
        const val KEY_SKIPPED = "skipped_version"
    }
}
