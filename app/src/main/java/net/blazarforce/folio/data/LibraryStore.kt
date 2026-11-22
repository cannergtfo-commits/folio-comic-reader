package net.blazarforce.folio.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/** Tiny JSON-backed library. Small enough that a whole-file rewrite is fine. */
class LibraryStore(context: Context) {
    private val file = File(context.filesDir, "library.json")
    private val _books = MutableStateFlow(load())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private fun load(): List<Book> = runCatching {
        val arr = JSONArray(file.readText())
        (0 until arr.length()).mapNotNull { runCatching { Book.fromJson(arr.getJSONObject(it)) }.getOrNull() }
    }.getOrDefault(emptyList())

    @Synchronized
    private fun persist(list: List<Book>) {
        val arr = JSONArray().apply { list.forEach { put(it.toJson()) } }
        val tmp = File(file.parentFile, "library.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }

    fun get(id: String): Book? = _books.value.firstOrNull { it.id == id }

    @Synchronized
    fun update(id: String, transform: (Book) -> Book) {
        val list = _books.value.map { if (it.id == id) transform(it) else it }
        _books.value = list
        persist(list)
    }

    @Synchronized
    fun add(book: Book) {
        val list = _books.value.filterNot { it.id == book.id } + book
        _books.value = list
        persist(list)
    }

    fun saveProgress(id: String, page: Int) = update(id) {
        it.copy(
            lastPage = page,
            lastReadAt = System.currentTimeMillis(),
            finished = it.finished || page >= it.pageCount - 1,
        )
    }

    fun setFinished(id: String, finished: Boolean) = update(id) {
        if (finished) it.copy(finished = true, lastPage = it.pageCount - 1, lastReadAt = maxOf(it.lastReadAt, System.currentTimeMillis()))
        else it.copy(finished = false, lastPage = 0, lastReadAt = 0L)
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val book = get(id) ?: return@withContext
        if (book.kind != BookKind.FOLDER) File(book.location).delete()
        book.coverPath?.let { File(it).delete() }
        synchronized(this@LibraryStore) {
            val list = _books.value.filterNot { it.id == id }
            _books.value = list
            persist(list)
        }
    }
}
