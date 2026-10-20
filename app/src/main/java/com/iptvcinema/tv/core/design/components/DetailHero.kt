package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.util.isolateDirection

private val DetailPosterInsetWidth = 120.dp
/** Start inset of hero text on full-screen detail pages (no nav rail). */
val DetailHeroContentStart = 48.dp
private val DetailBackTop = 22.dp
private val ChannelLogoSize = 150.dp
private val PrimaryProgressTrackFocused = Color(0xFFD6D8DC)

/** How a badge in the hero's badge row is drawn. */
enum class HeroBadgeStyle {
    /** Translucent white fill: what the title is (MOVIE, SERIES, a category). */
    Overlay,

    /** Accent fill: news worth shouting (NEW EPISODE, 4K). */
    Accent,

    /** Thin outline, no fill: quality labels (HD). */
    Outline,

    /** Red pill with a white dot. */
    Live,
}

data class HeroBadge(
    val text: String,
    val style: HeroBadgeStyle = HeroBadgeStyle.Overlay,
)

/** What sits behind the hero text. */
enum class DetailBackdropStyle {
    /** Artwork cropped to fill the whole hero. */
    FullBleed,

    /** A channel logo, fitted on a dark panel at the top end, so small logos stay sharp. */
    Logo,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailHero(
    title: String,
    metadata: List<String>,
    synopsis: String,
    onWatchNow: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier,
    primaryActionLabel: String = "",
    watchLaterLabel: String = "",
    showTrailer: Boolean = false,
    onTrailer: (() -> Unit)? = null,
    onChooseEpisode: (() -> Unit)? = null,
    onMoreLikeThis: (() -> Unit)? = null,
    showScrollHint: Boolean = false,
    backdropUrl: String? = null,
    posterUrl: String? = null,
    showPosterInset: Boolean = false,
    watchNowFocusRequester: FocusRequester? = null,
    badges: List<HeroBadge> = emptyList(),
    backLabel: String? = null,
    contentStart: Dp = DetailHeroContentStart,
    primaryProgress: Float? = null,
    backdropStyle: DetailBackdropStyle = DetailBackdropStyle.FullBleed,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val resolvedPrimaryActionLabel = primaryActionLabel.ifBlank {
        stringResource(R.string.btn_watch_now)
    }
    val resolvedWatchLaterLabel = watchLaterLabel.ifBlank {
        stringResource(R.string.btn_watch_later)
    }
    val hasPosterInset = showPosterInset && !posterUrl.isNullOrBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CinemaSpacing.HeroMinHeight),
    ) {
        when (backdropStyle) {
            DetailBackdropStyle.FullBleed -> CinemaAsyncImage(
                imageUrl = backdropUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                fallbackLabel = title,
            )
            DetailBackdropStyle.Logo -> Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(0.58f)
                    .fillMaxHeight(0.7f)
                    .background(CinemaColors.SurfaceSoft),
                contentAlignment = Alignment.Center,
            ) {
                CinemaAsyncImage(
                    imageUrl = backdropUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(ChannelLogoSize)
                        .clip(CinemaShapes.XLarge),
                    contentScale = ContentScale.Fit,
                    fallbackLabel = title,
                )
            }
        }

        DetailHeroScrim(isRtl = isRtl, modifier = Modifier.matchParentSize())

        if (backLabel != null) {
            BackPill(
                text = backLabel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = contentStart, top = DetailBackTop),
            )
        }

        if (hasPosterInset) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = contentStart, bottom = 20.dp)
                    .width(DetailPosterInsetWidth)
                    .height(DetailPosterInsetWidth * 1.5f)
                    .clip(CinemaShapes.Card)
                    .background(CinemaColors.Surface),
            ) {
                CinemaAsyncImage(
                    imageUrl = posterUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    fallbackLabel = title,
                )
            }
        }

        // The text block keeps a readable width over the artwork; the buttons get the full width
        // so a third or fourth button never squeezes. The top padding keeps the block below the
        // Back pill however long the title and plot are.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = contentStart + if (hasPosterInset) DetailPosterInsetWidth + 16.dp else 0.dp,
                    end = 40.dp,
                    top = if (backLabel != null) DetailBackTop + 48.dp else 24.dp,
                    bottom = if (showScrollHint) 36.dp else 28.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (badges.isNotEmpty()) {
                HeroBadgeRow(badges = badges)
            }
            Text(
                modifier = Modifier.fillMaxWidth(DetailTextWidthFraction),
                text = title.uppercase().isolateDirection(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (metadata.isNotEmpty()) {
                MetadataRow(items = metadata.take(5), modifier = Modifier.fillMaxWidth(DetailTextWidthFraction))
            }
            headerContent?.invoke(this)
            if (synopsis.isNotBlank()) {
                Text(
                    modifier = Modifier.fillMaxWidth(DetailTextWidthFraction),
                    text = synopsis.isolateDirection(),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryHeroButton(
                    text = resolvedPrimaryActionLabel,
                    progress = primaryProgress,
                    onClick = onWatchNow,
                    focusRequester = watchNowFocusRequester,
                )
                if (onChooseEpisode != null) {
                    CinemaButton(
                        text = stringResource(R.string.btn_choose_episode),
                        variant = CinemaButtonVariant.SecondaryDark,
                        icon = Icons.Default.ViewAgenda,
                        onClick = onChooseEpisode,
                    )
                }
                if (onMoreLikeThis != null) {
                    CinemaButton(
                        text = stringResource(R.string.btn_more_like_this),
                        variant = CinemaButtonVariant.SecondaryDark,
                        icon = Icons.Default.ViewCarousel,
                        onClick = onMoreLikeThis,
                    )
                }
                if (showTrailer && onTrailer != null) {
                    CinemaButton(
                        text = "",
                        variant = CinemaButtonVariant.Icon,
                        icon = Icons.Default.Videocam,
                        onClick = onTrailer,
                        contentDescription = stringResource(R.string.btn_trailer),
                    )
                }
                CinemaButton(
                    text = resolvedWatchLaterLabel,
                    variant = CinemaButtonVariant.SecondaryDark,
                    icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    onClick = onFavorite,
                )
            }
        }
        if (showScrollHint) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = CinemaColors.TextSecondary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(28.dp),
            )
        }
    }
}

