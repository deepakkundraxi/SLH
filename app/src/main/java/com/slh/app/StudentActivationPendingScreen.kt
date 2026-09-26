package com.slh.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudentActivationPendingScreen(
    user: DemoUser,
    onLogout: () -> Unit
) {

    BackHandler {
        // Student ko is screen se back karke
        // dashboard access nahi dena hai.
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            // =========================================================
            // STATUS ICON
            // =========================================================

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.10f)
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Payment,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(50.dp),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            Spacer(
                modifier =
                    Modifier.size(18.dp)
            )

            // =========================================================
            // TITLE
            // =========================================================

            Text(
                text =
                    "Account Activation Pending",
                modifier =
                    Modifier.fillMaxWidth(),
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center,
                color =
                    MaterialTheme
                        .colorScheme
                        .onBackground
            )

            Spacer(
                modifier =
                    Modifier.size(8.dp)
            )

            Text(
                text =
                    "खाता सक्रिय होना बाकी है",
                modifier =
                    Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign =
                    TextAlign.Center,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                modifier =
                    Modifier.size(22.dp)
            )

            // =========================================================
            // STUDENT DETAILS
            // =========================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surface
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                ) {

                    Text(
                        text = "Student Details",
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.size(14.dp)
                    )

                    ActivationDetailRow(
                        icon =
                            Icons.Default.Person,
                        label =
                            "Student",
                        value =
                            user.displayName
                                .ifBlank {
                                    user.username
                                }
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )

                    ActivationDetailRow(
                        icon =
                            Icons.Default.Lock,
                        label =
                            "Username",
                        value =
                            user.username
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )

                    ActivationDetailRow(
                        icon =
                            Icons.Default.Schedule,
                        label =
                            "Account Status",
                        value =
                            "PENDING"
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(18.dp)
            )

            // =========================================================
            // PAYMENT MESSAGE
            // =========================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primary
                                .copy(alpha = 0.08f)
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Payment,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(38.dp),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    Spacer(
                        modifier =
                            Modifier.size(12.dp)
                    )

                    Text(
                        text =
                            "Please ensure you make the payment to activate the app.",
                        modifier =
                            Modifier.fillMaxWidth(),
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        textAlign =
                            TextAlign.Center,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.size(12.dp)
                    )

                    Text(
                        text =
                            "ऐप को सक्रिय करने के लिए कृपया भुगतान सुनिश्चित करें।",
                        modifier =
                            Modifier.fillMaxWidth(),
                        fontSize = 15.sp,
                        fontWeight =
                            FontWeight.Medium,
                        textAlign =
                            TextAlign.Center,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(18.dp)
            )

            // =========================================================
            // ACTIVATION PROCESS
            // =========================================================

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                ) {

                    Text(
                        text =
                            "Activation Process",
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.size(14.dp)
                    )

                    ActivationStep(
                        number = "1",
                        text =
                            "Complete your payment."
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )

                    ActivationStep(
                        number = "2",
                        text =
                            "Admin verifies your payment."
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )

                    ActivationStep(
                        number = "3",
                        text =
                            "Admin approves your account."
                    )

                    Spacer(
                        modifier =
                            Modifier.size(10.dp)
                    )

                    ActivationStep(
                        number = "4",
                        text =
                            "Login again to access the Student Dashboard."
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(24.dp)
            )

            // =========================================================
            // LOGOUT
            // =========================================================

            OutlinedButton(
                onClick = onLogout,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape =
                    RoundedCornerShape(15.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Logout,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(19.dp)
                )

                Spacer(
                    modifier =
                        Modifier.size(8.dp)
                )

                Text(
                    text = "Logout",
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            Spacer(
                modifier =
                    Modifier.size(18.dp)
            )

            PoweredBySLH(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(
                            max = 300.dp
                        )
            )
        }
    }
}


// =====================================================================
// ACTIVATION DETAIL ROW
// =====================================================================

@Composable
private fun ActivationDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            modifier =
                Modifier.size(20.dp),
            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = label,
                fontSize = 11.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.SemiBold,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurface
            )
        }
    }
}


// =====================================================================
// ACTIVATION STEP
// =====================================================================

@Composable
private fun ActivationStep(
    number: String,
    text: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = number,
                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimary,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.size(12.dp)
        )

        Text(
            text = text,
            modifier =
                Modifier.weight(1f),
            fontSize = 13.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}