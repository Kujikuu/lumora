package com.iptvcinema.tv.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.EmptyState
import com.iptvcinema.tv.core.design.components.FocusableCinemaCard
import com.iptvcinema.tv.core.design.components.SkeletonProfileRow
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.model.ProfileType
import com.iptvcinema.tv.core.model.UserProfile
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.ProfileSelectionMode

private val AvatarSize = 96.dp
private val CardWidth = 120.dp

// Fixed palette so a profile keeps its colour across devices; picked from the id.
private val AvatarColors = listOf(
    Color(0xFFE70302),
    Color(0xFFD37A00),
    Color(0xFF3F3DFF),
    Color(0xFF00897B),
    Color(0xFF8E24AA),
)

data class ProfileScreenActions(
    val onProfileSelected: (profileId: String) -> Unit,
    val onEditProfile: (UserProfile) -> Unit,
    val onAddProfile: () -> Unit,
    val onRetry: () -> Unit,
    val onBack: () -> Unit,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    mode: ProfileSelectionMode,
    currentProfileId: String?,
    profilesUiState: ProfilesUiState,
    canAddProfile: Boolean,
    actions: ProfileScreenActions,
    editorContent: @Composable () -> Unit = {},
) {
    var manageMode by rememberSaveable(mode) { mutableStateOf(mode == ProfileSelectionMode.Manage) }

    PopBackHandler(
        onBack = {
            if (manageMode && mode != ProfileSelectionMode.Manage) manageMode = false else actions.onBack()
        },
    )

    CinemaScreen(showTopNav = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            when (profilesUiState) {
                ProfilesUiState.Loading -> SkeletonProfileRow(count = 3)
                ProfilesUiState.Error -> EmptyState(
                    title = stringResource(R.string.profile_load_error),
                    description = stringResource(R.string.profile_load_error_body),
                    primaryAction = stringResource(R.string.btn_retry),
                    secondaryAction = null,
                    onPrimary = actions.onRetry,
                    onSecondary = null,
                )
                is ProfilesUiState.Ready -> ProfileGrid(
                    mode = mode,
                    manageMode = manageMode,
                    profiles = profilesUiState.profiles,
                    currentProfileId = currentProfileId,
                    canAddProfile = canAddProfile,
                    actions = actions,
                    onToggleManage = {
                        if (mode == ProfileSelectionMode.Manage) actions.onBack() else manageMode = !manageMode
                    },
                )
            }
        }
    }
    editorContent()
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileGrid(
    mode: ProfileSelectionMode,
    manageMode: Boolean,
    profiles: List<UserProfile>,
    currentProfileId: String?,
    canAddProfile: Boolean,
    actions: ProfileScreenActions,
    onToggleManage: () -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val focusIndex = profiles.indexOfFirst { it.id == currentProfileId }.coerceAtLeast(0)
    LaunchedEffect(profiles.size) { runCatching { initialFocus.requestFocus() } }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(
                when {
                    manageMode -> R.string.profile_manage
                    mode == ProfileSelectionMode.SwitchProfile -> R.string.profile_switch
                    else -> R.string.profile_title
                },
            ),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                color = CinemaColors.White,
            ),
        )
        if (manageMode) {
            Text(
                text = stringResource(R.string.profile_manage_hint),
                style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            profiles.forEachIndexed { index, profile ->
                ProfileCard(
                    profile = profile,
                    isCurrent = profile.id == currentProfileId,
                    showEditBadge = manageMode,
                    modifier = if (index == focusIndex) Modifier.focusRequester(initialFocus) else Modifier,
                    onClick = {
                        if (manageMode) actions.onEditProfile(profile) else actions.onProfileSelected(profile.id)
                    },
                )
            }
            if (canAddProfile) {
                AddProfileCard(onClick = actions.onAddProfile)
            }
        }
        Spacer(Modifier.height(32.dp))
        CinemaButton(
            text = stringResource(if (manageMode) R.string.profile_done else R.string.profile_manage),
            variant = CinemaButtonVariant.SecondaryDark,
            onClick = onToggleManage,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileCard(
    profile: UserProfile,
    isCurrent: Boolean,
    showEditBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCinemaCard(
        modifier = modifier.width(CardWidth),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        defaultBorderWidth = 0.dp,
        focusScale = 1.08f,
        contentDescription = profile.name,
    ) { focused ->
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(avatarColor(profile.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = profile.avatarInitial,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = CinemaColors.White,
                        ),
                    )
                }
                if (showEditBadge) {
                    Box(
                        modifier = Modifier
                            .size(AvatarSize)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CinemaColors.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = if (focused || isCurrent) CinemaColors.White else CinemaColors.TextSecondary,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            if (profile.type == ProfileType.KIDS) {
                Text(
                    text = stringResource(R.string.profile_kids_badge),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CinemaColors.Secondary)
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CinemaColors.Background,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    FocusableCinemaCard(
        modifier = Modifier.width(CardWidth),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        defaultBorderWidth = 0.dp,
        focusScale = 1.08f,
        contentDescription = stringResource(R.string.profile_add),
    ) { focused ->
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(if (focused) CinemaColors.Surface else CinemaColors.SurfaceSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = CinemaColors.TextSecondary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.profile_add),
                style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun avatarColor(profileId: String): Color =
    AvatarColors[Math.floorMod(profileId.hashCode(), AvatarColors.size)]
