package net.blazarforce.folio.data

import org.json.JSONArray
import org.json.JSONObject

enum class BookKind { CBZ, PDF, FOLDER }

data class Book(
    val id: String,
    val kind: BookKind,
    /** Absolute path of the private copy (CBZ / PDF) or a tree URI for folders. */
    val location: String,
    val title: String,
    val series: String?,
    val number: String?,
    val numberLabel: String? = null,
    val pageCount: Int,
    val coverPath: String?,
    val rightToLeft: Boolean = false,
    val doublePages: Set<Int> = emptySet(),
    val coverIndex: Int = 0,
    val lastPage: Int = 0,
    val finished: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long = 0L,
    val sourceName: String = "",
    val sizeBytes: Long = 0L,
    /** Per-book reading-direction override; null = use ComicInfo. */
    val rtlOverride: Boolean? = null,
) {
    val progress: Float
        get() = when {
            finished -> 1f
            pageCount <= 1 -> 0f
            else -> (lastPage.toFloat() / (pageCount - 1)).coerceIn(0f, 1f)
        }
    val started: Boolean get() = lastReadAt > 0L
    val inProgress: Boolean get() = started && !finished
    val effectiveRtl: Boolean get() = rtlOverride ?: rightToLeft
    val numberValue: Double get() = number?.toDoubleOrNull() ?: Double.MAX_VALUE

    /** "Chapter 1" style short label, falling back to the title. */
    val shortLabel: String
        get() = if (number != null) "${numberLabel ?: "No."} $number" else title

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("kind", kind.name); put("location", location); put("title", title)
        put("series", series); put("number", number); put("numberLabel", numberLabel)
        put("pageCount", pageCount); put("coverPath", coverPath); put("rtl", rightToLeft)
        put("double", JSONArray(doublePages.toList())); put("coverIndex", coverIndex)
        put("lastPage", lastPage); put("finished", finished); put("addedAt", addedAt)
        put("lastReadAt", lastReadAt); put("sourceName", sourceName); put("size", sizeBytes)
        if (rtlOverride != null) put("rtlOverride", rtlOverride)
    }

    companion object {
        fun fromJson(o: JSONObject): Book {
            fun str(k: String) = if (o.isNull(k)) null else o.optString(k).takeIf { it.isNotEmpty() }
            val d = o.optJSONArray("double")
            return Book(
                id = o.getString("id"),
                kind = BookKind.valueOf(o.getString("kind")),
                location = o.getString("location"),
                title = o.getString("title"),
                series = str("series"),
                number = str("number"),
                numberLabel = str("numberLabel"),
                pageCount = o.optInt("pageCount"),
                coverPath = str("coverPath"),
                rightToLeft = o.optBoolean("rtl"),
                doublePages = if (d == null) emptySet() else (0 until d.length()).map { d.getInt(it) }.toSet(),
                coverIndex = o.optInt("coverIndex"),
                lastPage = o.optInt("lastPage"),
                finished = o.optBoolean("finished"),
                addedAt = o.optLong("addedAt"),
                lastReadAt = o.optLong("lastReadAt"),
                sourceName = o.optString("sourceName"),
                sizeBytes = o.optLong("size"),
                rtlOverride = if (o.has("rtlOverride")) o.optBoolean("rtlOverride") else null,
            )
        }
    }
}

/** A shelf entry: either a lone book or a series with several issues. */
sealed interface ShelfItem {
    val key: String
    val sortTime: Long

    data class Single(val book: Book) : ShelfItem {
        override val key get() = "b:" + book.id
        override val sortTime get() = maxOf(book.lastReadAt, book.addedAt)
    }

    data class Series(val name: String, val books: List<Book>) : ShelfItem {
        override val key get() = "s:$name"
        override val sortTime get() = books.maxOf { maxOf(it.lastReadAt, it.addedAt) }
        val readCount get() = books.count { it.finished }
        val progress get() = books.sumOf { it.progress.toDouble() }.toFloat() / books.size
        /** The issue to open next: in-progress, else first unfinished, else the first. */
        val next: Book
            get() = books.filter { it.inProgress }.maxByOrNull { it.lastReadAt }
                ?: books.firstOrNull { !it.finished } ?: books.first()
    }
}

fun List<Book>.sortedForSeries(): List<Book> =
    sortedWith(compareBy<Book> { it.numberValue }.thenBy { it.kind.ordinal }.thenBy { it.title })

fun buildShelf(books: List<Book>): List<ShelfItem> {
    val (withSeries, singles) = books.partition { it.series != null }
    val items = mutableListOf<ShelfItem>()
    withSeries.groupBy { it.series!!.lowercase() }.forEach { (_, group) ->
        if (group.size == 1) items += ShelfItem.Single(group.first())
        else items += ShelfItem.Series(group.first().series!!, group.sortedForSeries())
    }
    singles.forEach { items += ShelfItem.Single(it) }
    return items.sortedByDescending { it.sortTime }
}

/** Next issue after [book] in its series (prefers the same format). */
fun List<Book>.nextInSeries(book: Book): Book? {
    val s = book.series ?: return null
    val pool = filter { it.series.equals(s, true) && it.numberValue > book.numberValue }
    val n = pool.minOfOrNull { it.numberValue } ?: return null
    val candidates = pool.filter { it.numberValue == n }
    return candidates.firstOrNull { it.kind == book.kind } ?: candidates.first()
}
