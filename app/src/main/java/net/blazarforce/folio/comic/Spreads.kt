package net.blazarforce.folio.comic

/**
 * Groups pages into two-page spreads. The cover, ComicInfo DoublePage pages and an
 * odd trailing page stand alone; everything else pairs up in reading order.
 */
fun buildSpreads(pageCount: Int, coverIndex: Int = 0, doublePages: Set<Int> = emptySet()): List<IntArray> {
    val out = mutableListOf<IntArray>()
    var i = 0
    while (i < pageCount) {
        val alone = i == coverIndex || i in doublePages || i == pageCount - 1 || (i + 1) in doublePages
        if (alone) {
            out += intArrayOf(i); i++
        } else {
            out += intArrayOf(i, i + 1); i += 2
        }
    }
    return out
}

fun singlePages(pageCount: Int): List<IntArray> = List(pageCount) { intArrayOf(it) }

/** Index of the unit (page or spread) containing [page]. */
fun List<IntArray>.unitOf(page: Int): Int = indexOfFirst { page in it }.coerceAtLeast(0)
