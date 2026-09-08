package com.srlakes.tone.sync

import com.srlakes.tone.model.ControlCommand
import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.StudioClip
import com.srlakes.tone.model.StudioSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Leitura da biblioteca do Studio por HTTP.
 *
 * Só GET, só leitura. O SR Lakes Tone nunca escreve nada no acervo da
 * banda — ele apenas precisa saber quais músicas existem e como a
 * timeline delas está desenhada.
 *
 * Sem biblioteca de rede: HttpURLConnection e o org.json que já vem com
 * o cliente Socket.IO dão conta de dois endpoints.
 */
class StudioApi(private val timeoutMs: Int = 5000) {

    suspend fun health(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { get(baseUrl + "/api/health") }.isSuccess
    }

    suspend fun songs(baseUrl: String): Result<List<StudioSong>> = withContext(Dispatchers.IO) {
        runCatching { parseSongs(get(baseUrl + "/api/songs")) }
    }

    /**
     * Transporte separado de leitura de proposito: assim da para testar
     * o entendimento do formato do Studio sem precisar de rede.
     */
    internal fun parseSongs(body: String): List<StudioSong> {
        val array = JSONArray(body)
        val out = ArrayList<StudioSong>(array.length())
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optInt("id", -1)
            if (id < 0) continue
            val tracks = item.optJSONObject("timeline")?.optJSONArray("tracks")
            out += StudioSong(
                id = id,
                title = item.optString("title", "").ifBlank { "Música " + id },
                artist = item.optString("artist", ""),
                key = item.optString("key", ""),
                bpm = item.optInt("bpm", 0),
                sections = clipsOfKind(tracks, SECTION_KIND, control = false),
                controls = clipsOfKind(tracks, CONTROL_KIND, control = true)
            )
        }
        return out
    }

    /**
     * Lê os blocos de uma trilha da timeline.
     *
     * A busca é pelo `kind`, exatamente como o resolvedor do próprio
     * Studio faz. É por isso que a trilha Controladora não precisa de
     * mecanismo novo lá: ela é só mais um `kind`.
     */
    private fun clipsOfKind(tracks: JSONArray?, kind: String, control: Boolean): List<StudioClip> {
        if (tracks == null) return emptyList()
        for (t in 0 until tracks.length()) {
            val track = tracks.optJSONObject(t) ?: continue
            if (track.optString("kind") != kind) continue
            val clips = track.optJSONArray("clips") ?: return emptyList()
            val out = ArrayList<StudioClip>(clips.length())
            for (c in 0 until clips.length()) {
                val clip = clips.optJSONObject(c) ?: continue
                val length = clip.optDouble("length", 0.0).toFloat()
                if (length <= 0f) continue
                val label = clip.optString("content", "")
                out += StudioClip(
                    startBar = clip.optDouble("start", 0.0).toFloat(),
                    lengthBars = length,
                    type = clip.optString("type", ""),
                    label = label,
                    control = if (control) parseControl(clip, label) else null
                )
            }
            // A ORDEM DO ARRAY E PRESERVADA DE PROPOSITO.
            //
            // O Studio resolve o bloco ativo com clips.find(...), que
            // devolve o PRIMEIRO da ordem do array. E blocos sobrepostos
            // existem no acervo real: em "SUA MANEIRA" um bloco vai do
            // compasso 2 ao 6.025 e outro comeca no 6 - os dois valem no
            // compasso 6. Ordenar por posicao faria o app aplicar uma
            // secao diferente da que esta escrita na tela do notebook.
            //
            // Concordar com o que o musico ve vale mais do que uma ordem
            // bonita.
            return out
        }
        return emptyList()
    }

    /**
     * O comando de um bloco da Controladora.
     *
     * Tolerante de propósito, porque o Studio ainda está sendo feito:
     * um bloco sem o objeto `control` vale como "vá para a cena escrita
     * no rótulo". Assim um bloco escrito só "SOLO" já funciona.
     */
    private fun parseControl(clip: JSONObject, label: String): ControlCommand? {
        val node = clip.optJSONObject("control")
            ?: return label.takeIf { it.isNotBlank() }?.let { ControlCommand(scene = it) }

        val effects = HashMap<EffectSlot, Boolean>(5)
        for ((key, slot) in EFFECT_KEYS) {
            if (node.has(key)) effects[slot] = node.optBoolean(key)
        }
        val command = ControlCommand(
            scene = node.optString("scene", "").ifBlank { label.ifBlank { null } },
            effects = effects,
            volume = if (node.has("volume")) node.optDouble("volume").toFloat() else null
        )
        return if (command.isEmpty) null else command
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("O Studio respondeu HTTP " + code + ".")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        /** A trilha de seções que o Studio já tem: `tr-section`. */
        const val SECTION_KIND = "section"

        /** A trilha Controladora, quando ela for criada no Studio. */
        const val CONTROL_KIND = "control"

        private val EFFECT_KEYS = listOf(
            "delay" to EffectSlot.DELAY,
            "reverb" to EffectSlot.REVERB,
            "boost" to EffectSlot.BOOST,
            "gate" to EffectSlot.GATE,
            "modulation" to EffectSlot.MODULATION,
            "mod" to EffectSlot.MODULATION
        )

        /** Aceita "192.168.0.10", "192.168.0.10:7575" ou a URL completa. */
        fun normalizeUrl(raw: String, defaultPort: Int = 7575): String? {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://" + trimmed
            }
            val hasPort = withScheme.substringAfter("://").contains(':')
            return if (hasPort) withScheme else withScheme + ":" + defaultPort
        }
    }
}
