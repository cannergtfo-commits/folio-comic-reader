package net.blazarforce.folio.comic

/**
 * "Human" ordering for file names: page2 < page10, case-insensitive,
 * leading zeros ignored for magnitude. Falls back to plain ordering for stability.
 */
object NaturalOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]
            val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                var si = i; while (si < a.length && a[si] == '0') si++
                var sj = j; while (sj < b.length && b[sj] == '0') sj++
                var ei = si; while (ei < a.length && a[ei].isDigit()) ei++
                var ej = sj; while (ej < b.length && b[ej].isDigit()) ej++
                val la = ei - si
                val lb = ej - sj
                if (la != lb) return la - lb
                for (k in 0 until la) {
                    val d = a[si + k] - b[sj + k]
                    if (d != 0) return d
                }
                i = ei; j = ej
            } else {
                val d = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (d != 0) return d
                i++; j++
            }
        }
        val rest = (a.length - i) - (b.length - j)
        return if (rest != 0) rest else a.compareTo(b)
    }
}
