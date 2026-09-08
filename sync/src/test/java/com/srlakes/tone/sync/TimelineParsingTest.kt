package com.srlakes.tone.sync

import com.srlakes.tone.model.EffectSlot
import com.srlakes.tone.model.SectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Entendimento do formato da timeline do SR Lakes Studio.
 *
 * O JSON abaixo é a forma real do acervo da banda: uma trilha
 * `kind: "section"` com blocos `{start, length, type, content}` em
 * compassos, mais uma trilha `kind: "control"` no formato combinado
 * para a Controladora.
 */
class TimelineParsingTest {

    private val api = StudioApi()

    /** Recorte fiel de "SÓ POR MEU PRAZER", como está no songs.json. */
    private val realShape = """
    [{
      "id": 2,
      "title": "SÓ POR MEU PRAZER",
      "artist": "",
      "key": "D",
      "bpm": 65,
      "timeline": {
        "bars": 130, "bpm": 65, "sig": "4/4",
        "tracks": [
          { "id": "tr-click", "kind": "click", "name": "Click", "clips": [] },
          { "id": "tr-section", "kind": "section", "name": "Secoes", "clips": [
            { "start": 1,  "length": 4, "type": "intro",     "content": "INTRODUÇÃO" },
            { "start": 5,  "length": 4, "type": "verso",     "content": "VERSO" },
            { "start": 9,  "length": 4, "type": "prerefrao", "content": "PRÉ REFRÃO" },
            { "start": 13, "length": 4, "type": "refrao",    "content": "REFRÃO" },
            { "start": 17, "length": 4, "type": "ponte",     "content": "PONTE" }
          ]},
          { "id": "tr-chord", "kind": "chord", "name": "Cifras", "clips": [] }
        ]
      }
    }]
    """.trimIndent()

    @Test
    fun `le a trilha de secoes que o Studio ja tem`() {
        val song = api.parseSongs(realShape).single()

        assertEquals(2, song.id)
        assertEquals("SÓ POR MEU PRAZER", song.title)
        assertEquals(5, song.sections.size)
        assertTrue("sem trilha Controladora ainda", song.controls.isEmpty())

        assertEquals(
            listOf(
                SectionType.INTRO,
                SectionType.VERSO,
                SectionType.PRE_REFRAO,
                SectionType.REFRAO,
                SectionType.PONTE
            ),
            song.sections.map { it.sectionType }
        )
        assertEquals("PRÉ REFRÃO", song.sections[2].label)
    }

    @Test
    fun `acha o bloco ativo pela mesma regra do Studio`() {
        val song = api.parseSongs(realShape).single()

        // start <= pos < start + length -- igual ao activeTimelineClip de la.
        assertEquals(SectionType.INTRO, song.sectionAt(1f)?.sectionType)
        assertEquals(SectionType.INTRO, song.sectionAt(4.99f)?.sectionType)
        assertEquals(SectionType.VERSO, song.sectionAt(5f)?.sectionType)
        assertEquals(SectionType.PRE_REFRAO, song.sectionAt(11.5f)?.sectionType)
        assertEquals(SectionType.PONTE, song.sectionAt(20.9f)?.sectionType)
    }

    @Test
    fun `fora dos blocos nao ha secao ativa`() {
        val song = api.parseSongs(realShape).single()
        assertNull("antes do primeiro bloco", song.sectionAt(0.5f))
        assertNull("depois do ultimo bloco", song.sectionAt(21f))
    }

