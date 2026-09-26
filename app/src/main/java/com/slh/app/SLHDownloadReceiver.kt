package com.slh.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.DownloadManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

class SLHDownloadReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            DownloadManager.ACTION_DOWNLOAD_COMPLETE
        ) {
            return
        }

        val downloadId =
            intent.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID,
                -1L
            )

        if (downloadId == -1L) {
            return
        }


        val downloadManager =
            context.getSystemService(
                Context.DOWNLOAD_SERVICE
            ) as DownloadManager


        val cursor =
            downloadManager.query(
                DownloadManager.Query()
                    .setFilterById(downloadId)
            )


        cursor.use {

            if (!it.moveToFirst()) {
                return
            }

            val status =
                it.getInt(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_STATUS
                    )
                )


            if (
                status !=
                DownloadManager.STATUS_SUCCESSFUL
            ) {
                return
            }


            val uriString =
                it.getString(
                    it.getColumnIndexOrThrow(
                        DownloadManager.COLUMN_LOCAL_URI
                    )
                )


            if (
                uriString.isNullOrBlank()
            ) {
                return
            }


            val downloadedUri =
                Uri.parse(uriString)


            val filePath =
                downloadedUri.path
                    ?: return


            val file =
                File(filePath)


            if (!file.exists()) {
                return
            }


            val apkUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )


            val installIntent =
                Intent(
                    Intent.ACTION_VIEW
                ).apply {

                    setDataAndType(
                        apkUri,
                        "application/vnd.android.package-archive"
                    )

                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }


            context.startActivity(
                installIntent
            )
        }
    }
}