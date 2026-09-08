package com.srlakes.tone.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * O casamento automático de músicas entre o SR Lakes Studio e o
 * SR Lakes Tone é feito pelo título. As duas bibliotecas foram digitadas
 * por pessoas diferentes, em momentos diferentes — uma em caixa alta,
 * outra não, uma com acento, outra sem. É aqui que isso é resolvido.
 */
class NormalizeTitleTest {

    @Test
    fun `ignora caixa e acento`() {
        assertEquals(normalizeTitle("SÓ POR MEU PRAZER"), normalizeTitle("Só por meu prazer"))
        assertEquals(normalizeTitle("SÓ POR MEU PRAZER"), normalizeTitle("So Por Meu Prazer"))
    }

    @Test
    fun `ignora pontuacao e espaco sobrando`() {
        assertEquals(normalizeTitle("Ana Júlia"), normalizeTitle("  ana-julia!  "))
        assertEquals(normalizeTitle("ABERTURA-NA MORAL"), normalizeTitle("Abertura na Moral"))
    }

    @Test
    fun `nao junta musicas diferentes`() {
        assertNotEquals(normalizeTitle("Meu Erro"), normalizeTitle("Meu Erro 2"))
        assertNotEquals(normalizeTitle("O Sol"), normalizeTitle("O Sal"))
    }

    @Test
    fun `titulos reais do Studio casam com os daqui`() {
        // Exatamente como estão nas duas bases hoje.
        val doStudio = listOf("SUA MANEIRA", "SÓ POR MEU PRAZER", "ABERTURA-NA MORAL")
        val daqui = listOf("Sua Maneira", "Só por Meu Prazer", "Abertura - Na Moral")
        doStudio.forEachIndexed { i, studio ->
            assertEquals(
                "deveria casar: " + studio + " / " + daqui[i],
                normalizeTitle(studio),
                normalizeTitle(daqui[i])
            )
        }
    }

    @Test
    fun `titulo vazio nao explode`() {
        assertEquals("", normalizeTitle(""))
        assertEquals("", normalizeTitle("   ---   "))
    }
}
