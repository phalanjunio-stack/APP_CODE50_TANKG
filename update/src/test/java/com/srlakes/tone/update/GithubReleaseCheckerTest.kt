package com.srlakes.tone.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Entendimento do formato real de `GET /repos/{owner}/{repo}/releases/latest`
 * do GitHub. Sem rede: o corpo abaixo é a forma documentada da API.
 */
class GithubReleaseCheckerTest {

    private val checker = GithubReleaseChecker(UpdateSource("phalanjunio-stack", "APP_CODE50_TANKG"))

    private val typicalResponse = """
    {
      "tag_name": "v1.0.0",
      "name": "SR Lakes Tone 1.0.0",
      "body": "- Sincronia com o SR Lakes Studio\n- Trilha Controladora",
      "published_at": "2026-09-08T12:00:00Z",
      "assets": [
        {
          "name": "app-release.apk",
          "browser_download_url": "https://github.com/phalanjunio-stack/APP_CODE50_TANKG/releases/download/v1.0.0/app-release.apk",
          "size": 12345678
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `le a versao e o link direto do apk`() {
        val info = checker.parse(typicalResponse)
        assertEquals("1.0.0", info.versionName)
        assertEquals("v1.0.0", info.tagName)
        assertEquals("app-release.apk", info.assetName)
        assertEquals(12345678L, info.assetSizeBytes)
        assertEquals(
            "https://github.com/phalanjunio-stack/APP_CODE50_TANKG/releases/download/v1.0.0/app-release.apk",
            info.downloadUrl
        )
        assertEquals("- Sincronia com o SR Lakes Studio\n- Trilha Controladora", info.releaseNotes)
    }

    @Test
    fun `com varios apks prefere o que tem release no nome`() {
        val comDebug = """
        {
          "tag_name": "v1.2.0",
          "assets": [
            { "name": "app-debug.apk", "browser_download_url": "https://x/app-debug.apk", "size": 1 },
            { "name": "app-release.apk", "browser_download_url": "https://x/app-release.apk", "size": 2 }
          ]
        }
        """.trimIndent()

        val info = checker.parse(comDebug)
        assertEquals("app-release.apk", info.assetName)
    }

    @Test
    fun `sem nenhum apk anexado da erro claro`() {
        val semApk = """{ "tag_name": "v1.0.0", "assets": [] }"""
        val error = assertThrows(IllegalStateException::class.java) { checker.parse(semApk) }
        assertTrue("mensagem deveria citar a tag", error.message!!.contains("v1.0.0"))
    }

    @Test
    fun `ignora assets que nao sao apk`() {
        val comZip = """
        {
          "tag_name": "v1.0.0",
          "assets": [
            { "name": "mapping.zip", "browser_download_url": "https://x/mapping.zip", "size": 1 },
            { "name": "app-release.apk", "browser_download_url": "https://x/app-release.apk", "size": 2 }
          ]
        }
        """.trimIndent()
        assertEquals("app-release.apk", checker.parse(comZip).assetName)
    }
}
