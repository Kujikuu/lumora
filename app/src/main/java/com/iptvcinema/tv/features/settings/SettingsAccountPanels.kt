package com.iptvcinema.tv.features.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.BuildConfig
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.components.AccountAvatar
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.SettingsHintText
import com.iptvcinema.tv.core.design.components.SettingsPanelHeader
import com.iptvcinema.tv.core.design.components.SettingsRow
import com.iptvcinema.tv.core.design.theme.CinemaColors
import java.time.Instant

private const val SUPPORT_EMAIL = "support@afifistudio.com"

/** Cloud sync state shown on the Account and Sync panels. */
data class CloudSyncUi(
    val isCloudAccount: Boolean,
    val isDegraded: Boolean,
    val isWriteDegraded: Boolean,
    val isSyncing: Boolean,
    val lastSyncedAt: Instant?,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AccountSettingsPanel(
    account: SettingsAccountUi,
    sync: CloudSyncUi,
    firstItemFocusRequester: FocusRequester,
    onSwitchProfile: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_account),
        subtitle = stringResource(R.string.settings_account_desc),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(size = 72.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = account.displayName ?: account.email ?: stringResource(R.string.settings_account_not_signed_in),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
            )
            account.email?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                )
            }
        }
    }
    SettingsRow(
        label = stringResource(R.string.settings_account_this_tv),
        isSelected = false,
        onClick = {},
        trailing = account.deviceName,
        modifier = Modifier.focusRequester(firstItemFocusRequester),
    )
    SettingsRow(
        label = stringResource(R.string.settings_cloud_status),
        isSelected = false,
        onClick = {},
        trailing = cloudStatusText(sync),
    )
    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CinemaButton(
            text = stringResource(R.string.settings_switch_profile),
            variant = CinemaButtonVariant.SecondaryDark,
            onClick = onSwitchProfile,
        )
        if (account.isCloudAccount) {
            CinemaButton(
                text = stringResource(R.string.settings_switch_account),
                variant = CinemaButtonVariant.SecondaryDark,
                onClick = onSwitchAccount,
            )
        }
        CinemaButton(
            text = stringResource(R.string.btn_sign_out),
            variant = CinemaButtonVariant.Danger,
            onClick = onSignOut,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun ProfilesSettingsPanel(
    activeProfileName: String?,
    firstItemFocusRequester: FocusRequester,
    onSwitchProfile: () -> Unit,
    onManageProfiles: () -> Unit,
) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_profiles),
        subtitle = stringResource(R.string.settings_profiles_desc),
    )
    SettingsRow(
        label = stringResource(R.string.settings_switch_profile),
        isSelected = false,
        onClick = onSwitchProfile,
        trailing = activeProfileName,
        modifier = Modifier.focusRequester(firstItemFocusRequester),
    )
    SettingsRow(
        label = stringResource(R.string.settings_manage_profiles),
        isSelected = false,
        onClick = onManageProfiles,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SyncSettingsPanel(
    sync: CloudSyncUi,
    firstItemFocusRequester: FocusRequester,
    onSyncNow: () -> Unit,
) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_sync),
        subtitle = stringResource(R.string.settings_sync_desc),
    )
    SettingsRow(
        label = stringResource(R.string.settings_cloud_status),
        isSelected = false,
        onClick = {},
        trailing = cloudStatusText(sync),
        modifier = Modifier.focusRequester(firstItemFocusRequester),
    )
    SettingsRow(
        label = stringResource(R.string.settings_last_synced),
        isSelected = false,
        onClick = {},
        trailing = lastSyncedText(sync.lastSyncedAt),
    )
    if (sync.isWriteDegraded) {
        SettingsHintText(text = stringResource(R.string.settings_write_degraded_hint))
    }
    if (sync.isCloudAccount) {
        CinemaButton(
            text = stringResource(if (sync.isSyncing) R.string.settings_syncing else R.string.settings_sync_now),
            variant = CinemaButtonVariant.PrimaryAccent,
            onClick = onSyncNow,
            enabled = !sync.isSyncing,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Real details a support request needs, so the user can read them out or photograph them. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SupportSettingsPanel(
    account: SettingsAccountUi,
    userId: String?,
    firstItemFocusRequester: FocusRequester,
) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_support),
        subtitle = stringResource(R.string.settings_support_desc),
    )
    SettingsRow(
        label = stringResource(R.string.settings_support_email_label),
        isSelected = false,
        onClick = {},
        trailing = SUPPORT_EMAIL,
        modifier = Modifier.focusRequester(firstItemFocusRequester),
    )
    SettingsRow(
        label = stringResource(R.string.settings_version),
        isSelected = false,
        onClick = {},
        trailing = BuildConfig.VERSION_NAME,
    )
    SettingsRow(
        label = stringResource(R.string.settings_account_this_tv),
        isSelected = false,
        onClick = {},
        trailing = account.deviceName,
    )
    account.email?.let { email ->
        SettingsRow(
            label = stringResource(R.string.settings_support_your_account),
            isSelected = false,
            onClick = {},
            trailing = email,
        )
    }
    userId?.let { id ->
        SettingsRow(
            label = stringResource(R.string.settings_support_account_id),
            isSelected = false,
            onClick = {},
            trailing = id.take(8).uppercase(),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun AboutSettingsPanel(firstItemFocusRequester: FocusRequester) {
    SettingsPanelHeader(
        title = stringResource(R.string.settings_about),
        subtitle = stringResource(R.string.settings_about_desc),
    )
    SettingsRow(
        label = stringResource(R.string.settings_version),
        isSelected = false,
        onClick = {},
        trailing = BuildConfig.VERSION_NAME,
        modifier = Modifier.focusRequester(firstItemFocusRequester),
    )
}

@Composable
private fun cloudStatusText(sync: CloudSyncUi): String = stringResource(
    when {
        !sync.isCloudAccount -> R.string.settings_cloud_local
        sync.isSyncing -> R.string.settings_syncing
        sync.isDegraded -> R.string.settings_cloud_degraded
        else -> R.string.settings_cloud_ok
    },
)

@Composable
private fun lastSyncedText(lastSyncedAt: Instant?): String {
    if (lastSyncedAt == null) return stringResource(R.string.settings_last_synced_never)
    return DateUtils.getRelativeTimeSpanString(
        lastSyncedAt.toEpochMilli(),
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}
