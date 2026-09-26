package com.slh.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MapsHomeWork
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

object BottomNavItems {

    val student = listOf(
        BottomNavItem("dashboard", "Home", Icons.Default.Home),
        BottomNavItem("attendance", "Attendance", Icons.Default.MapsHomeWork),
        BottomNavItem("fees", "Fees", Icons.Default.Payments),
        BottomNavItem("notices", "Notices", Icons.Default.Notifications)
    )

    val teacher = listOf(
        BottomNavItem("dashboard", "Home", Icons.Default.Home),
        BottomNavItem("questions", "Tests", Icons.Default.Assignment),
        BottomNavItem("notices", "Notices", Icons.Default.Notifications),
        BottomNavItem("results", "Results", Icons.Default.Assessment)
    )

    val admin = listOf(
        BottomNavItem("dashboard", "Home", Icons.Default.Home),
        BottomNavItem("batches", "Batches", Icons.Default.Assignment),
        BottomNavItem("fees", "Fees", Icons.Default.Payments),
        BottomNavItem("teachers", "Teachers", Icons.Default.Person)
    )
}

/**
 * Bottom nav with larger icons (like typical Material size).
 * navigationBarsPadding only once — parent must NOT also pad for nav bars.
 */
@Composable
fun SLHBottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val selected = item.route == currentRoute
                val tint =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = { onNavigate(item.route) })
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = tint,
                        // Larger icons as requested
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = item.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
