package net.blazarforce.folio.comic

import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Screen-sized page decoding with an LRU memory cache and neighbour preloading.
 * One loader for full pages and one (smaller target) for scrubber thumbnails.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PageLoader(
    private val source: PageSource,
    private val targetWidth: Int,
    private val targetHeight: Int,
    private val scope: CoroutineScope,
    cacheFraction: Int = 5,
) {
    private val io = Dispatchers.IO.limitedParallelism(2)
    private val cache = object : LruCache<Int, Bitmap>(
        (Runtime.getRuntime().maxMemory() / cacheFraction).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
    ) {
        override fun sizeOf(key: Int, value: Bitmap) = value.allocationByteCount
    }
    private val inflight = HashMap<Int, Deferred<Bitmap?>>()
    private val mutex = Mutex()

    val pageCount get() = source.pageCount

    fun peek(index: Int): Bitmap? = cache.get(index)

    fun aspect(index: Int): Float? = source.aspect(index)

    suspend fun load(index: Int): Bitmap? {
        if (index !in 0 until source.pageCount) return null
        cache.get(index)?.let { return it }
        val job = mutex.withLock {
            inflight[index] ?: scope.async(io) {
                source.decode(index, targetWidth, targetHeight)?.also { cache.put(index, it) }
            }.also { inflight[index] = it }
        }
        return try {
            job.await()
        } finally {
            mutex.withLock { if (inflight[index] === job && job.isCompleted) inflight.remove(index) }
        }
    }

    /** Warms the cache around [center]: center first, then ±1, ±2. */
    fun preload(center: Int, radius: Int = 2) {
        scope.launch {
            for (d in 0..radius) {
                for (i in if (d == 0) listOf(center) else listOf(center + d, center - d)) {
                    if (i in 0 until source.pageCount && cache.get(i) == null) load(i)
                }
            }
        }
    }

    fun clear() = cache.evictAll()
}
