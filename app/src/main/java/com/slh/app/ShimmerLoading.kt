package com.slh.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * A single shimmering bar/block. Combine a few of these to build a
 * skeleton that matches the shape of the real card underneath it.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerProgress"
    )

    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val color = lerp(base, highlight, progress)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
    )
}

/**
 * Skeleton for a dashboard-style stat/notice card. Show this while the
 * real content is loading, instead of a spinner.
 *
 * Usage:
 *   if (isLoading) {
 *       repeat(3) { ShimmerCard() }
 *   } else { ... real cards ... }
 */
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 14.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 20.dp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        ShimmerBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
    }
}