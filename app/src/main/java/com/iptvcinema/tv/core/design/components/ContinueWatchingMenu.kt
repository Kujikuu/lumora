package com.iptvcinema.tv.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.model.home.HomeContentCard

object ContinueWatchingMenuOptionId {
    const val VIEW_DETAILS = "view_details"
    const val REMOVE = "remove"
}

@Composable
fun continueWatchingMenuOptions(card: HomeContentCard): List<CinemaMenuOption> = buildList {
    when (card.contentType) {
        "episode" -> {
            if (!card.seriesId.isNullOrBlank()) {
                add(
                    CinemaMenuOption(
                        id = ContinueWatchingMenuOptionId.VIEW_DETAILS,
                        label = stringResource(R.string.continue_watching_menu_view_series),
                    ),
                )
            }
        }
        "movie" -> {
            add(
                CinemaMenuOption(
                    id = ContinueWatchingMenuOptionId.VIEW_DETAILS,
                    label = stringResource(R.string.continue_watching_menu_view_movie),
                ),
            )
        }
    }
    add(
        CinemaMenuOption(
            id = ContinueWatchingMenuOptionId.REMOVE,
            label = stringResource(R.string.continue_watching_menu_remove),
            destructive = true,
        ),
    )
}

@Composable
fun ContinueWatchingMenuDialog(
    card: HomeContentCard?,
    onDismiss: () -> Unit,
    onViewDetails: (HomeContentCard) -> Unit,
    onRemove: (HomeContentCard) -> Unit,
) {
    if (card == null) return
    val options = continueWatchingMenuOptions(card)
    CinemaContextMenuDialog(
        title = card.title,
        options = options,
        onDismiss = onDismiss,
        onOptionSelected = { option ->
            when (option.id) {
                ContinueWatchingMenuOptionId.VIEW_DETAILS -> onViewDetails(card)
                ContinueWatchingMenuOptionId.REMOVE -> onRemove(card)
            }
            onDismiss()
        },
    )
}
