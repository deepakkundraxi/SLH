package com.slh.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class SLHUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val forceUpdate: Boolean,
    val releaseNotes: String
)

object AppUpdateManager {

    /*
     * =========================================================
     * GITHUB CONFIGURATION
     * =========================================================
     *
     * बाद में इन 2 values को अपने GitHub username/repository
     * के अनुसार बदलना है।
     */

    private const val GITHUB_OWNER = "YOUR_GITHUB_USERNAME"
    private const val GITHUB_REPOSITORY = "SLH"

    private const val VERSION_FILE =
        "https://raw.githubusercontent.com/$GITHUB_OWNER/$GITHUB_REPOSITORY/main/version.json"

    /*
     * =========================================================
     * CHECK UPDATE
     * =========================================================
     */

    suspend fun checkForUpdate(
        context: Context
    ): SLHUpdateInfo? {

        return withContext(Dispatchers.IO) {

            try {

                val url =
                    URL(
                        VERSION_FILE +
                                "?t=" +
                                System.currentTimeMillis()
                    )

                val connection =
                    url.openConnection()
                            as HttpURLConnection

                connection.requestMethod = "GET"

                connection.connectTimeout = 10_000

                connection.readTimeout = 10_000

                connection.setRequestProperty(
                    "Cache-Control",
                    "no-cache"
                )

                connection.connect()

                if (
                    connection.responseCode !=
                    HttpURLConnection.HTTP_OK
                ) {

                    connection.disconnect()

                    return@withContext null
                }

                val jsonText =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val json =
                    JSONObject(jsonText)

                val latestVersionCode =
                    json.optInt(
                        "versionCode",
                        0
                    )

                val latestVersionName =
                    json.optString(
                        "versionName",
                        ""
                    )

                val apkUrl =
                    json.optString(
                        "apkUrl",
                        ""
                    )

                val forceUpdate =
                    json.optBoolean(
                        "forceUpdate",
                        false
                    )

                val releaseNotes =
                    json.optString(
                        "releaseNotes",
                        ""
                    )

                val currentVersionCode =
                    getCurrentVersionCode(
                        context
                    )

                if (
                    latestVersionCode >
                    currentVersionCode &&
                    apkUrl.isNotBlank()
                ) {

                    SLHUpdateInfo(

                        versionCode =
                            latestVersionCode,

                        versionName =
                            latestVersionName,

                        apkUrl =
                            apkUrl,

                        forceUpdate =
                            forceUpdate,

                        releaseNotes =
                            releaseNotes
                    )

                } else {

                    null
                }

            } catch (
                e: Exception
            ) {

                null
            }
        }
    }


    /*
     * =========================================================
     * CURRENT VERSION
     * =========================================================
     */

    private fun getCurrentVersionCode(
        context: Context
    ): Int {

        return try {

            val packageInfo =
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.P
            ) {

                packageInfo.longVersionCode
                    .toInt()

            } else {

                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

        } catch (
            e: Exception
        ) {

            0
        }
    }


    /*
     * =========================================================
     * DOWNLOAD APK
     * =========================================================
     */

    fun downloadAndInstall(
        context: Context,
        updateInfo: SLHUpdateInfo
    ) {

        try {

            val downloadManager =
                context.getSystemService(
                    Context.DOWNLOAD_SERVICE
                ) as DownloadManager


            val request =
                DownloadManager.Request(
                    Uri.parse(
                        updateInfo.apkUrl
                    )
                )


            request.setTitle(
                "SLH ${updateInfo.versionName}"
            )

            request.setDescription(
                "SLH update downloading..."
            )

            request.setNotificationVisibility(
                DownloadManager.Request
                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

            request.setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "SLH-${updateInfo.versionName}.apk"
            )


            downloadManager.enqueue(
                request
            )

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }
    }


    /*
     * =========================================================
     * INSTALL APK
     * =========================================================
     *
     * DownloadManager पूरा होने के बाद इस function को call
     * किया जाएगा।
     */

    fun installApk(
        context: Context,
        apkFile: File
    ) {

        try {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                if (
                    !context.packageManager
                        .canRequestPackageInstalls()
                ) {

                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
                        ).apply {

                            data =
                                Uri.parse(
                                    "package:${context.packageName}"
                                )

                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                        }

                    context.startActivity(
                        intent
                    )

                    return
                }
            }


            val apkUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )


            val intent =
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
                intent
            )

        } catch (
            e: Exception
        ) {

            e.printStackTrace()
        }
    }
}