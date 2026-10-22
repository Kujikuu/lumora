package com.iptvcinema.tv.core.tvhome.launcher

import com.iptvcinema.tv.core.tvhome.launcher.LauncherRowsCodec.wireName

/**
 * Turns a snapshot into cursor rows, one per card, in [LauncherRowsContract.COLUMNS] order.
 * Pure so the column layout is unit tested; the provider wraps the result in a MatrixCursor.
 */
object LauncherRowsCursor {
    fun rowsOf(snapshot: LauncherRowsSnapshot): List<Array<Any?>> = snapshot.rows.flatMap { row ->
        row.programs.map { card ->
            arrayOf(
                row.key,
                row.displayName,
                card.key,
                card.kind.wireName(),
                card.title,
                card.description,
                card.posterUrl,
                if (card.isWidePoster) 1 else 0,
                card.progress?.toDouble(),
                card.validUntilMs,
                card.intentUri,
                snapshot.generatedAtMs,
            )
        }
    }
}
