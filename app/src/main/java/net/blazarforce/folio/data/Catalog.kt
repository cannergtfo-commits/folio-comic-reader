package net.blazarforce.folio.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * The online library. Folio fetches one small JSON file (see catalog/catalog.json in the repo)
 * that lists series and their chapters, so new chapters appear without an app update.
 */
data class Catalog(
    val schema: Int,
    val name: String,
    val updated: String?,
    val app: AppRelease?,
    val series: List<CatalogSeries>,
) {
    val chapters: List<CatalogChapter> get() = series.flatMap { it.chapters }

    companion object {
        const val SCHEMA = 1

        fun parse(text: String): Catalog {
            val o = JSONObject(text)
            val arr = o.optJSONArray("series") ?: JSONArray()
            return Catalog(
                schema = o.optInt("schema", 1),
                name = o.optString("name", "Folio Library"),
                updated = o.str("updated"),
                app = o.optJSONObject("app")?.let { a ->
                    AppRelease(a.optString("version"), a.optInt("versionCode"), a.optString("apk"), a.str("notes"))
                },
                series = (0 until arr.length()).mapNotNull { i ->
                    runCatching { CatalogSeries.fromJson(arr.getJSONObject(i)) }.getOrNull()
                }.filter { it.chapters.isNotEmpty() },
            )
        }
    }
}

data class AppRelease(val version: String, val versionCode: Int, val apk: String, val notes: String?)

data class CatalogSeries(
    val id: String,
    val title: String,
    val author: String?,
    val description: String?,
    val cover: String?,
    val tags: List<String>,
    val chapters: List<CatalogChapter>,
) {
    companion object {
        fun fromJson(o: JSONObject): CatalogSeries {
            val id = o.getString("id")
            val title = o.getString("title")
            val tags = o.optJSONArray("tags")?.let { t -> (0 until t.length()).map { t.getString(it) } } ?: emptyList()
            val ch = o.optJSONArray("chapters") ?: JSONArray()
            val chapters = (0 until ch.length()).mapNotNull { i ->
                runCatching { CatalogChapter.fromJson(ch.getJSONObject(i), id, title) }.getOrNull()
            }.sortedBy { it.number }
            return CatalogSeries(id, title, o.str("author"), o.str("description"), o.str("cover"), tags, chapters)
        }
    }
}

data class CatalogChapter(
    val id: String,
    val seriesId: String,
    val seriesTitle: String,
    val number: Double,
    val title: String,
    val cover: String?,
    val cbz: String,
    /** File name the chapter is saved under. Same name + size as a side-loaded copy = same shelf book. */
    val fileName: String,
    val size: Long,
    val pages: Int,
    val published: String?,
) {
    val numberLabel: String get() = if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()

    /** Shelf id the Importer gives this file (see Importer.idFor). */
    val bookId: String get() = bookIdFor(fileName, size.toString())

    companion object {
        fun fromJson(o: JSONObject, seriesId: String, seriesTitle: String): CatalogChapter {
            val cbz = o.getString("cbz")
            return CatalogChapter(
                id = o.getString("id"), seriesId = seriesId, seriesTitle = seriesTitle,
                number = o.getDouble("number"), title = o.getString("title"),
                cover = o.str("cover"), cbz = cbz,
                fileName = o.str("file") ?: cbz.substringAfterLast('/').substringBefore('?'),
                size = o.optLong("size", 0L), pages = o.optInt("pages", 0), published = o.str("published"),
            )
        }
    }
}

private fun JSONObject.str(k: String): String? = if (isNull(k)) null else optString(k).takeIf { it.isNotEmpty() }

/** Same scheme as Importer: first 8 bytes of SHA-1 over "name|size". */
fun bookIdFor(vararg parts: String): String {
    val md = MessageDigest.getInstance("SHA-1").digest(parts.joinToString("|").toByteArray())
    return md.take(8).joinToString("") { "%02x".format(it) }
}
