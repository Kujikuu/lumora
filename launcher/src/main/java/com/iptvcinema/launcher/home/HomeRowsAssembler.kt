package com.iptvcinema.launcher.home

import com.iptvcinema.launcher.apps.AppCard
import com.iptvcinema.launcher.content.ContentCard
import com.iptvcinema.launcher.content.ContentState
import com.iptvcinema.launcher.content.LauncherRowsContract
import com.iptvcinema.launcher.inputs.InputDescriptor
import com.iptvcinema.launcher.inputs.InputState

/**
 * Turns what the repositories know into the six home rows, always in the same order and never
 * empty: a row with nothing to show carries one placeholder card, so the layout and the D-pad
 * path never change. Pure, so every rule here is unit tested.
 */
object HomeRowsAssembler {
    const val PLACEHOLDER_KEY = "placeholder"

    fun assemble(
        content: ContentState,
        apps: List<AppCard>?,
        inputs: List<InputDescriptor>?,
        nowMs: Long,
        labels: HomeLabels,
    ): List<HomeRow> = listOf(
        contentRow(RowKey.Continue, RowStyle.Landscape, LauncherRowsContract.ROW_CONTINUE, content, nowMs, labels),
        contentRow(RowKey.Favorites, RowStyle.Channel, LauncherRowsContract.ROW_FAVORITES, content, nowMs, labels),
        contentRow(RowKey.Live, RowStyle.Channel, LauncherRowsContract.ROW_LIVE, content, nowMs, labels),
        contentRow(RowKey.New, RowStyle.Poster, LauncherRowsContract.ROW_NEW, content, nowMs, labels),
        appsRow(apps, labels),
        inputsRow(inputs, labels),
    )

    private fun contentRow(
        key: RowKey,
        style: RowStyle,
        rowKey: String,
        content: ContentState,
        nowMs: Long,
        labels: HomeLabels,
    ): HomeRow {
        val cards = when {
            !content.appInstalled -> listOf(placeholder(labels.installApp, CardAction.InstallMainApp))
            content.snapshot == null -> listOf(placeholder(labels.openApp, CardAction.OpenMainApp))
            else -> content.snapshot.row(rowKey)?.cards.orEmpty()
                .map { it.toCard(nowMs) }
                .ifEmpty { listOf(placeholder(labels.emptyRow, CardAction.OpenMainApp)) }
        }
        return HomeRow(key, style, cards)
    }

    private fun ContentCard.toCard(nowMs: Long) = HomeCard(
        key = key,
        title = title,
        // A programme that has ended must not be shown as "on now".
        subtitle = subtitle?.takeIf { validUntilMs == null || validUntilMs > nowMs },
        image = imageUrl,
        progress = progress,
        action = CardAction.OpenLink(intentUri),
    )

    private fun appsRow(apps: List<AppCard>?, labels: HomeLabels): HomeRow {
        val cards = apps.orEmpty().map { app ->
            HomeCard(
                key = app.entry.packageName,
                title = app.entry.label,
                image = app.artwork?.toURI()?.toString(),
                action = CardAction.OpenApp(app.entry.packageName, app.entry.isSystem),
            )
        }.ifEmpty { listOf(placeholder(labels.emptyRow, CardAction.OpenMainApp)) }
        return HomeRow(RowKey.Apps, RowStyle.App, cards)
    }

    private fun inputsRow(inputs: List<InputDescriptor>?, labels: HomeLabels): HomeRow {
        val cards = inputs.orEmpty().map { input ->
            HomeCard(
                key = input.id,
                title = input.label,
                mark = labels.inputTypeNames[input.type] ?: input.type.name,
                badge = when (input.state) {
                    InputState.Connected, InputState.Standby -> CardBadge.Connected
                    InputState.Disconnected -> CardBadge.Disconnected
                    InputState.Unknown -> null
                },
                action = CardAction.OpenInput(input),
            )
        }.ifEmpty { listOf(placeholder(labels.noInputs, CardAction.OpenInputPicker)) }
        return HomeRow(RowKey.Inputs, RowStyle.Input, cards)
    }

    private fun placeholder(label: String, action: CardAction) =
        HomeCard(key = PLACEHOLDER_KEY, title = label, action = action, isPlaceholder = true)
}
