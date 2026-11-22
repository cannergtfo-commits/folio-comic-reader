package net.blazarforce.folio.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme { INK, PAPER }
enum class ReadingMode { PAGED, SPREAD, WEBTOON }
enum class PageTurn { SLIDE, FADE, NONE }

data class Settings(
    val theme: AppTheme = AppTheme.INK,
    val readingMode: ReadingMode = ReadingMode.PAGED,
    val autoSpreadLandscape: Boolean = true,
    val pageTurn: PageTurn = PageTurn.SLIDE,
    val haptics: Boolean = true,
    /** 0..1 amber "night paper" filter. */
    val warmth: Float = 0f,
    /** Window brightness 0.05..1, or -1 to follow the system. */
    val brightness: Float = -1f,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("folio", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private fun read() = Settings(
        theme = runCatching { AppTheme.valueOf(sp.getString("theme", "INK")!!) }.getOrDefault(AppTheme.INK),
        readingMode = runCatching { ReadingMode.valueOf(sp.getString("mode", "PAGED")!!) }.getOrDefault(ReadingMode.PAGED),
        autoSpreadLandscape = sp.getBoolean("autoSpread", true),
        pageTurn = runCatching { PageTurn.valueOf(sp.getString("turn", "SLIDE")!!) }.getOrDefault(PageTurn.SLIDE),
        haptics = sp.getBoolean("haptics", true),
        warmth = sp.getFloat("warmth", 0f),
        brightness = sp.getFloat("brightness", -1f),
    )

    fun update(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        _settings.value = s
        sp.edit()
            .putString("theme", s.theme.name).putString("mode", s.readingMode.name)
            .putBoolean("autoSpread", s.autoSpreadLandscape).putString("turn", s.pageTurn.name)
            .putBoolean("haptics", s.haptics).putFloat("warmth", s.warmth)
            .putFloat("brightness", s.brightness).apply()
    }
}
