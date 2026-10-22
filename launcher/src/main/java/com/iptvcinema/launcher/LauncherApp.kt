package com.iptvcinema.launcher

import android.app.Application
import android.content.ComponentCallbacks2
import coil.ImageLoader
import coil.ImageLoaderFactory

class LauncherApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Nothing else runs here: the first frame should not wait on anything.
        LauncherGraph.init(this)
    }

    override fun newImageLoader(): ImageLoader = LauncherGraph.imageLoader

    // A home screen must survive memory pressure; bitmaps are the only thing worth dropping and
    // they reload from the disk cache when needed.
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) LauncherGraph.dropImageMemory()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LauncherGraph.dropImageMemory()
    }
}
