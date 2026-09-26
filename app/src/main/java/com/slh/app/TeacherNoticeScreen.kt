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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun TeacherNoticeScreen(
    user: DemoUser,
    onBack: () -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {

    var selectedNotice by remember {
        mutableStateOf<CoachingNotice?>(null)
    }

    BackHandler {

        if (selectedNotice != null) {
            selectedNotice = null
        } else {
            onBack()
        }
    }

    if (selectedNotice != null) {

        TeacherNoticeDetailScreen(
            notice = selectedNotice!!,
            userId = user.id,
            onBack = {
                selectedNotice = null
            }
        )

    } else {

        TeacherNoticeListScreen(
            user = user,
            onBack = onBack,
            onNavigateTab = onNavigateTab,
            onNoticeClick = { notice ->

                /*
                 * Mark this notice as read only
                 * for the currently logged-in teacher.
                 */
                NoticeStore.markNoticeAsRead(
                    noticeId = notice.id,
                    userId = user.id
                )

                selectedNotice = notice
            }
        )
    }
}


/* =========================================================
 * TEACHER NOTICE LIST
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherNoticeListScreen(
    user: DemoUser,
    onBack: () -> Unit,
    onNoticeClick: (CoachingNotice) -> Unit,
    onNavigateTab: (String) -> Unit = {}
) {

    val coachingId =
        user.coachingId ?: ""

    /*
     * Teacher currently receives all active notices
     * belonging to this coaching.
     *
     * This includes:
     * - Coaching-wide notices
     * - Batch-specific notices
     *
     * Individual read state is checked using user.id.
     */
    val notices =
        NoticeStore.findActiveNoticesByCoaching(
            coachingId = coachingId
        )

    val unreadCount =
        NoticeStore.getUnreadNoticeCount(
            coachingId = coachingId,
            userId = user.id
        )

    // Same layout as fixed Teacher Dashboard — no nested Scaffold
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            /*
             * Unread summary
             */
            if (unreadCount > 0) {

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFFFEBEE)
                        )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(12.dp)
                                    .background(
                                        color =
                                            Color(0xFFD32F2F),
                                        shape =
                                            androidx.compose.foundation.shape.CircleShape
                                    )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(10.dp)
                        )

                        Text(
                            text =
                                if (unreadCount == 1) {
                                    "1 unread notice"
                                } else {
                                    "$unreadCount unread notices"
                                },

                            color =
                                Color(0xFFC62828),

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            if (notices.isEmpty()) {

                EmptyTeacherNoticeState()

            } else {

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize(),

                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 24.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = notices,
                        key = {
                            it.id
                        }
                    ) { notice ->

                        val isUnread =
                            !NoticeStore.hasUserReadNotice(
                                noticeId = notice.id,
                                userId = user.id
                            )

                        TeacherNoticeCard(
                            notice = notice,
                            isUnread = isUnread,
                            onClick = {
                                onNoticeClick(notice)
                            }
                        )
                    }
                }
            }
        }

        SLHBottomNavBar(
            items = BottomNavItems.teacher,
            currentRoute = "notices",
            onNavigate = { route ->
                if (route == "notices") {
                    // already here
                } else if (route == "dashboard") {
                    onBack()
                } else {
                    onNavigateTab(route)
                }
            }
        )
    }
}


/* =========================================================
 * NOTICE CARD
 * ========================================================= */

@Composable
private fun TeacherNoticeCard(
    notice: CoachingNotice,
    isUnread: Boolean,
    onClick: () -> Unit
) {

    val containerColor =
        if (isUnread) {
            Color(0xFFFFF5F5)
        } else {
            Color.White
        }

    val borderColor =
        if (isUnread) {
            Color(0xFFE53935)
        } else {
            Color(0xFFE0E0E0)
        }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        colors =
            CardDefaults.cardColors(
                containerColor =
                    containerColor
            ),

        border =
            androidx.compose.foundation.BorderStroke(
                width =
                    if (isUnread) 1.5.dp else 1.dp,
                color =
                    borderColor
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (isUnread) 3.dp else 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.Top
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(46.dp)
                            .background(
                                color =
                                    if (isUnread) {
                                        Color(0xFFFFEBEE)
                                    } else {
                                        Color(0xFFFFF8E1)
                                    },

                                shape =
                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                        12.dp
                                    )
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Campaign,

                        contentDescription =
                            "Notice",

                        tint =
                            if (isUnread) {
                                Color(0xFFD32F2F)
                            } else {
                                Color(0xFFF9A825)
                            },

                        modifier =
                            Modifier.size(26.dp)
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

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                notice.title,

                            modifier =
                                Modifier.weight(1f),

                            style =
                                MaterialTheme.typography.titleMedium,

                            fontWeight =
                                if (isUnread) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.SemiBold
                                },

                            color =
                                if (isUnread) {
                                    Color(0xFFB71C1C)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                        )

                        if (isUnread) {

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .size(10.dp)
                                        .background(
                                            color =
                                                Color(0xFFD32F2F),
                                            shape =
                                                androidx.compose.foundation.shape.CircleShape
                                        )
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text =
                            notice.message,

                        maxLines = 3,

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Today,

                    contentDescription =
                        "Date",

                    modifier =
                        Modifier.size(17.dp),

                    tint =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier =
                        Modifier.width(5.dp)
                )

                Text(
                    text =
                        notice.noticeDate
                            .ifBlank {
                                "Date not available"
                            },

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (notice.batchId.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.width(14.dp)
                    )

                    Icon(
                        imageVector =
                            Icons.Default.Groups,

                        contentDescription =
                            "Batch",

                        modifier =
                            Modifier.size(17.dp),

                        tint =
                            Color(0xFF5E35B1)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(5.dp)
                    )

                    val batch =
                        BatchStore.findBatch(
                            notice.batchId
                        )

                    Text(
                        text =
                            batch?.name
                                ?.ifBlank {
                                    "Batch Notice"
                                }
                                ?: "Batch Notice",

                        style =
                            MaterialTheme.typography.bodySmall,

                        color =
                            Color(0xFF5E35B1),

                        maxLines = 1
                    )
                }
            }
        }
    }
}


