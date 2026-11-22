package net.blazarforce.folio.comic

/** Derives series / number / display title from file names like "Lyra_Ch1.cbz". */
object NameHeuristics {
    data class Guess(val series: String?, val number: String?, val label: String?, val title: String)

    private val pattern = Regex(
        """^(.*?)[\s_.\-]*(ch(?:apter)?|vol(?:ume)?|v|issue|no|#|ep(?:isode)?)?[\s_.\-#]*(\d+(?:\.\d+)?)$""",
        RegexOption.IGNORE_CASE,
    )

    fun clean(raw: String): String =
        raw.replace('_', ' ').replace(Regex("\\s+"), " ").trim()

    fun guess(fileName: String): Guess {
        val base = clean(fileName.substringBeforeLast('.'))
        val m = pattern.matchEntire(base)
        if (m != null && m.groupValues[1].isNotBlank()) {
            val series = clean(m.groupValues[1]).trimEnd('-', ',', ' ')
            val num = m.groupValues[3].trimStart('0').ifEmpty { "0" }
            val label = when (m.groupValues[2].lowercase()) {
                "vol", "volume", "v" -> "Volume"
                "issue", "no", "#" -> "Issue"
                "ep", "episode" -> "Episode"
                else -> "Chapter"
            }
            return Guess(series, num, label, "$series — $label $num")
        }
        return Guess(null, null, null, base)
    }
}
