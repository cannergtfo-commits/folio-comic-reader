package net.blazarforce.folio.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** What the Discover screen shows. */
data class LibraryState(
    val catalog: Catalog? = null,
    val refreshing: Boolean = false,
    /** Wall time of the last successful fetch, 0 if never. */
    val fetchedAt: Long = 0L,
    /** Set when the latest refresh failed; the cached catalog (if any) is still shown. */
    val error: String? = null,
    /** Chapter ids the reader has already seen in Discover. Anything else is "new". */
    val seen: Set<String> = emptySet(),
    /** Local cover thumbnails by URL, filled as they download. */
    val covers: Map<String, String> = emptyMap(),
    /** chapter id -> download progress 0..1 (negative = size unknown). */
    val downloads: Map<String, Float> = emptyMap(),
)

/**
 * Fetches the online catalog, keeps a copy on disk for offline use, caches cover thumbnails,
 * and downloads chapters straight onto the shelf.
 */
class CatalogRepo(
    context: Context,
    private val store: LibraryStore,
    private val importer: Importer,
    private val url: String = CATALOG_URL,
) {
    private val dir = File(context.filesDir, "catalog").apply { mkdirs() }
    private val cacheFile = File(dir, "catalog.json")
    private val coverDir = File(dir, "covers").apply { mkdirs() }
    private val sp = context.getSharedPreferences("folio_catalog", Context.MODE_PRIVATE)
    private val refreshLock = Mutex()

    private val _state = MutableStateFlow(initial())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private fun initial(): LibraryState {
        val cat = runCatching { Catalog.parse(cacheFile.readText()) }.getOrNull()
        val covers = cat?.let { c -> coverUrls(c).mapNotNull { u -> coverFile(u).takeIf { it.exists() }?.let { u to it.absolutePath } }.toMap() }
        return LibraryState(
            catalog = cat,
            fetchedAt = sp.getLong("fetchedAt", 0L),
            seen = sp.getStringSet("seen", emptySet())!!.toSet(),
            covers = covers ?: emptyMap(),
        )
    }

    fun isOnShelf(ch: CatalogChapter): Boolean = store.get(ch.bookId) != null
    fun bookFor(ch: CatalogChapter): Book? = store.get(ch.bookId)

    /** Chapters not on the shelf and not yet seen in Discover. */
    fun newChapters(s: LibraryState = _state.value): List<CatalogChapter> =
        s.catalog?.chapters.orEmpty().filter { it.id !in s.seen && !isOnShelf(it) }

    /**
     * Re-downloads the catalog. Returns the chapters that appeared since the previous fetch
     * (empty on the very first fetch, so a new install isn't greeted with a toast).
     */
    suspend fun refresh(): List<CatalogChapter> = refreshLock.withLock {
        _state.update { it.copy(refreshing = true) }
        try {
            val text = withContext(Dispatchers.IO) { httpText(url) }
            val cat = Catalog.parse(text)
            withContext(Dispatchers.IO) {
                val tmp = File(dir, "catalog.json.tmp"); tmp.writeText(text); tmp.renameTo(cacheFile)
            }
            val known = sp.getStringSet("known", null)
            val ids = cat.chapters.map { it.id }.toSet()
            val now = System.currentTimeMillis()
            sp.edit().putStringSet("known", ids).putLong("fetchedAt", now).apply()
            _state.update { it.copy(catalog = cat, refreshing = false, fetchedAt = now, error = null) }
            fetchCovers(cat)
            if (known == null) emptyList() else cat.chapters.filter { it.id !in known }
        } catch (e: CancellationException) {
            _state.update { it.copy(refreshing = false) }
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(refreshing = false, error = if (it.catalog == null) "offline-empty" else "offline") }
            emptyList()
        }
    }

    fun markSeen(ids: Collection<String>) {
        val s = _state.value.seen + ids
        if (s.size == _state.value.seen.size) return
        sp.edit().putStringSet("seen", s).apply()
        _state.update { it.copy(seen = s) }
    }

    /** Downloads one chapter into app storage and puts it on the shelf. */
    suspend fun download(ch: CatalogChapter): Result<Book> {
        bookFor(ch)?.let { return Result.success(it) }
        if (ch.id in _state.value.downloads) return Result.failure(IllegalStateException("busy"))
        _state.update { it.copy(downloads = it.downloads + (ch.id to 0f)) }
        return try {
            val book = withContext(Dispatchers.IO) {
                val target = importer.downloadTarget(ch.bookId)
                httpToFile(ch.cbz, target) { done, total ->
                    val t = if (total > 0) total else ch.size
                    val p = if (t > 0) (done.toFloat() / t).coerceIn(0f, 1f) else -1f
                    _state.update { s -> s.copy(downloads = s.downloads + (ch.id to p)) }
                }
                if (ch.size > 0 && target.length() != ch.size) {
                    target.delete(); throw IOException("Download was incomplete")
                }
                importer.importDownloaded(target, ch.fileName) ?: throw IOException("Not a comic file")
            }
            markSeen(listOf(ch.id))
            Result.success(book)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _state.update { it.copy(downloads = it.downloads - ch.id) }
        }
    }

    private fun coverUrls(c: Catalog) = (c.series.mapNotNull { it.cover } + c.chapters.mapNotNull { it.cover }).distinct()
    private fun coverFile(u: String) = File(coverDir, bookIdFor(u) + ".img")

    private suspend fun fetchCovers(c: Catalog) = withContext(Dispatchers.IO) {
        for (u in coverUrls(c)) {
            val f = coverFile(u)
            if (!f.exists()) {
                val ok = runCatching { httpToFile(u, f) { _, _ -> } }.isSuccess
                if (!ok) continue
            }
            _state.update { it.copy(covers = it.covers + (u to f.absolutePath)) }
        }
    }

    companion object {
        /** Stable, public URL of the catalog. Edit catalog/catalog.json in the repo to publish. */
        const val CATALOG_URL =
            "https://raw.githubusercontent.com/cannergtfo-commits/folio-comic-reader/main/catalog/catalog.json"

        private fun open(url: String): HttpURLConnection {
            var u = URL(url)
            repeat(6) {
                val c = (u.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "BlazarReader-Android")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                val code = c.responseCode
                if (code in 300..399) {
                    val loc = c.getHeaderField("Location") ?: throw IOException("Redirect without location")
                    c.disconnect(); u = URL(u, loc); return@repeat
                }
                if (code != 200) { c.disconnect(); throw IOException("HTTP $code") }
                return c
            }
            throw IOException("Too many redirects")
        }

        fun httpText(url: String): String {
            val c = open(url)
            return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
        }

        /** Streams [url] into [dest] (via a .part file). Returns [dest]. */
        fun httpToFile(url: String, dest: File, progress: (Long, Long) -> Unit): File {
            val c = open(url)
            val part = File(dest.parentFile, dest.name + ".part")
            try {
                val total = c.contentLengthLong
                c.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(128 * 1024)
                        var done = 0L; var last = 0L
                        while (true) {
                            val n = input.read(buf); if (n < 0) break
                            out.write(buf, 0, n); done += n
                            if (done - last > 256 * 1024) { progress(done, total); last = done }
                        }
                        progress(done, total)
                    }
                }
                if (dest.exists()) dest.delete()
                if (!part.renameTo(dest)) throw IOException("Could not save file")
                return dest
            } catch (e: Exception) {
                part.delete(); throw e
            } finally { c.disconnect() }
        }
    }
}