/* =========================================================
 * NOTICE DETAIL
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherNoticeDetailScreen(
    notice: CoachingNotice,
    userId: String,
    onBack: () -> Unit
) {

    /*
     * Safety:
     * If the detail screen is opened directly,
     * mark it as read for this teacher.
     */
    LaunchedEffect(
        notice.id,
        userId
    ) {

        NoticeStore.markNoticeAsRead(
            noticeId = notice.id,
            userId = userId
        )
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier =
            Modifier.fillMaxSize(),

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        text = "Notice"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),

            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 32.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFFFF8E1)
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(52.dp)
                                        .background(
                                            color =
                                                Color.White,
                                            shape =
                                                androidx.compose.foundation.shape.RoundedCornerShape(
                                                    14.dp
                                                )
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Campaign,

                                    contentDescription =
                                        "Notice",

                                    tint =
                                        Color(0xFFF9A825),

                                    modifier =
                                        Modifier.size(30.dp)
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(14.dp)
                            )

                            Text(
                                text =
                                    notice.title,

                                style =
                                    MaterialTheme.typography.headlineSmall,

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                    ) {

                        Text(
                            text = "Notice Details",

                            style =
                                MaterialTheme.typography.titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        Text(
                            text =
                                notice.message,

                            style =
                                MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            item {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color(0xFFF5F5F5)
                        )
                ) {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                    ) {

                        NoticeDetailRow(
                            icon =
                                Icons.Default.Today,

                            label =
                                "Notice Date",

                            value =
                                notice.noticeDate
                                    .ifBlank {
                                        "Not specified"
                                    }
                        )

                        if (notice.expiryDate.isNotBlank()) {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            NoticeDetailRow(
                                icon =
                                    Icons.Default.Today,

                                label =
                                    "Expiry Date",

                                value =
                                    notice.expiryDate
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        NoticeDetailRow(
                            icon =
                                Icons.Default.School,

                            label =
                                "Issued By",

                            value =
                                notice.createdByName
                                    .ifBlank {
                                        "Coaching Admin"
                                    }
                        )

                        if (notice.batchId.isNotBlank()) {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            val batch =
                                BatchStore.findBatch(
                                    notice.batchId
                                )

                            NoticeDetailRow(
                                icon =
                                    Icons.Default.Groups,

                                label =
                                    "Batch",

                                value =
                                    batch?.name
                                        ?.ifBlank {
                                            "Batch Notice"
                                        }
                                        ?: "Batch Notice"
                            )
                        } else {

                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )

                            NoticeDetailRow(
                                icon =
                                    Icons.Default.School,

                                label =
                                    "Audience",

                                value =
                                    "Entire Coaching"
                            )
                        }
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    color =
                        Color(0xFFE8F5E9),

                    shape =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            12.dp
                        )
                ) {

                    Text(
                        text =
                            "✓ Notice read",

                        modifier =
                            Modifier.padding(14.dp),

                        color =
                            Color(0xFF2E7D32),

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


/* =========================================================
 * DETAIL ROW
 * ========================================================= */

@Composable
private fun NoticeDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.Top
    ) {

        Icon(
            imageVector =
                icon,

            contentDescription =
                label,

            modifier =
                Modifier.size(20.dp),

            tint =
                MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    label,

                style =
                    MaterialTheme.typography.labelMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text =
                    value,

                style =
                    MaterialTheme.typography.bodyMedium,

                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}


/* =========================================================
 * EMPTY STATE
 * ========================================================= */

@Composable
private fun EmptyTeacherNoticeState() {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Default.Campaign,

                contentDescription =
                    "No notices",

                modifier =
                    Modifier.size(64.dp),

                tint =
                    Color(0xFFBDBDBD)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            Text(
                text =
                    "No notices available",

                style =
                    MaterialTheme.typography.titleMedium,

                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    "New coaching announcements will appear here.",

                style =
                    MaterialTheme.typography.bodyMedium,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}