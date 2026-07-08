package com.iptvcinema.tv.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.AccountAvatar
import com.iptvcinema.tv.core.design.components.CinemaAsyncImage
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.SkeletonProfileRow
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.ProfileSelectionMode
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

private val AccountRowShape = RoundedCornerShape(0.dp)
private val PrimaryAvatarSize = 160.dp
private val SecondaryAvatarSize = 150.dp
private val SettingsButtonSize = 52.dp

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    mode: ProfileSelectionMode = ProfileSelectionMode.Onboarding,
    currentProfileId: String? = null,
    profilesUiState: ProfilesUiState,
    onProfileSelected: (profileId: String) -> Unit,
    onManageProfiles: () -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val profileFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("profile_selection")

    PopBackHandler(onBack = onBack)

    when (profilesUiState) {
        ProfilesUiState.Loading -> {
            CinemaScreen(showTopNav = false) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    SkeletonProfileRow(count = 4)
                }
            }
        }
        is ProfilesUiState.Error -> {
            CinemaScreen(showTopNav = false) {
                EmptyState(
                    title = stringResource(R.string.profile_load_error),
                    description = profilesUiState.message,
                    primaryAction = stringResource(R.string.btn_retry),
                    secondaryAction = null,
                    onPrimary = onRetry,
                    onSecondary = null,
                )
            }
        }
        is ProfilesUiState.Ready -> {
            ProfileSelectionContent(
                mode = mode,
                profiles = profilesUiState.profiles,
                currentProfileId = currentProfileId,
                profileFocus = profileFocus,
                focusState = focusState,
                onProfileSelected = onProfileSelected,
                onManageProfiles = onManageProfiles,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileSelectionContent(
    mode: ProfileSelectionMode,
    profiles: List<UserProfile>,
    currentProfileId: String?,
    profileFocus: FocusRequester,
    focusState: com.iptvcinema.tv.core.navigation.ScreenFocusState,
    onProfileSelected: (profileId: String) -> Unit,
    onManageProfiles: () -> Unit,
) {
    val currentProfileIndex = profiles.indexOfFirst { it.id == currentProfileId }.takeIf { it >= 0 } ?: 0
    var selectedProfileIndex by remember(currentProfileId, focusState.focusIndex, profiles) {
        mutableIntStateOf(
            if (focusState.hasSavedFocus) {
                focusState.focusIndex.coerceIn(0, profiles.lastIndex.coerceAtLeast(0))
            } else {
                currentProfileIndex
            },
        )
    }

    LaunchedEffect(focusState.hasSavedFocus, selectedProfileIndex) {
        if (profiles.isEmpty()) return@LaunchedEffect
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(profileFocus)
        } else {
            focusState.requestInitialFocus(profileFocus)
            focusState.saveFocusIndex(selectedProfileIndex)
        }
    }

    CinemaScreen(showTopNav = false) {
        val heroImage = remember {
            FakeDataProvider.movies.firstOrNull { !it.imageUrl.isNullOrBlank() }?.imageUrl
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
        ) {
            CinemaAsyncImage(
                imageUrl = heroImage,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(1320.dp),
                contentScale = ContentScale.Crop,
                fallbackLabel = "",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                CinemaColors.Background,
                                CinemaColors.Background,
                                CinemaColors.Background.copy(alpha = 0.92f),
                                CinemaColors.Background.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 88.dp, top = 96.dp)
                    .width(720.dp),
                verticalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Text(
                    text = if (mode == ProfileSelectionMode.SwitchProfile) {
                        stringResource(R.string.profile_switch)
                    } else {
                        stringResource(R.string.profile_title)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = CinemaColors.TextMuted,
                    ),
                )
                if (profiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_none_found),
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                    )
                } else {
                    val primaryProfile = profiles.find { it.id == currentProfileId } ?: profiles.first()
                    val otherProfiles = profiles.filterNot { it.id == primaryProfile.id }

                    Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                        PrimaryAccountRow(
                            name = primaryProfile.name,
                            subtitle = stringResource(R.string.profile_role_administrator),
                            modifier = Modifier.focusRequester(profileFocus),
                            onClick = {
                                focusState.saveFocusIndex(selectedProfileIndex)
                                onProfileSelected(primaryProfile.id)
                            },
                            onSettingsClick = onManageProfiles,
                        )

                        if (mode == ProfileSelectionMode.SwitchProfile && otherProfiles.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                                otherProfiles.forEach { profile ->
                                    AccountRow(
                                        name = profile.name,
                                        subtitle = stringResource(R.string.profile_role_administrator),
                                        avatarVariant = AccountAvatarVariant.Other,
                                        onClick = {
                                            val actualIndex = profiles.indexOfFirst { it.id == profile.id }
                                            selectedProfileIndex = actualIndex
                                            focusState.saveFocusIndex(actualIndex)
                                            onProfileSelected(profile.id)
                                        },
                                    )
                                }
                            }
                        }

                        OtherAccountsRow(onClick = onManageProfiles)
                    }
                }
            }
        }
    }
}

private enum class AccountAvatarVariant {
    Primary,
    Other,
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PrimaryAccountRow(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AccountRow(
            name = name,
            subtitle = subtitle,
            avatarVariant = AccountAvatarVariant.Primary,
            onClick = onClick,
        )
        SettingsIconButton(
            modifier = Modifier.padding(top = 12.dp),
            onClick = onSettingsClick,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingsIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.size(SettingsButtonSize),
        onClick = onClick,
        shape = CircleShape,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = 1.06f,
        contentDescription = stringResource(R.string.profile_manage),
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (focused) CinemaColors.Surface else CinemaColors.SurfaceSoft.copy(alpha = 0.85f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = if (focused) CinemaColors.White else CinemaColors.TextSecondary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AccountRow(
    name: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarVariant: AccountAvatarVariant = AccountAvatarVariant.Primary,
) {
    FocusableCinemaCard(
        modifier = modifier,
        onClick = onClick,
        shape = AccountRowShape,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
        focusScale = 1.02f,
    ) { focused ->
        val nameColor = if (focused || avatarVariant == AccountAvatarVariant.Primary) {
            CinemaColors.White
        } else {
            CinemaColors.TextSecondary
        }
        val subtitleColor = if (focused || avatarVariant == AccountAvatarVariant.Primary) {
            CinemaColors.TextSecondary
        } else {
            CinemaColors.TextMuted
        }
        val avatarSize = if (avatarVariant == AccountAvatarVariant.Primary) {
            PrimaryAvatarSize
        } else {
            SecondaryAvatarSize
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatarVariant == AccountAvatarVariant.Primary) {
                AccountAvatar(size = avatarSize)
            } else {
                Box(
                    modifier = Modifier
                        .size(avatarSize)
                        .clip(CircleShape)
                        .background(CinemaColors.SurfaceSoft.copy(alpha = if (focused) 1f else 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (focused) CinemaColors.TextSecondary else CinemaColors.TextMuted,
                        modifier = Modifier.size(avatarSize * 0.48f),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = nameColor,
                    ),
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleSmall.copy(color = subtitleColor),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun OtherAccountsRow(onClick: () -> Unit) {
    AccountRow(
        name = stringResource(R.string.profile_other_accounts),
        subtitle = null,
        onClick = onClick,
        avatarVariant = AccountAvatarVariant.Other,
    )
}
