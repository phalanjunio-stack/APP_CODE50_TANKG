package com.srlakes.tone.update

/**
 * Compara duas versoes no estilo "1.2.3", tolerando o prefixo "v" das
 * tags do GitHub ("v1.2.3") e um sufixo de pre-lancamento ("1.0.0-mvp").
 *
 * Nao e um parser de SemVer completo - so o suficiente para decidir uma
 * pergunta: "a versao remota e mais nova que a instalada?". Errar essa
 * pergunta para "sim" sem necessidade incomoda o musico com um aviso a
 * toa; errar para "nao" o deixa preso numa versao velha. Por isso tem
 * testes cobrindo os casos de canto.
 */
object VersionComparator {

    /** > 0 se [a] for mais nova que [b], 0 se iguais, < 0 se mais velha. */
    fun compare(a: String, b: String): Int {
        val (coreA, preA) = split(a)
        val (coreB, preB) = split(b)

        val len = maxOf(coreA.size, coreB.size)
        for (i in 0 until len) {
            val partA = coreA.getOrElse(i) { 0 }
            val partB = coreB.getOrElse(i) { 0 }
            if (partA != partB) return partA - partB
        }

        // Numeros iguais: uma versao sem sufixo de pre-lancamento e mais
        // nova que a mesma versao COM sufixo ("1.0.0" > "1.0.0-mvp").
        return when {
            preA == null && preB == null -> 0
            preA == null -> 1
            preB == null -> -1
            else -> preA.compareTo(preB)
        }
    }

    fun isNewer(remote: String, local: String): Boolean = compare(remote, local) > 0

    private fun split(version: String): Pair<List<Int>, String?> {
        val trimmed = version.trim().removePrefix("v").removePrefix("V")
        val dashIndex = trimmed.indexOf('-')
        val corePart = if (dashIndex >= 0) trimmed.substring(0, dashIndex) else trimmed
        val preRelease = if (dashIndex >= 0) trimmed.substring(dashIndex + 1) else null
        val core = corePart.split(".").map { it.toIntOrNull() ?: 0 }
        return core to preRelease
    }
}
