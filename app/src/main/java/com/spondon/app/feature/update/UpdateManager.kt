package com.spondon.app.feature.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class UpdateManager(private val context: Context) {

    private var downloadId: Long = -1

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                installApk(ctx)
                try {
                    ctx.unregisterReceiver(this)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun downloadUpdate(apkUrl: String) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("App Update")
            .setDescription("Downloading latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        ContextCompat.registerReceiver(
            context,
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun installApk(ctx: Context = context) {
        val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (!file.exists()) return

        if (!ctx.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${ctx.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(settingsIntent)
            return
        }

        val uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.provider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ctx.startActivity(installIntent)
    }
}
