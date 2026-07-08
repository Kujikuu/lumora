package com.iptvcinema.tv.features.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.navigation.BlockBackHandler
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.QrActivationPanel
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

private val ActivationQrSize = 575.dp
private val ActivationDividerHeight = 575.dp
private const val ActivationEmphasisTextScale = 0.6f

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActivationScreen(
    uiState: ActivationUiState,
    onEnterAccount: () -> Unit,
    onCreateAccount: () -> Unit,
    onRetry: () -> Unit,
) {
    val primaryButtonFocus = remember { FocusRequester() }
    val qrUrl = (uiState as? ActivationUiState.Ready)?.qrUrl.orEmpty()

    BlockBackHandler()

    LaunchedEffect(Unit) {
        primaryButtonFocus.requestFocus()
    }

    CinemaScreen(showTopNav = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaColors.Background)
                .padding(horizontal = 72.dp, vertical = 56.dp),
        ) {
            Text(
                text = stringResource(R.string.activation_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = CinemaColors.White,
                ),
            )

            Spacer(Modifier.height(36.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.activation_step1),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CinemaColors.TextSecondary,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                    QrActivationPanel(
                        content = qrUrl,
                        size = ActivationQrSize,
                    )
                }

                ActivationOrDivider(
                    modifier = Modifier.align(Alignment.CenterVertically),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(
                        text = stringResource(R.string.activation_step2),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CinemaColors.TextSecondary,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.activation_url),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineMedium
                            .scaled(ActivationEmphasisTextScale)
                            .copy(
                                color = CinemaColors.White,
                                fontWeight = FontWeight.Black,
                            ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.activation_scan),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CinemaColors.TextSecondary,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                    ActivationCodeState(uiState = uiState, onRetry = onRetry)
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                        CinemaButton(
                            text = stringResource(R.string.activation_enter_account),
                            variant = CinemaButtonVariant.SecondaryDark,
                            onClick = onEnterAccount,
                            modifier = Modifier.focusRequester(primaryButtonFocus),
                            enabled = uiState is ActivationUiState.Ready,
                        )
                        CinemaButton(
                            text = stringResource(R.string.activation_create_account),
                            variant = CinemaButtonVariant.SecondaryDark,
                            onClick = onCreateAccount,
                            enabled = uiState is ActivationUiState.Ready,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActivationOrDivider(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 40.dp)
            .height(ActivationDividerHeight)
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .width(1.dp)
                .background(CinemaColors.TextMuted.copy(alpha = 0.45f)),
        )
        Text(
            text = stringResource(R.string.activation_or),
            modifier = Modifier
                .background(CinemaColors.Background)
                .padding(vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                color = CinemaColors.TextMuted,
                fontWeight = FontWeight.Medium,
            ),
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
private fun ActivationCodeState(
    uiState: ActivationUiState,
    onRetry: () -> Unit,
) {
    when (uiState) {
        ActivationUiState.Loading -> {
            Text(
                text = stringResource(R.string.activation_generating),
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = CinemaColors.White,
                    fontWeight = FontWeight.Black,
                ),
            )
        }
        is ActivationUiState.Ready -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = uiState.code,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineLarge
                        .scaled(ActivationEmphasisTextScale)
                        .copy(
                            color = CinemaColors.White,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (2.sp.value * ActivationEmphasisTextScale).sp,
                        ),
                )
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(color = CinemaColors.TextMuted),
                )
            }
        }
        is ActivationUiState.Error -> {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.titleLarge.copy(color = CinemaColors.Danger),
                )
                CinemaButton(
                    text = stringResource(R.string.btn_try_again),
                    variant = CinemaButtonVariant.SecondaryDark,
                    onClick = onRetry,
                )
            }
        }
        ActivationUiState.Succeeded -> {
            Text(
                text = stringResource(R.string.activation_signed_in_continuing),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = CinemaColors.Success,
                    fontWeight = FontWeight.Black,
                ),
            )
        }
    }
}

private fun TextStyle.scaled(factor: Float): TextStyle = copy(
    fontSize = (fontSize.value * factor).sp,
    lineHeight = (lineHeight.value * factor).sp,
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActivationScreenWithViewModel(
    viewModel: ActivationViewModel,
    onEnterAccount: (StartupDestination) -> Unit,
    onCreateAccount: (StartupDestination) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ActivationUiState.Succeeded) {
            val destination = viewModel.resolvePostAuthDestination()
            onEnterAccount(destination)
        }
    }

    ActivationScreen(
        uiState = uiState,
        onEnterAccount = { viewModel.authenticate(onEnterAccount) },
        onCreateAccount = { viewModel.authenticate(onCreateAccount) },
        onRetry = viewModel::startActivation,
    )
}
