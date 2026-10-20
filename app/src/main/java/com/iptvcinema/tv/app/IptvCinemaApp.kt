package com.iptvcinema.tv.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore
import com.iptvcinema.tv.core.data.repository.AuthRepository
import com.iptvcinema.tv.core.di.ApplicationScope
import com.iptvcinema.tv.core.sync.CloudSyncInitializer
import com.iptvcinema.tv.core.tvhome.WatchNextSync
import com.iptvcinema.tv.core.util.safeSummary
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class IptvCinemaApp : Application(), ImageLoaderFactory, Configuration.Provider {
    // Lazy: nothing heavy is built in onCreate, which runs on the main thread before the
    // first frame. Each one is built where and when it is first needed.
    @Inject lateinit var imageLoader: Lazy<ImageLoader>
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var cloudSyncInitializer: Lazy<CloudSyncInitializer>
    @Inject lateinit var watchNextSync: Lazy<WatchNextSync>
    @Inject lateinit var authRepository: Lazy<AuthRepository>
    @Inject lateinit var localCredentialsStore: Lazy<LocalCredentialsStore>
    @Inject lateinit var firstFrameSignal: FirstFrameSignal

    @Inject
    @field:ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        installUncaughtExceptionGuard()
        startDeferredInit()
    }

    override fun newImageLoader(): ImageLoader = imageLoader.get()

    private fun startDeferredInit() {
        applicationScope.launch {
            // Building the auth repository builds the Supabase client, which starts loading the
            // saved session. Doing it now, beside the first screen, gets auth ready sooner and
            // keeps the client (Ktor, Auth, Realtime) and the Keystore off the main thread.
            warmUp("auth") { authRepository.get() }
            warmUp("credentials") { localCredentialsStore.get().warmUp() }
            // Sync scheduling, realtime and Watch Next compete with the first screen for CPU
            // and disk, so they start once it has drawn.
            firstFrameSignal.await(FIRST_FRAME_TIMEOUT_MS)
            // Ready before Home asks for its first poster, so the main thread never builds it.
            warmUp("images") { imageLoader.get() }
            cloudSyncInitializer.get()
            watchNextSync.get()
        }
    }

    // A failed warm-up is not fatal: the same object is built again on first real use.
    private suspend fun warmUp(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Start-up warm-up '$name' failed: ${error.safeSummary()}")
        }
    }

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
        private const val FIRST_FRAME_TIMEOUT_MS = 5_000L
    }
}
