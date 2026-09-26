package com.slh.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.slh.app.ui.theme.ChipColors
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun DashboardSectionTitle(
    text: String
) {

    Text(
        text = text,
        style =
            MaterialTheme
                .typography
                .titleLarge,
        fontWeight =
            FontWeight.Bold
    )
}

@Composable
fun WelcomeCard(
    name: String,
    subtitle: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primary
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Person,
                    contentDescription = null,
                    modifier =
                        Modifier.size(30.dp),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onPrimary
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Welcome back 👋",
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge
                )

                Text(
                    text = name,
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    containerColor: Color,
    iconColor: Color
) {

    Card(
        modifier = modifier,
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    containerColor
            )
    ) {

        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {

            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(
                                alpha = 0.75f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier =
                        Modifier.size(22.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text = title,
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
fun DashboardFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Color,
    iconColor: Color,
    onClick: (() -> Unit)? = null
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable {
                            onClick()
                        }
                    } else {
                        Modifier
                    }
                ),
        shape =
            RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(14.dp)
                        )
                        .background(
                            iconBackground
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier =
                        Modifier.size(25.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    iconColor: Color,
    onClick: (() -> Unit)? = null
) {

    Card(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier.clickable {
                        onClick()
                    }
                } else {
                    Modifier
                }
            ),
        shape =
            RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier =
                    Modifier.size(30.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text = title,
                fontWeight =
                    FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DashboardFeeManagementCard(
    totalFee: Double,
    collected: Double,
    remaining: Double,
    overdueStudents: Int,
    overdueInstallments: Int,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    ChipColors.amber.container
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                ChipColors.orange.container
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier =
                            Modifier.size(29.dp),
                        tint =
                            ChipColors.orange.icon
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "Fees Management",
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "3-installment fee collection",
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "OPEN",
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        ChipColors.orange.icon
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                FeeDashboardMiniCard(
                    title = "Total",
                    value =
                        formatMoney(totalFee),
                    color =
                        ChipColors.blue.icon,
                    modifier =
                        Modifier.weight(1f)
                )

                FeeDashboardMiniCard(
                    title = "Collected",
                    value =
                        formatMoney(collected),
                    color =
                        ChipColors.green.icon,
                    modifier =
                        Modifier.weight(1f)
                )

                FeeDashboardMiniCard(
                    title = "Remaining",
                    value =
                        formatMoney(remaining),
                    color =
                        ChipColors.orange.icon,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (overdueStudents > 0) {

                Text(
                    text =
                        "$overdueStudents students have overdue fees " +
                                "($overdueInstallments installments)",
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        ChipColors.red.icon
                )
            }
        }
    }
}

@Composable
private fun FeeDashboardMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier
) {

    Card(
        modifier = modifier,
        shape =
            RoundedCornerShape(14.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.80f
                    )
            )
    ) {

        Column(
            modifier =
                Modifier.padding(9.dp)
        ) {

            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun DashboardStudentFeeCard(
    summary: StudentFeeSummary?,
    overdueCount: Int,
    onClick: () -> Unit
) {

    val total =
        summary?.totalFee ?: 0.0

    val paid =
        summary?.totalPaid ?: 0.0

    val remaining =
        summary?.remaining ?: 0.0

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },
        shape =
            RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    ChipColors.pink.container
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.AttachMoney,
                    contentDescription = null,
                    tint =
                        ChipColors.pink.icon,
                    modifier =
                        Modifier.size(30.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Column {

                    Text(
                        text = "My Fees",
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "View your payment details",
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                StudentFeeMiniCard(
                    title = "Total",
                    value =
                        formatMoney(total),
                    color =
                        ChipColors.blue.icon,
                    modifier =
                        Modifier.weight(1f)
                )

                StudentFeeMiniCard(
                    title = "Paid",
                    value =
                        formatMoney(paid),
                    color =
                        ChipColors.green.icon,
                    modifier =
                        Modifier.weight(1f)
                )

                StudentFeeMiniCard(
                    title = "Remaining",
                    value =
                        formatMoney(remaining),
                    color =
                        ChipColors.orange.icon,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            if (overdueCount > 0) {

                Text(
                    text =
                        "⚠ $overdueCount overdue installment(s)",
                    fontSize = 12.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        ChipColors.red.icon
                )
            }
        }
    }
}

@Composable
private fun StudentFeeMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier
) {

    Column(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(12.dp)
                )
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.80f
                    )
                )
                .padding(9.dp)
    ) {

        Text(
            text = title,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun PrincipalMiniDashboard(
    user: DemoUser
) {

    WelcomeCard(
        name = user.displayName,
        subtitle =
            "Limited platform administration"
    )

    DashboardSectionTitle(
        text = "Principal Controls"
    )

    DashboardFeatureCard(
        icon =
            Icons.Default.AccountBalance,
        title =
            "Coaching Management",
        subtitle =
            "Manage coaching institutions.",
        iconBackground =
            ChipColors.blue.container,
        iconColor =
            ChipColors.blue.icon
    )

    DashboardFeatureCard(
        icon =
            Icons.Default.Person,
        title =
            "Coaching Admins",
        subtitle =
            "Create and approve coaching admins.",
        iconBackground =
            ChipColors.green.container,
        iconColor =
            ChipColors.green.icon
    )

    DashboardFeatureCard(
        icon =
            Icons.Default.Settings,
        title =
            "Platform Settings",
        subtitle =
            "Limited platform-level settings.",
        iconBackground =
            ChipColors.orange.container,
        iconColor =
            ChipColors.orange.icon
    )
}

fun formatMoney(
    amount: Double
): String {

    return "₹" +
            String.format(
                Locale.getDefault(),
                "%.0f",
                amount
            )
}