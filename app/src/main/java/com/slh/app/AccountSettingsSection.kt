package com.slh.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Settings: biometric only.
 * Language option removed.
 * Delete account and auto-logout are not included.
 */
@Composable
fun AccountSettingsSection(
    staffUser: DemoUser? = null,
    student: Student? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var biometricOn by remember {
        mutableStateOf(
            BiometricHelper.isEnabled(context)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Biometric
            if (BiometricHelper.isHardwareAvailable(context)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = AppStrings.enableBiometric
                    )

                    Switch(
                        checked = biometricOn,
                        onCheckedChange = { enabled ->

                            if (enabled) {

                                val u =
                                    staffUser?.username
                                        ?: student?.username

                                val role =
                                    staffUser?.role
                                        ?: UserRole.STUDENT

                                if (!u.isNullOrBlank()) {

                                    BiometricHelper.enable(
                                        context,
                                        u,
                                        role
                                    )

                                    biometricOn = true
                                }

                            } else {

                                BiometricHelper.disable(context)

                                biometricOn = false
                            }
                        }
                    )
                }

            } else {

                Text(
                    text = "Biometric not available on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}