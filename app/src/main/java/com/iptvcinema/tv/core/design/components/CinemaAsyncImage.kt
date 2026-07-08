package com.iptvcinema.tv.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.iptvcinema.tv.R
import com.iptvcinema.tv.core.design.theme.CinemaColors
import com.iptvcinema.tv.core.design.theme.CinemaShapes

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CinemaAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackLabel: String = contentDescription.orEmpty(),
    showLoadingSkeleton: Boolean = false,
) {
    if (imageUrl.isNullOrBlank()) {
        ImageFallback(
            label = fallbackLabel,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    var loadFailed by remember(imageUrl) { mutableStateOf(false) }
    var isLoading by remember(imageUrl) { mutableStateOf(showLoadingSkeleton) }
    if (loadFailed) {
        ImageFallback(
            label = fallbackLabel,
            modifier = modifier,
            contentScale = contentScale,
        )
        return
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onError = {
                loadFailed = true
                isLoading = false
            },
            onSuccess = { isLoading = false },
            onLoading = { isLoading = showLoadingSkeleton },
        )
        if (isLoading && showLoadingSkeleton) {
            SkeletonBox(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ImageFallback(
    label: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(R.drawable.tv_banner_src),
        contentDescription = label.takeIf { it.isNotBlank() },
        modifier = modifier.fillMaxSize(),
        contentScale = contentScale,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ChannelImageFallback(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(CinemaColors.GoldDeep.copy(alpha = 0.22f), CinemaColors.SurfaceSoft),
                ),
                CinemaShapes.Medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.take(2).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = CinemaColors.GoldSoft,
            ),
        )
    }
}
