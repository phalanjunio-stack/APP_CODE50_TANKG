package com.srlakes.tone.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `versao maior e mais nova`() {
        assertTrue(VersionComparator.isNewer("1.1.0", "1.0.0"))
        assertTrue(VersionComparator.isNewer("2.0.0", "1.9.9"))
        assertTrue(VersionComparator.isNewer("1.0.10", "1.0.9"))
    }

    @Test
    fun `versao igual nao e mais nova`() {
        assertFalse(VersionComparator.isNewer("1.0.0", "1.0.0"))
        assertEquals(0, VersionComparator.compare("1.0.0", "1.0.0"))
    }

    @Test
    fun `versao menor nao e mais nova`() {
        assertFalse(VersionComparator.isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun `prefixo v das tags do GitHub e ignorado`() {
        assertTrue(VersionComparator.isNewer("v1.1.0", "1.0.0"))
        assertTrue(VersionComparator.isNewer("1.1.0", "v1.0.0"))
        assertEquals(0, VersionComparator.compare("v1.0.0", "1.0.0"))
    }

    @Test
    fun `numero de componentes diferente nao quebra`() {
        // "1.0" e tratado como "1.0.0"
        assertEquals(0, VersionComparator.compare("1.0", "1.0.0"))
        assertTrue(VersionComparator.isNewer("1.0.1", "1.0"))
    }

    @Test
    fun `versao sem sufixo e mais nova que a mesma com pre-lancamento`() {
        assertTrue(VersionComparator.isNewer("1.0.0", "1.0.0-mvp"))
        assertFalse(VersionComparator.isNewer("1.0.0-mvp", "1.0.0"))
    }

    @Test
    fun `numero maior vence mesmo com sufixo de pre-lancamento`() {
        assertTrue(VersionComparator.isNewer("1.1.0-beta", "1.0.0"))
    }

    @Test
    fun `lixo nao numerico nao derruba a comparacao`() {
        // Entrada malformada vira 0 nesse componente, em vez de lançar exceção.
        // Errar em "não há atualização" é o lado seguro para um app de palco.
        assertEquals(0, VersionComparator.compare("abc", "0.0.0"))
    }
}