/**
 * Darkens the start side (behind the text) and the bottom (behind the buttons) in one draw
 * pass, so the artwork stays bright at the top end.
 */
@Composable
private fun DetailHeroScrim(isRtl: Boolean, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val sideWidth = size.width * 0.67f
            val side = Brush.horizontalGradient(
                0f to CinemaColors.Background,
                0.5f to CinemaColors.Background.copy(alpha = 0.85f),
                1f to Color.Transparent,
                startX = if (isRtl) size.width else 0f,
                endX = if (isRtl) size.width - sideWidth else sideWidth,
            )
            val bottom = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to CinemaColors.Background.copy(alpha = 0.92f),
                1f to CinemaColors.Background,
                startY = size.height * 0.48f,
                endY = size.height,
            )
            onDrawBehind {
                drawRect(side)
                drawRect(bottom)
            }
        },
    )
}

@Composable
fun HeroBadgeRow(
    badges: List<HeroBadge>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        badges.forEach { badge ->
            when (badge.style) {
                HeroBadgeStyle.Overlay -> BadgeChip(text = badge.text, backgroundColor = CinemaColors.OverlayFill)
                HeroBadgeStyle.Accent -> BadgeChip(text = badge.text, backgroundColor = CinemaColors.Accent)
                HeroBadgeStyle.Outline -> OutlineBadge(text = badge.text)
                HeroBadgeStyle.Live -> LiveDotBadge(text = badge.text)
            }
        }
    }
}

/**
 * The hero's main button. With [progress] (a title already started) a thin bar sits inside the
 * pill under the label: white on the accent pill, accent once the pill turns white on focus.
 */
@Composable
private fun PrimaryHeroButton(
    text: String,
    progress: Float?,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
) {
    var focused by remember { mutableStateOf(false) }
    Box(modifier = Modifier.onFocusChanged { focused = it.hasFocus }) {
        CinemaButton(
            text = text,
            variant = CinemaButtonVariant.PrimaryAccent,
            icon = Icons.Default.PlayArrow,
            onClick = onClick,
            modifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier,
        )
        if (progress != null && progress > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                InsetProgressBar(
                    fraction = progress,
                    modifier = Modifier.fillMaxWidth(),
                    height = 3.dp,
                    trackColor = if (focused) PrimaryProgressTrackFocused else CinemaColors.White.copy(alpha = 0.35f),
                    fillColor = if (focused) CinemaColors.Accent else CinemaColors.White,
                )
            }
        }
    }
}

/** Width of the hero's text block, as a share of the screen, so lines stay readable. */
private const val DetailTextWidthFraction = 0.62f
