package com.iptvcinema.tv.features.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.datastore.StartupDestination
import com.iptvcinema.tv.core.navigation.BlockBackHandler
import com.iptvcinema.tv.core.design.components.BenefitItem
import com.iptvcinema.tv.core.design.components.CinemaButton
import com.iptvcinema.tv.core.design.components.CinemaButtonVariant
import com.iptvcinema.tv.core.design.components.CinemaLogo
import com.iptvcinema.tv.core.design.components.CinemaScreen
import com.iptvcinema.tv.core.design.components.QrActivationPanel
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes
import com.iptvcinema.tv.core.design.theme.CinemaSpacing

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ActivationScreen(
    uiState: ActivationUiState,
    onEnterAccount: () -> Unit,
    onCreateAccount: () -> Unit,
    onRetry: () -> Unit,
) {
    val primaryButtonFocus = remember { FocusRequester() }

    BlockBackHandler()

    LaunchedEffect(Unit) {
        primaryButtonFocus.requestFocus()
    }

    CinemaScreen(showTopNav = false, showRemoteHints = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 360.dp, height = 520.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(CinemaColors.GoldDeep.copy(alpha = 0.28f), CinemaColors.Background.copy(alpha = 0f)),
                        ),
                    ),
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier
                        .width(560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CinemaLogo()
                    Text(
                        text = stringResource(R.string.activation_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Normal,
                            color = CinemaColors.GoldSoft,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.activation_step1),
                        style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                    )
                    Text(
                        text = stringResource(R.string.activation_step2),
                        style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                    )

                    when (uiState) {
                        ActivationUiState.Loading -> {
                            Text(
                                text = stringResource(R.string.activation_generating),
                                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.TextSecondary),
                            )
                        }
                        is ActivationUiState.Ready -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                                    .border(1.dp, CinemaColors.Gold, CinemaShapes.Medium)
                                    .background(CinemaColors.SurfaceGlass, CinemaShapes.Medium)
                                    .padding(horizontal = 28.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = uiState.code,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CinemaColors.Gold,
                                    ),
                                )
                            }
                            Text(
                                text = uiState.statusMessage,
                                style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                            )
                        }
                        is ActivationUiState.Error -> {
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.Danger),
                            )
                            CinemaButton(
                                text = stringResource(R.string.btn_try_again),
                                variant = CinemaButtonVariant.SecondaryDark,
                                onClick = onRetry,
                            )
                        }
                        ActivationUiState.Succeeded -> {
                            Text(
                                text = stringResource(R.string.activation_signed_in_continuing),
                                style = MaterialTheme.typography.bodyLarge.copy(color = CinemaColors.Success),
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(CinemaSpacing.ButtonGap)) {
                        CinemaButton(
                            text = stringResource(R.string.activation_enter_account),
                            variant = CinemaButtonVariant.PrimaryAccent,
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

                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        BenefitItem(
                            label = stringResource(R.string.benefit_sync_watchlist),
                            description = stringResource(R.string.benefit_sync_watchlist_desc),
                            modifier = Modifier.width(150.dp),
                        )
                        BenefitItem(
                            label = stringResource(R.string.benefit_resume),
                            description = stringResource(R.string.benefit_resume_desc),
                            modifier = Modifier.width(150.dp),
                        )
                        BenefitItem(
                            label = stringResource(R.string.benefit_recommendations),
                            description = stringResource(R.string.benefit_recommendations_desc),
                            modifier = Modifier.width(170.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.width(360.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    QrActivationPanel(
                        content = (uiState as? ActivationUiState.Ready)?.qrUrl.orEmpty(),
                        size = 290.dp,
                    )
                    Text(
                        text = stringResource(R.string.activation_scan),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = CinemaColors.Gold,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text(
                        text = (uiState as? ActivationUiState.Ready)?.qrUrl
                            ?: stringResource(R.string.activation_scan_hint),
                        style = MaterialTheme.typography.bodySmall.copy(color = CinemaColors.TextSecondary),
                    )
                }
            }
        }
    }
}

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
