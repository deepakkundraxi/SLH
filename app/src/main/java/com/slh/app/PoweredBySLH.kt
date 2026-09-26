package com.slh.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp


@Composable
fun PoweredBySLH(
    modifier: Modifier = Modifier
) {
    // Use APP theme (Light/Dark/System preference), not only system theme
    val isDark = isAppInDarkTheme()

    val logoRes = if (isDark) {
        R.drawable.slh_word_logo_dark
    } else {
        R.drawable.slh_word_logo_light
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Powered by",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "SLH",
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .height(44.dp),
            contentScale = ContentScale.Fit
        )
    }
}
