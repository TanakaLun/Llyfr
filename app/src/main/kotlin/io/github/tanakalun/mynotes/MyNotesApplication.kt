package io.github.tanakalun.mynotes

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import io.github.tanakalun.mynotes.core.AppContainer
import io.github.tanakalun.mynotes.core.PowerSaveModeTracker
import io.github.tanakalun.mynotes.data.settings.SettingsStore

class MyNotesApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        AppContainer(this).also { container = it }
        SettingsStore.init(this)
        PowerSaveModeTracker.init(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}