    @Test
    fun `com blocos sobrepostos vence o primeiro do array, como no Studio`() {
        // Isto é o acervo real de "SUA MANEIRA": o bloco do compasso 2 tem
        // 4.025 de comprimento, então vai até 6.025 e invade o bloco que
        // começa no 6. No compasso 6 os dois valem.
        //
        // O Studio resolve com clips.find(...), que devolve o primeiro da
        // ordem do array — o do compasso 6. O app precisa concordar com o
        // que está escrito na tela do notebook.
        val sobrepostos = """
        [{ "id": 1, "title": "SUA MANEIRA", "timeline": { "tracks": [
          { "kind": "section", "clips": [
            { "start": 6, "length": 8, "type": "solo",  "content": "Verso" },
            { "start": 2, "length": 4.025, "type": "intro",  "content": "Introdução" },
            { "start": 14, "length": 8, "type": "refrao", "content": "REFRÃO" }
          ]}
        ]}}]
        """.trimIndent()

        val song = api.parseSongs(sobrepostos).single()
        assertEquals("a ordem do array é preservada", listOf(6f, 2f, 14f), song.sections.map { it.startBar })

        // No trecho em que só um bloco vale, não há dúvida.
        assertEquals(SectionType.INTRO, song.sectionAt(3f)?.sectionType)
        assertEquals(SectionType.REFRAO, song.sectionAt(15f)?.sectionType)

        // No compasso 6, onde os dois valem, vence o primeiro do array.
        assertEquals(SectionType.SOLO, song.sectionAt(6f)?.sectionType)
    }

    // ---------------- trilha Controladora ----------------

    @Test
    fun `le o bloco da Controladora no formato combinado`() {
        val comControle = """
        [{ "id": 2, "title": "SÓ POR MEU PRAZER", "timeline": { "tracks": [
          { "kind": "control", "name": "Controladora", "clips": [
            { "start": 22, "length": 8, "type": "scene", "content": "SOLO",
              "control": { "scene": "SOLO", "boost": true, "delay": true, "volume": 8 } }
          ]}
        ]}}]
        """.trimIndent()

        val clip = api.parseSongs(comControle).single().controls.single()
        assertEquals(22f, clip.startBar, 0.001f)
        assertEquals(8f, clip.lengthBars, 0.001f)

        assertNotNull("o bloco deveria ter comando", clip.control)
        val control = clip.control!!
        assertEquals("SOLO", control.scene)
        assertEquals(true, control.effects[EffectSlot.BOOST])
        assertEquals(true, control.effects[EffectSlot.DELAY])
        assertEquals(8f, control.volume!!, 0.001f)
        // Só o que foi escrito vira comando: reverb e gate ficam como o preset manda.
        assertNull(control.effects[EffectSlot.REVERB])
        assertNull(control.effects[EffectSlot.GATE])
    }

    @Test
    fun `bloco sem objeto control vale pelo rotulo`() {
        // Tolerância proposital: o Studio ainda está sendo feito, e um
        // bloco escrito só "SOLO" já deve funcionar.
        val soRotulo = """
        [{ "id": 2, "title": "X", "timeline": { "tracks": [
          { "kind": "control", "clips": [
            { "start": 0, "length": 4, "type": "scene", "content": "SOLO" }
          ]}
        ]}}]
        """.trimIndent()

        val control = api.parseSongs(soRotulo).single().controls.single().control!!
        assertEquals("SOLO", control.scene)
        assertTrue(control.effects.isEmpty())
        assertNull(control.volume)
    }

    @Test
    fun `desligar um efeito tambem e um comando`() {
        val desliga = """
        [{ "id": 2, "title": "X", "timeline": { "tracks": [
          { "kind": "control", "clips": [
            { "start": 0, "length": 4, "type": "effect", "content": "SEM DELAY",
              "control": { "scene": "BASE", "delay": false } }
          ]}
        ]}}]
        """.trimIndent()

        val control = api.parseSongs(desliga).single().controls.single().control!!
        assertEquals("BASE", control.scene)
        assertEquals(false, control.effects[EffectSlot.DELAY])
    }

    @Test
    fun `musica sem timeline nao explode`() {
        val semTimeline = """[{ "id": 9, "title": "Nova" }]"""
        val song = api.parseSongs(semTimeline).single()
        assertTrue(song.sections.isEmpty())
        assertTrue(song.controls.isEmpty())
        assertNull(song.sectionAt(1f))
    }

    @Test
    fun `tipo desconhecido nao vira secao`() {
        assertNull(SectionType.fromStudioType("interludio"))
        assertNull(SectionType.fromStudioType(null))
        assertEquals(SectionType.OUTRO, SectionType.fromStudioType("final"))
        assertEquals(SectionType.PRE_REFRAO, SectionType.fromStudioType("prerefrao"))
    }
}
