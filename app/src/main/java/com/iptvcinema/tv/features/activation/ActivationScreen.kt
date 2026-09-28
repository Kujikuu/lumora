package com.iptvcinema.tv.features.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.QrActivationPanel
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing
import com.iptvcinema.tv.core.navigation.PopBackHandler

private val MaxQrSize = 340.dp
private val QrCaptionAllowance = 40.dp
private val CountdownBarHeight = 6.dp
private val CountdownBarWidth = 180.dp

@Composable
fun ActivationScreenWithViewModel(
    viewModel: ActivationViewModel,
    onSignedIn: (StartupDestination) -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        (uiState as? ActivationUiState.Succeeded)?.let { onSignedIn(it.destination) }
    }

    ActivationScreen(
        uiState = uiState,
        onCheckApproval = viewModel::checkApprovalNow,
        onNewCode = viewModel::startActivation,
        onBack = onBack,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActivationScreen(
    uiState: ActivationUiState,
    onCheckApproval: () -> Unit,
    onNewCode: () -> Unit,
    onBack: () -> Unit,
) {
    PopBackHandler(onBack)

    CinemaScreen(showTopNav = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background)
                .padding(horizontal = 56.dp, vertical = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.activation_heading),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
            )
            val deviceName = (uiState as? ActivationUiState.Ready)?.deviceName
            if (!deviceName.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.activation_this_tv, deviceName),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
                )
            }
            Spacer(Modifier.height(20.dp))

            if (uiState is ActivationUiState.Error) {
                ActivationErrorPanel(kind = uiState.kind, onRetry = onNewCode)
            } else {
                ActivationBody(
                    uiState = uiState,
                    onCheckApproval = onCheckApproval,
                    onNewCode = onNewCode,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActivationBody(
    uiState: ActivationUiState,
    onCheckApproval: () -> Unit,
    onNewCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = uiState as? ActivationUiState.Ready
    val primaryFocus = remember { FocusRequester() }
    val isReady = ready != null

    // Buttons are disabled while loading, and a disabled button cannot take focus, so
    // focus is requested once the code is actually on screen.
    LaunchedEffect(isReady) {
        if (isReady) runCatching { primaryFocus.requestFocus() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val qrSize = min(maxHeight - QrCaptionAllowance, MaxQrSize)
        Row(verticalAlignment = Alignment.Top) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrActivationPanel(content = ready?.qrUrl.orEmpty(), size = qrSize)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.activation_scan_caption),
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextSecondary),
                )
            }

            OrDivider(height = qrSize, modifier = Modifier.align(Alignment.Top))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StepLabel(number = 1, text = stringResource(R.string.activation_step_go_to))
                Text(
                    text = stringResource(R.string.activation_url),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = CinemaColors.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                StepLabel(number = 2, text = stringResource(R.string.activation_step_enter_code))
                CodeBlock(uiState = uiState)
                ready?.let { CountdownRow(it) }
                StatusLine(uiState = uiState)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                    CinemaButton(
                        text = stringResource(R.string.activation_approved_button),
                        variant = CinemaButtonVariant.PrimaryAccent,
                        onClick = onCheckApproval,
                        modifier = Modifier.focusRequester(primaryFocus),
                        enabled = isReady,
                    )
                    CinemaButton(
                        text = stringResource(R.string.activation_new_code_button),
                        variant = CinemaButtonVariant.SecondaryDark,
                        onClick = onNewCode,
                        enabled = isReady && ready?.status != ActivationStatus.DevMode,
                    )
                }
                Text(
                    text = stringResource(R.string.activation_no_account_hint),
                    style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextMuted),
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StepLabel(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CinemaColors.Surface)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge.copy(color = CinemaColors.White),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CodeBlock(uiState: ActivationUiState) {
    val code = (uiState as? ActivationUiState.Ready)?.code
    Text(
        text = code ?: stringResource(R.string.activation_generating),
        style = if (code != null) {
            MaterialTheme.typography.displaySmall.copy(
                color = CinemaColors.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
            )
        } else {
            MaterialTheme.typography.titleLarge.copy(color = CinemaColors.TextSecondary)
        },
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CountdownRow(state: ActivationUiState.Ready) {
    val remaining = state.remainingSeconds ?: return
    val total = state.totalSeconds?.takeIf { it > 0 } ?: return
    val fraction = (remaining.toFloat() / total).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(CountdownBarWidth)
                .height(CountdownBarHeight)
                .clip(RoundedCornerShape(50))
                .background(CinemaColors.Surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(if (remaining <= LOW_TIME_SECONDS) CinemaColors.Warning else CinemaColors.TextSecondary),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.activation_expires_in, formatClock(remaining)),
            style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatusLine(uiState: ActivationUiState) {
    val (textRes, color) = when (uiState) {
        ActivationUiState.Loading -> return
        is ActivationUiState.Error -> return
        is ActivationUiState.Succeeded -> R.string.activation_signed_in_continuing to CinemaColors.Success
        is ActivationUiState.Ready -> statusText(uiState.status)
    }
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall.copy(color = color),
    )
}

private fun statusText(status: ActivationStatus): Pair<Int, Color> = when (status) {
    ActivationStatus.Waiting -> R.string.activation_status_waiting to CinemaColors.TextSecondary
    ActivationStatus.NotApprovedYet -> R.string.activation_status_not_approved to CinemaColors.Warning
    ActivationStatus.SigningIn -> R.string.activation_status_signing_in to CinemaColors.Success
    ActivationStatus.SignInFailed -> R.string.activation_status_sign_in_failed to CinemaColors.Danger
    ActivationStatus.ConnectionProblem -> R.string.activation_status_connection to CinemaColors.Warning
    ActivationStatus.DevMode -> R.string.activation_status_dev_mode to CinemaColors.TextMuted
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OrDivider(height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 32.dp)
            .height(height)
            .width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(1.dp)
                .background(CinemaColors.TextMuted.copy(alpha = 0.45f)),
        )
        Text(
            text = stringResource(R.string.activation_or),
            modifier = Modifier.padding(vertical = 10.dp),
            style = MaterialTheme.typography.titleSmall.copy(color = CinemaColors.TextMuted),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .width(1.dp)
                .background(CinemaColors.TextMuted.copy(alpha = 0.45f)),
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActivationErrorPanel(kind: ActivationError, onRetry: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(kind) { runCatching { retryFocus.requestFocus() } }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(
                when (kind) {
                    ActivationError.Network -> R.string.activation_error_network_title
                    ActivationError.Server -> R.string.activation_error_server_title
                },
            ),
            style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.White),
        )
        Text(
            text = stringResource(
                when (kind) {
                    ActivationError.Network -> R.string.activation_error_network_body
                    ActivationError.Server -> R.string.activation_error_server_body
                },
            ),
            style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
        )
        CinemaButton(
            text = stringResource(R.string.btn_try_again),
            variant = CinemaButtonVariant.PrimaryAccent,
            onClick = onRetry,
            modifier = Modifier.focusRequester(retryFocus),
        )
    }
}

internal fun formatClock(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(java.util.Locale.ROOT, minutes, seconds)
}

private const val LOW_TIME_SECONDS = 60L
