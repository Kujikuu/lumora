package com.iptvcinema.tv.features.home.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.iptvcinema.tv.core.model.MovieItem
import com.iptvcinema.tv.core.model.home.HomeContentCard
import com.iptvcinema.tv.features.home.HomeSpotlight
import com.iptvcinema.tv.features.home.HomeSpotlightMapper

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
