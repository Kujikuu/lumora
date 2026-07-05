package com.iptvcinema.tv.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaLogo
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.ProfileCard
import com.iptvcinema.tv.core.design.components.SkeletonProfileRow
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
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
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(CinemaColors.GoldDeep.copy(alpha = 0.14f), CinemaColors.Background.copy(alpha = 0f)),
                        ),
                    ),
            )
            CinemaLogo(modifier = Modifier.align(Alignment.TopStart))
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (mode == ProfileSelectionMode.SwitchProfile) {
                            stringResource(R.string.profile_switch)
                        } else {
                            stringResource(R.string.profile_title)
                        },
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            color = CinemaColors.GoldSoft,
                        ),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.profile_subtitle),
                        style = MaterialTheme.typography.titleMedium.copy(color = CinemaColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                }
                if (profiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_none_found),
                        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.RailGap)) {
                        profiles.forEachIndexed { index, profile ->
                            val isActiveProfile = profile.id == currentProfileId
                            ProfileCard(
                                name = profile.name,
                                initial = profile.avatarInitial,
                                isSelected = index == selectedProfileIndex || isActiveProfile,
                                onClick = {
                                    selectedProfileIndex = index
                                    focusState.saveFocusIndex(index)
                                    onProfileSelected(profile.id)
                                },
                                modifier = if (index == selectedProfileIndex) {
                                    Modifier.focusRequester(profileFocus)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }
                }
                CinemaButton(
                    text = stringResource(R.string.profile_manage),
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = onManageProfiles,
                )
            }
        }
    }
}
