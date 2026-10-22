package com.iptvcinema.tv.core.tvhome.launcher

import com.iptvcinema.tv.core.tvhome.HomeChannelSpec
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherSnapshotStoreTest {
    private val dir: File = Files.createTempDirectory("rows").toFile()

    @Test
    fun writeThenRead_returnsTheSameSnapshot() {
        val store = LauncherSnapshotStore(dir)
        val snapshot = LauncherRowsSnapshot(7, listOf(HomeChannelSpec("new", "New", emptyList())))
        store.write(snapshot)
        assertEquals(snapshot, store.read())
        assertEquals(listOf("rows.json"), dir.list()!!.toList())
    }

    @Test
    fun read_isNullWhenMissingOrCorrupt() {
        val store = LauncherSnapshotStore(dir)
        assertNull(store.read())
        File(dir, "rows.json").writeText("{")
        assertNull(store.read())
        store.clear()
        assertNull(store.read())
    }
}
