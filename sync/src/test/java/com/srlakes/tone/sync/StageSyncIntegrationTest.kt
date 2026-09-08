package com.srlakes.tone.sync

import com.srlakes.tone.model.SyncState
import io.socket.client.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Teste de integração contra o servidor real do SR Lakes Studio.
 *
 * Ele NÃO roda sozinho: se não houver um Studio escutando em
 * [SERVER_URL], os testes são pulados. É de propósito — a máquina de
 * build de qualquer outra pessoa não tem o notebook da banda na rede.
 *
 * Para rodar:
 *
 *   cd "<pasta do SR Lakes Studio>/apps/desktop"
 *   PORT=7599 node server.js
 *   ./gradlew :sync:test
 *
 * O que ele prova, e que ler o código não provava: que o nosso cliente
 * entra na sala como receptor, recebe o snapshot, e traduz o evento
 * "state" do transmissor no nosso StageState.
 */
class StageSyncIntegrationTest {

    @Test
    fun `recebe a troca de musica do transmissor`() = runBlocking {
        assumeTrue("Nenhum Studio escutando em " + SERVER_URL, serverReachable())

        val client = StageSyncClient()
        try {
            client.connect(SERVER_URL, "SR Lakes Tone (teste)")

            withTimeout(TIMEOUT_MS) {
                client.status.first { it.state == SyncState.CONNECTED }
            }

            // Um segundo cliente faz o papel do notebook: identifica-se
            // como transmissor e publica a troca de música.
            val transmitter = IO.socket(URI.create(SERVER_URL))
            try {
                transmitter.connect()
                withTimeout(TIMEOUT_MS) {
                    while (!transmitter.connected()) Thread.sleep(50)
                }
                transmitter.emit("identify", JSONObject().put("name", "Notebook").put("mode", "tx"))
                transmitter.emit(
                    "state",
                    JSONObject()
                        .put("songId", 2)
                        .put("partIdx", 1)
                        .put("playing", true)
                        .put("bpm", 65)
                        .put("bar", 3)
                        .put("beat", 2)
                        .put("activeChord", "D")
                )

                val received = withTimeout(TIMEOUT_MS) {
                    client.stage.first { it != null && it.studioSongId == 2 }
                }!!

                assertEquals(2, received.studioSongId)
                assertEquals(1, received.partIndex)
                assertEquals(65, received.bpm)
                assertTrue(received.playing)
                assertEquals("D", received.activeChord)
            } finally {
                transmitter.disconnect()
                transmitter.close()
            }
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `le a biblioteca do Studio`() = runBlocking {
        assumeTrue("Nenhum Studio escutando em " + SERVER_URL, serverReachable())

        val songs = StudioApi().songs(SERVER_URL).getOrThrow()
        assertTrue("O Studio deveria devolver ao menos uma música", songs.isNotEmpty())
        songs.forEach {
            assertTrue("toda música precisa de id", it.id >= 0)
            assertTrue("toda música precisa de título", it.title.isNotBlank())
        }

        // O acervo real tem a trilha de seções desenhada. Se isto falhar,
        // o formato da timeline mudou e o app precisa acompanhar.
        val comSecoes = songs.filter { it.sections.isNotEmpty() }
        assertTrue(
            "nenhuma música do Studio tem trilha de seções — o formato mudou?",
            comSecoes.isNotEmpty()
        )
        comSecoes.forEach { song ->
            song.sections.forEach { clip ->
                assertTrue("bloco sem duração em " + song.title, clip.lengthBars > 0f)
            }
            // Todo bloco deve cair dentro de si mesmo: garante que a regra
            // de posição bate com a do Studio.
            song.sections.forEach { clip ->
                assertEquals(clip.key, song.sectionAt(clip.startBar)?.key)
            }
        }
    }

    private fun serverReachable(): Boolean = runCatching {
        val connection = URL(SERVER_URL + "/api/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 1500
        connection.readTimeout = 1500
        try {
            connection.responseCode == 200
        } finally {
            connection.disconnect()
        }
    }.getOrDefault(false)

    private companion object {
        const val SERVER_URL = "http://127.0.0.1:7599"
        const val TIMEOUT_MS = 15000L
    }
}

/** Testes que não dependem de servidor nenhum. */
class StudioApiTest {

    @Test
    fun `completa o esquema e a porta padrao`() {
        assertEquals("http://192.168.0.10:7575", StudioApi.normalizeUrl("192.168.0.10"))
        assertEquals("http://192.168.0.10:7599", StudioApi.normalizeUrl("192.168.0.10:7599"))
        assertEquals("http://192.168.0.10:7575", StudioApi.normalizeUrl("http://192.168.0.10/"))
        assertEquals("http://192.168.0.10:7575", StudioApi.normalizeUrl("  192.168.0.10  "))
    }

    @Test
    fun `endereco vazio nao vira url`() {
        assertEquals(null, StudioApi.normalizeUrl(""))
        assertEquals(null, StudioApi.normalizeUrl("   "))
    }
}
