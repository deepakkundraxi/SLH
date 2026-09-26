package com.slh.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SLHUpdateDialog(
    updateInfo: SLHUpdateInfo,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {

    AlertDialog(

        onDismissRequest = {

            if (!updateInfo.forceUpdate) {
                onLater()
            }
        },

        title = {

            Text(
                text = "SLH Update Available"
            )
        },

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Text(
                    text =
                        "New version ${updateInfo.versionName} is available."
                )


                if (
                    updateInfo.releaseNotes
                        .isNotBlank()
                ) {

                    Text(
                        text =
                            updateInfo.releaseNotes
                    )
                }


                if (
                    updateInfo.forceUpdate
                ) {

                    Text(
                        text =
                            "This update is required to continue using SLH."
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = onUpdate
            ) {

                Text(
                    text = "Download & Update"
                )
            }
        },

        dismissButton = {

            if (
                !updateInfo.forceUpdate
            ) {

                TextButton(
                    onClick = onLater
                ) {

                    Text(
                        text = "Later"
                    )
                }
            }
        }
    )
}