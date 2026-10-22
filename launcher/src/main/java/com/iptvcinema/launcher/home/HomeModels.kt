package com.iptvcinema.launcher.home

import com.iptvcinema.launcher.inputs.InputDescriptor

enum class RowKey { Continue, Favorites, Live, New, Apps, Inputs }

enum class RowStyle { Landscape, Channel, Poster, App, Input }

sealed interface CardAction {
    data class OpenLink(val intentUri: String) : CardAction
    data class OpenApp(val packageName: String, val isSystem: Boolean) : CardAction
    data class OpenInput(val input: InputDescriptor) : CardAction
    data object OpenInputPicker : CardAction
    data object OpenMainApp : CardAction
    data object InstallMainApp : CardAction
}

enum class CardBadge { Connected, Disconnected }

data class HomeCard(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    /** URL or file URI for Coil; null draws the title on a surface tile. */
    val image: String? = null,
    val progress: Float? = null,
    val action: CardAction,
    val isPlaceholder: Boolean = false,
    val badge: CardBadge? = null,
    /** Short type mark for input tiles ("HDMI"). */
    val mark: String? = null,
)

data class HomeRow(val key: RowKey, val style: RowStyle, val cards: List<HomeCard>)

/** Placeholder texts, resolved by the screen in the current language. */
data class HomeLabels(
    val openApp: String,
    val installApp: String,
    val emptyRow: String,
    val noInputs: String,
    val inputTypeNames: Map<com.iptvcinema.launcher.inputs.InputType, String> = emptyMap(),
)
