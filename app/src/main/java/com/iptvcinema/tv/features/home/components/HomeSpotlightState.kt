package com.iptvcinema.tv.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeSpotlight
import com.iptvcinema.tv.features.home.HomeSpotlightMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * What the spotlight shows: the current hero slide, or the focused rail card.
 *
 * Only the backdrop and the panel read this state, so moving focus between cards recomposes
 * those two and nothing else.
 */
@Stable
class HomeSpotlightState(initialHeroIndex: Int = 0) {
    var heroIndex by mutableIntStateOf(initialHeroIndex)
    var focusedCard by mutableStateOf<HomeContentCard?>(null)
        private set
    var heroFocused by mutableStateOf(false)

    val isOnHero: Boolean get() = focusedCard == null

    fun showHero() {
        focusedCard = null
    }

    fun showCard(card: HomeContentCard) {
        focusedCard = card
    }

    fun current(hero: List<MovieItem>): HomeSpotlight? =
        focusedCard?.let(HomeSpotlightMapper::fromCard)
            ?: hero.getOrNull(heroIndex.coerceIn(0, (hero.size - 1).coerceAtLeast(0)))
                ?.let(HomeSpotlightMapper::fromHero)

    companion object {
        val Saver: Saver<HomeSpotlightState, Int> = Saver(
            save = { it.heroIndex },
            restore = { HomeSpotlightState(it) },
        )
    }
}

@Composable
fun rememberHomeSpotlightState(): HomeSpotlightState =
    rememberSaveable(saver = HomeSpotlightState.Saver) { HomeSpotlightState() }

/** How long focus must rest on a card before the spotlight text follows it. */
internal const val SPOTLIGHT_SETTLE_MS = 150L

/**
 * Wait before showing the next spotlight. A card waits for focus to rest, so a burst of D-pad
 * presses costs one text layout instead of one per card. The hero, and the very first
 * spotlight, show at once.
 */
internal fun spotlightSettleDelayMs(hasShown: Boolean, targetIsCard: Boolean): Long =
    if (hasShown && targetIsCard) SPOTLIGHT_SETTLE_MS else 0L

/**
 * The spotlight to draw: [HomeSpotlightState.current], once focus has settled. It is collected
 * in an effect instead of read during composition, so a focus move does not recompose the caller.
 */
@Composable
internal fun rememberSettledSpotlight(state: HomeSpotlightState, hero: List<MovieItem>): HomeSpotlight? {
    val currentHero by rememberUpdatedState(hero)
    var shown by remember(state) {
        mutableStateOf(Snapshot.withoutReadObservation { state.current(hero) })
    }
    LaunchedEffect(state) {
        snapshotFlow { state.current(currentHero) to !state.isOnHero }
            .collectLatest { (target, isCard) ->
                delay(spotlightSettleDelayMs(hasShown = shown != null, targetIsCard = isCard))
                shown = target
            }
    }
    return shown
}

/** Home layout sizes. The screen is split into a fixed spotlight area and a rails area. */
internal object HomeDimens {
    const val SPOTLIGHT_WEIGHT = 0.54f
    const val RAILS_WEIGHT = 1f - SPOTLIGHT_WEIGHT

    val PosterWidth = 104.dp
    val PosterHeight = 156.dp
    val LandscapeWidth = 208.dp
    val LandscapeHeight = 117.dp
    val CardGap = 12.dp
    val RailTitleGap = 8.dp
    val FocusOverflow = 12.dp
    val SpotlightTextHeight = 200.dp
}
