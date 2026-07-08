package com.iptvcinema.tv.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.iptvcinema.tv.core.sync.CloudSyncInitializer
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.CancellationException

@HiltAndroidApp
class IptvCinemaApp : Application(), ImageLoaderFactory, Configuration.Provider {
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var cloudSyncInitializer: CloudSyncInitializer

    override fun onCreate() {
        super.onCreate()
        installUncaughtExceptionGuard()
        cloudSyncInitializer.hashCode()
    }

    override fun newImageLoader(): ImageLoader = imageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun installUncaughtExceptionGuard() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on ${thread.name}", throwable)
            if (throwable.isKnownNetworkTeardownFailure()) {
                return@setDefaultUncaughtExceptionHandler
            }
            previousHandler?.uncaughtException(thread, throwable) ?: run {
                android.os.Process.killProcess(android.os.Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun Throwable.isKnownNetworkTeardownFailure(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is IOException ||
                current is SocketTimeoutException ||
                current is UnknownHostException ||
                current is CancellationException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    companion object {
        private const val TAG = "IptvCinemaApp"
    }
}
