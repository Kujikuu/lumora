package com.iptvcinema.tv.core.epg

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LatestPerSourceJobRunner(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val guard = Any()
    private val jobs = mutableMapOf<String, Job>()
    private val sourceLocks = ConcurrentHashMap<String, Mutex>()

    fun launch(sourceId: String, block: suspend CoroutineScope.() -> Unit): Job {
        val sourceLock = sourceLocks.computeIfAbsent(sourceId) { Mutex() }
        val job = scope.launch(dispatcher, start = CoroutineStart.LAZY) {
            sourceLock.withLock { block() }
        }
        val previous = synchronized(guard) {
            jobs.put(sourceId, job).also {
                job.invokeOnCompletion {
                    synchronized(guard) {
                        if (jobs[sourceId] === job) jobs.remove(sourceId)
                    }
                }
            }
        }
        previous?.cancel()
        job.start()
        return job
    }

    suspend fun cancelAndJoin(sourceId: String) {
        val sourceLock = sourceLocks.computeIfAbsent(sourceId) { Mutex() }
        val job = synchronized(guard) { jobs.remove(sourceId) }
        job?.cancel()
        sourceLock.withLock { }
        job?.join()
    }

    suspend fun awaitIdle(sourceId: String) {
        while (true) {
            val job = synchronized(guard) { jobs[sourceId] } ?: return
            job.join()
            if (synchronized(guard) { jobs[sourceId] } == null) return
        }
    }
}
