package com.iptvcinema.tv.features.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.data.fake.FakeDataProvider
import com.iptvcinema.tv.core.design.components.CategoryChip
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.ProfileChip
import com.iptvcinema.tv.core.design.components.RatingRestrictionSelector
import com.iptvcinema.tv.core.design.components.SettingsMenu
import com.iptvcinema.tv.core.design.components.SettingsMenuItem
import com.iptvcinema.tv.core.design.components.SettingsToggle
import com.iptvcinema.tv.core.design.components.SkeletonProfileRow
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.model.ParentalControls
import com.iptvcinema.tv.core.navigation.PopBackHandler
import com.iptvcinema.tv.core.navigation.rememberScreenFocusState
import com.iptvcinema.tv.features.settings.SettingsSection

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ParentalControlsScreen(
    navController: NavController,
    uiState: ParentalUiState,
    onSelectProfile: (String) -> Unit,
    onUpdateControls: ((ParentalControls) -> ParentalControls) -> Unit,
    onRetry: () -> Unit,
    onBeginSetPin: () -> Unit,
    onPinEntered: (PinEntryMode, String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onClearPin: () -> Unit,
) {
    PopBackHandler(onBack = { navController.popBackStack() })

    val profileFocus = remember { FocusRequester() }
    val focusState = rememberScreenFocusState("parental")
    var pinDialogMode by remember { mutableStateOf<PinEntryMode?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var changingPinAfterVerify by remember { mutableStateOf(false) }
    val sections = SettingsSection.entries
    val sectionLabels = sections.map { stringResource(it.labelRes) }

    LaunchedEffect(focusState.hasSavedFocus) {
        if (focusState.hasSavedFocus) {
            focusState.restoreFocus(profileFocus)
        } else {
            focusState.requestInitialFocus(profileFocus)
            focusState.saveFocusIndex(0)
        }
    }

    if (pinDialogMode != null) {
        PinEntryDialog(
            mode = pinDialogMode!!,
            title = when (pinDialogMode) {
                PinEntryMode.Verify -> stringResource(R.string.pin_enter)
                PinEntryMode.SetNew -> "Set New PIN"
                PinEntryMode.ConfirmNew -> "Confirm PIN"
                else -> stringResource(R.string.pin_enter)
            },
            errorMessage = pinError,
            onDismiss = {
                pinDialogMode = null
                pinError = null
                changingPinAfterVerify = false
            },
            onPinComplete = { pin ->
                val mode = pinDialogMode ?: return@PinEntryDialog
                onPinEntered(
                    mode,
                    pin,
                    {
                        pinError = null
                        when (mode) {
                            PinEntryMode.Verify -> {
                                if (changingPinAfterVerify) {
                                    pinDialogMode = PinEntryMode.SetNew
                                } else {
                                    pinDialogMode = null
                                }
                            }
                            PinEntryMode.SetNew -> pinDialogMode = PinEntryMode.ConfirmNew
                            PinEntryMode.ConfirmNew -> {
                                pinDialogMode = null
                                changingPinAfterVerify = false
                            }
                        }
                    },
                    { message ->
                        pinError = message
                    },
                )
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(CinemaSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap)) {
            SettingsMenu(
                items = sectionLabels.map { SettingsMenuItem(label = it) },
                selectedIndex = sections.indexOf(SettingsSection.ParentalControls),
                onSelected = { index ->
                    if (sections[index] != SettingsSection.ParentalControls) {
                        navController.popBackStack()
                    }
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CinemaSpacing.SectionGap),
            ) {
                Text(
                    text = stringResource(R.string.parental_title),
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                )
                when (uiState) {
                    ParentalUiState.Loading -> SkeletonProfileRow()
                    is ParentalUiState.Error -> {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.Danger),
                        )
                        CinemaButton(
                            text = stringResource(R.string.btn_retry),
                            variant = CinemaButtonVariant.SecondaryDark,
                            onClick = onRetry,
                        )
                    }
                    is ParentalUiState.Ready -> {
                        ParentalControlsContent(
                            uiState = uiState,
                            profileFocus = profileFocus,
                            onSelectProfile = onSelectProfile,
                            onUpdateControls = onUpdateControls,
                            onChangePin = {
                                onBeginSetPin()
                                if (uiState.controls.pinEnabled) {
                                    changingPinAfterVerify = true
                                    pinDialogMode = PinEntryMode.Verify
                                } else {
                                    changingPinAfterVerify = false
                                    pinDialogMode = PinEntryMode.SetNew
                                }
                            },
                            onClearPin = onClearPin,
                        )
                    }
                }
                CinemaButton(text = stringResource(R.string.btn_back), variant = CinemaButtonVariant.Ghost, onClick = { navController.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ParentalControlsContent(
    uiState: ParentalUiState.Ready,
    profileFocus: FocusRequester,
    onSelectProfile: (String) -> Unit,
    onUpdateControls: ((ParentalControls) -> ParentalControls) -> Unit,
    onChangePin: () -> Unit,
    onClearPin: () -> Unit,
) {
    val controls = uiState.controls
    val selectedProfileIndex = uiState.profiles.indexOfFirst { it.id == controls.profileId }.coerceAtLeast(0)
    var selectedRating by remember(controls.maxRating) {
        mutableIntStateOf(
            FakeDataProvider.ratingOptions.indexOf(controls.maxRating ?: "12+").coerceAtLeast(0),
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
        uiState.profiles.forEachIndexed { index, profile ->
            ProfileChip(
                name = profile.name,
                isSelected = index == selectedProfileIndex,
                onClick = { onSelectProfile(profile.id) },
                modifier = if (index == 0) {
                    Modifier.focusRequester(profileFocus)
                } else {
                    Modifier
                },
            )
        }
    }
    Text(
        text = if (controls.pinEnabled) "PIN status: Enabled" else "PIN status: Disabled",
        style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
        CinemaButton(
            text = if (controls.pinEnabled) "Change PIN" else "Set PIN",
            variant = CinemaButtonVariant.SecondaryDark,
            onClick = onChangePin,
        )
        if (controls.pinEnabled) {
            CinemaButton(
                text = stringResource(R.string.parental_remove_pin),
                variant = CinemaButtonVariant.Ghost,
                onClick = onClearPin,
            )
        }
    }
    Text(text = stringResource(R.string.parental_rating_restrictions), style = MaterialTheme.typography.titleMedium)
    RatingRestrictionSelector(
        options = FakeDataProvider.ratingOptions,
        selectedIndex = selectedRating,
        onSelected = { index ->
            selectedRating = index
            onUpdateControls { current ->
                current.copy(maxRating = FakeDataProvider.ratingOptions[index])
            }
        },
    )
    SettingsToggle(
        label = stringResource(R.string.parental_hide_adult),
        isOn = controls.hideAdultCategories,
        onToggle = {
            onUpdateControls { current -> current.copy(hideAdultCategories = !current.hideAdultCategories) }
        },
    )
    SettingsToggle(
        label = stringResource(R.string.parental_lock_playlist),
        isOn = controls.lockPlaylistSettings,
        onToggle = {
            onUpdateControls { current -> current.copy(lockPlaylistSettings = !current.lockPlaylistSettings) }
        },
    )
    SettingsToggle(
        label = stringResource(R.string.parental_require_pin_live),
        isOn = controls.lockLiveCategories,
        onToggle = {
            onUpdateControls { current -> current.copy(lockLiveCategories = !current.lockLiveCategories) }
        },
    )
    BlockedCategoryEditor(
        blockedCategories = controls.blockedCategories,
        availableCategories = uiState.availableCategories,
        onUpdateCategories = { updated ->
            onUpdateControls { current -> current.copy(blockedCategories = updated) }
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BlockedCategoryEditor(
    blockedCategories: List<String>,
    availableCategories: List<String>,
    onUpdateCategories: (List<String>) -> Unit,
) {
    val blocked = blockedCategories.ifEmpty { emptyList() }
    val addableCategories = availableCategories.filter { category ->
        blocked.none { it.equals(category, ignoreCase = true) }
    }

    Text(
        text = stringResource(R.string.settings_blocked_categories),
        style = MaterialTheme.typography.titleMedium.copy(
            color = CinemaColors.White,
            fontWeight = FontWeight.Bold,
        ),
    )
    if (blocked.isEmpty()) {
        Text(
            text = stringResource(R.string.parental_no_blocked_categories),
            style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            blocked.forEach { category ->
                CategoryChip(
                    label = category,
                    isSelected = true,
                    onClick = {
                        onUpdateCategories(blocked.filterNot { it.equals(category, ignoreCase = true) })
                    },
                )
            }
        }
    }
    if (addableCategories.isNotEmpty()) {
        Text(
            text = stringResource(R.string.parental_add_blocked_category),
            style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextSecondary),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
            addableCategories.take(12).forEach { category ->
                CategoryChip(
                    label = category,
                    isSelected = false,
                    onClick = {
                        onUpdateCategories((blocked + category).distinctBy { it.lowercase() })
                    },
                )
            }
        }
    }
}
