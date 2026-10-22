package com.iptvcinema.tv.core.tvhome.launcher

import java.io.File

/**
 * The rows snapshot on disk. Written whole then renamed, so a reader (the provider, on a binder
 * thread) never sees a half-written file. Plain files, so it can be unit tested with a temp dir.
 */
class LauncherSnapshotStore(private val directory: File) {
    private val file = File(directory, LauncherRowsContract.SNAPSHOT_FILE)

    fun write(snapshot: LauncherRowsSnapshot) {
        directory.mkdirs()
        val temp = File(directory, "${LauncherRowsContract.SNAPSHOT_FILE}.tmp")
        temp.writeText(LauncherRowsCodec.encode(snapshot))
        if (!temp.renameTo(file)) {
            // Rename over an existing file can fail on some file systems; fall back to copy.
            file.writeText(temp.readText())
            temp.delete()
        }
    }

    fun read(): LauncherRowsSnapshot? {
        if (!file.isFile) return null
        return runCatching { LauncherRowsCodec.decode(file.readText()) }.getOrNull()
    }

    fun clear() {
        file.delete()
    }
}
