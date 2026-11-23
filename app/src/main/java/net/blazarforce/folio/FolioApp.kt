package net.blazarforce.folio

import android.app.Application
import android.content.Context
import net.blazarforce.folio.data.CatalogRepo
import net.blazarforce.folio.data.Importer
import net.blazarforce.folio.data.LibraryStore
import net.blazarforce.folio.data.Prefs

class FolioApp : Application() {
    val store by lazy { LibraryStore(this) }
    val prefs by lazy { Prefs(this) }
    val importer by lazy { Importer(this, store) }
    val catalog by lazy { CatalogRepo(this, store, importer) }
}

val Context.folio: FolioApp get() = applicationContext as FolioApp
