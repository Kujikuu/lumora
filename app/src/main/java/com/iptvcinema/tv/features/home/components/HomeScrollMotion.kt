package com.iptvcinema.tv.features.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import com.iptvcinema.tv.core.design.components.TvScrollMotion

private val glideTween = tween<Float>(
    durationMillis = TvScrollMotion.HORIZONTAL_MS,
    easing = FastOutSlowInEasing,
)

/**
 * Glides the list so item [index] sits exactly at the start of the content area.
 *
 * Item offsets are measured from the content padding edge, so an aligned item has offset 0;
 * scrolling by the offset itself is the whole distance. Home turns off focus bring-into-view,
 * so this glide is the only thing that moves Home's lists.
 */
suspend fun LazyListState.glideItemToStart(index: Int) {
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo == null) {
        animateScrollToItem(index)
        return
    }
    val delta = itemInfo.offset.toFloat()
    if (delta == 0f) return
    animateScrollBy(delta, glideTween)
}
