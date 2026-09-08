package com.srlakes.tone.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Le a ultima release publicada no GitHub do projeto.
 *
 * Só GET, endpoint público (`releases/latest`) — não precisa de token
 * para um repositório aberto. Se o repositório virar privado um dia,
 * um cabeçalho `Authorization` entra aqui, e só aqui.
 */
class GithubReleaseChecker(
    private val source: UpdateSource,
    private val timeoutMs: Int = 8000
) {

    suspend fun latest(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching { parse(get(source.apiUrl)) }
    }

    /** Parser separado da rede de propósito: testável sem HTTP nenhum. */
    internal fun parse(body: String): UpdateInfo {
        val root = JSONObject(body)
        val tag = root.optString("tag_name", "")
        val assets = root.optJSONArray("assets") ?: JSONArray()
        val apk = findApkAsset(assets)
            ?: throw IllegalStateException("A release " + tag + " não tem nenhum arquivo .apk anexado.")

        return UpdateInfo(
            versionName = tag.removePrefix("v").removePrefix("V"),
            tagName = tag,
            releaseNotes = root.optString("body", "").trim(),
            downloadUrl = apk.optString("browser_download_url", ""),
            assetName = apk.optString("name", "app-release.apk"),
            assetSizeBytes = apk.optLong("size", 0L),
            publishedAtIso = root.optString("published_at", "")
        )
    }

    /**
     * Quando há mais de um .apk anexado (por exemplo debug e release),
     * prefere o que tem "release" no nome; senão, o primeiro que achar.
     */
    private fun findApkAsset(assets: JSONArray): JSONObject? {
        var fallback: JSONObject? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name", "")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            if (fallback == null) fallback = asset
            if (name.contains("release", ignoreCase = true)) return asset
        }
        return fallback
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = timeoutMs
        connection.readTimeout = timeoutMs
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            val code = connection.responseCode
            if (code == 404) {
                throw IllegalStateException("Nenhuma release publicada ainda neste repositório.")
            }
            if (code !in 200..299) {
                throw IllegalStateException("O GitHub respondeu HTTP " + code + ".")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
