package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slh.app.ui.theme.ChipColors



@Composable
fun AdminDashboardScreen(
    user: DemoUser,
    coaching: CoachingProfile?,
    onCoachingProfile: () -> Unit,
    onStudentManagement: () -> Unit,
    onTeacherManagement: () -> Unit,
    onBatchManagement: () -> Unit,
    onFeeManagement: () -> Unit,
    onFeeReport: () -> Unit = {},
    onPaymentVerification: () -> Unit = {},
    onAttendance: () -> Unit = {},
    onTestManagement: () -> Unit,
    onTestApproval: () -> Unit = {},
    onResultManagement: () -> Unit,
    onNoticeManagement: () -> Unit,
    onResultAnalytics: () -> Unit = {}
) {

    BackHandler {
        // MainActivity controls dashboard back behaviour.
    }

    val coachingId = user.coachingId ?: ""

    val studentCount =
        StudentStore.students.count { it.coachingId == coachingId }

    val teacherCount = TeacherStore.getTeacherCount(coachingId)
    val batchCount = BatchStore.getBatchCount(coachingId)
    val activeBatchCount = BatchStore.getActiveBatchCount(coachingId)
    val activeTestCount = TestStore.getActiveTestCount(coachingId)
    val noticeCount =
        NoticeStore.findActiveNoticesByCoaching(coachingId).size
    val pendingApprovalCount =
        TestStore.getPendingApprovalCount(coachingId)

    val (themeMode, setThemeMode) = rememberThemeMode()

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isNarrow = screenWidthDp < 360

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = if (isNarrow) 10.dp else 14.dp,
                vertical = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        AdminWelcomeCard(
            name = user.displayName,
            subtitle = coaching?.tagline
                ?.takeIf { it.isNotBlank() }
                ?: "Manage your coaching from one place."
        )

        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (isNarrow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    title = "Students",
                    value = studentCount.toString(),
                    containerColor = ChipColors.blue.container,
                    iconColor = ChipColors.blue.icon
                )
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    title = "Teachers",
                    value = teacherCount.toString(),
                    containerColor = ChipColors.green.container,
                    iconColor = ChipColors.green.icon
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Groups,
                    title = "Batches",
                    value = activeBatchCount.toString(),
                    containerColor = ChipColors.orange.container,
                    iconColor = ChipColors.orange.icon
                )
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Quiz,
                    title = "Tests",
                    value = activeTestCount.toString(),
                    containerColor = ChipColors.purple.container,
                    iconColor = ChipColors.purple.icon
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    title = "Students",
                    value = studentCount.toString(),
                    containerColor = ChipColors.blue.container,
                    iconColor = ChipColors.blue.icon
                )
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    title = "Teachers",
                    value = teacherCount.toString(),
                    containerColor = ChipColors.green.container,
                    iconColor = ChipColors.green.icon
                )
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Groups,
                    title = "Batches",
                    value = activeBatchCount.toString(),
                    containerColor = ChipColors.orange.container,
                    iconColor = ChipColors.orange.icon
                )
                AdminStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Quiz,
                    title = "Tests",
                    value = activeTestCount.toString(),
                    containerColor = ChipColors.purple.container,
                    iconColor = ChipColors.purple.icon
                )
            }
        }

        Text(
            text = "Management",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.People,
                    title = "Students",
                    subtitle = "Manage students",
                    iconBackground = ChipColors.blue.container,
                    iconColor = ChipColors.blue.icon,
                    onClick = onStudentManagement
                )
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    title = "Teachers",
                    subtitle = "Manage teachers",
                    iconBackground = ChipColors.green.container,
                    iconColor = ChipColors.green.icon,
                    onClick = onTeacherManagement
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Groups,
                    title = "Batches",
                    subtitle = "Manage batches",
                    iconBackground = ChipColors.orange.container,
                    iconColor = ChipColors.orange.icon,
                    onClick = onBatchManagement
                )
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Today,
                    title = "Attendance",
                    subtitle = "View attendance",
                    iconBackground = ChipColors.teal.container,
                    iconColor = ChipColors.teal.icon,
                    onClick = onAttendance
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AttachMoney,
                    title = "Fees",
                    subtitle = "Fees & payments",
                    iconBackground = ChipColors.pink.container,
                    iconColor = ChipColors.pink.icon,
                    onClick = onFeeManagement
                )
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AttachMoney,
                    title = "Fee Report",
                    subtitle = "Payments & collection",
                    iconBackground = ChipColors.pink.container,
                    iconColor = ChipColors.pink.icon,
                    onClick = onFeeReport
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    title = "Verify Payments",
                    subtitle = "Approve & receipts",
                    iconBackground = ChipColors.indigo.container,
                    iconColor = ChipColors.indigo.icon,
                    onClick = onPaymentVerification
                )
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Quiz,
                    title = "Tests & Questions",
                    subtitle = "Tests & question bank",
                    iconBackground = ChipColors.purple.container,
                    iconColor = ChipColors.purple.icon,
                    onClick = onTestManagement
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Quiz,
                    title = "Test Approvals",
                    subtitle = if (pendingApprovalCount > 0)
                        "$pendingApprovalCount Pending"
                    else
                        "Review teacher papers",
                    iconBackground = ChipColors.orange.container,
                    iconColor = ChipColors.orange.icon,
                    onClick = onTestApproval
                )
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Assessment,
                    title = "Marks & Results",
                    subtitle = "Manage results",
                    iconBackground = ChipColors.teal.container,
                    iconColor = ChipColors.teal.icon,
                    onClick = onResultManagement
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Campaign,
                    title = "Notices",
                    subtitle = "Manage notices",
                    iconBackground = ChipColors.amber.container,
                    iconColor = ChipColors.amber.icon,
                    onClick = onNoticeManagement
                )
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Assessment,
                    title = "Result Analytics",
                    subtitle = "Performance analytics",
                    iconBackground = ChipColors.blue.container,
                    iconColor = ChipColors.blue.icon,
                    onClick = onResultAnalytics
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminGridFeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.School,
                    title = "Coaching Profile",
                    subtitle = "Profile & details",
                    iconBackground = ChipColors.green.container,
                    iconColor = ChipColors.green.icon,
                    onClick = onCoachingProfile
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Text(
            text = "Quick Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminCompactInfoCard(
                modifier = Modifier.weight(1f),
                title = "Batches",
                value = batchCount.toString(),
                subtitle = "Total",
                icon = Icons.Default.Groups,
                iconBackground = ChipColors.orange.container,
                iconColor = ChipColors.orange.icon
            )
            AdminCompactInfoCard(
                modifier = Modifier.weight(1f),
                title = "Tests",
                value = activeTestCount.toString(),
                subtitle = "Active",
                icon = Icons.Default.Quiz,
                iconBackground = ChipColors.purple.container,
                iconColor = ChipColors.purple.icon
            )
            AdminCompactInfoCard(
                modifier = Modifier.weight(1f),
                title = "Notices",
                value = noticeCount.toString(),
                subtitle = "Active",
                icon = Icons.Default.Campaign,
                iconBackground = ChipColors.amber.container,
                iconColor = ChipColors.amber.icon
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ThemeModeSelector(
                    current = themeMode,
                    onSelect = setThemeMode
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}


@Composable
private fun AdminWelcomeCard(
    name: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(17.dp)) {
            Text(
                text = "Welcome back",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = name,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun AdminStatCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    containerColor: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(31.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(9.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
private fun AdminGridFeatureCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Color,
    iconColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .heightIn(min = 108.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = iconBackground,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}


@Composable
private fun AdminCompactInfoCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = iconBackground,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
