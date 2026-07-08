package com.iptvcinema.tv.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.ProfileSelectionMode
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState

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
                                CinemaColors.Background.copy(alpha = 0.96f),
                                CinemaColors.Background.copy(alpha = 0.40f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CinemaColors.Background.copy(alpha = 0.26f)),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 88.dp, top = 96.dp)
                    .width(660.dp),
                verticalArrangement = Arrangement.spacedBy(46.dp),
            ) {
                Text(
                    text = if (mode == ProfileSelectionMode.SwitchProfile) {
                        stringResource(R.string.profile_switch)
                    } else {
                        stringResource(R.string.profile_title)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CinemaColors.TextSecondary,
                    ),
                )
                if (profiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_none_found),
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                    )
                } else {
                    val primaryProfile = profiles[selectedProfileIndex.coerceIn(profiles.indices)]
                    val otherProfiles = profiles.filterNot { it.id == primaryProfile.id }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccountRow(
                            name = primaryProfile.name,
                            subtitle = stringResource(R.string.profile_role_administrator),
                            selected = primaryProfile.id == currentProfileId,
                            modifier = Modifier.focusRequester(profileFocus),
                            onClick = {
                                focusState.saveFocusIndex(selectedProfileIndex)
                                onProfileSelected(primaryProfile.id)
                            },
                        )
                        FocusableCinemaCard(
                            modifier = Modifier.size(80.dp),
                            onClick = onManageProfiles,
                            shape = CinemaShapes.Card,
                            focusedBorderWidth = 0.dp,
                            contentDescription = stringResource(R.string.profile_manage),
                        ) { _ ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CinemaColors.SurfaceSoft, CinemaShapes.Card),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = CinemaColors.White,
                                    modifier = Modifier.size(34.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (otherProfiles.isEmpty()) {
                        OtherAccountsRow(onClick = onManageProfiles)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            otherProfiles.forEachIndexed { offset, profile ->
                                AccountRow(
                                    name = if (offset == 0) stringResource(R.string.profile_other_accounts) else profile.name,
                                    subtitle = if (offset == 0) null else stringResource(R.string.profile_role_administrator),
                                    selected = false,
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
private fun AccountRow(
    name: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarVariant: AccountAvatarVariant = AccountAvatarVariant.Primary,
) {
    FocusableCinemaCard(
        modifier = modifier.width(500.dp),
        onClick = onClick,
        shape = CinemaShapes.Card,
        defaultBorderWidth = 0.dp,
        focusedBorderWidth = 0.dp,
    ) { focused ->
        Row(
            modifier = Modifier
                .clip(CinemaShapes.Card)
                .background(if (focused || selected) CinemaColors.Surface.copy(alpha = 0.28f) else Color.Transparent)
                .padding(horizontal = 0.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatarVariant == AccountAvatarVariant.Primary) {
                AccountAvatar(size = 160.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(CinemaColors.SurfaceSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = CinemaColors.TextSecondary,
                        modifier = Modifier.size(74.dp),
                    )
                }
            }
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (focused || selected) CinemaColors.White else CinemaColors.TextSecondary,
                    ),
                    maxLines = 1,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextMuted),
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
        selected = false,
        onClick = onClick,
        avatarVariant = AccountAvatarVariant.Other,
    )
}
