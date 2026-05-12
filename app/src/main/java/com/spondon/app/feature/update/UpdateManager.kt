package com.spondon.app.feature.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.spondon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
    }

    /**
     * Downloads the APK from the given URL.
     *
     * For **private** GitHub repos the `browser_download_url` redirects through
     * a GitHub auth gate.  `DownloadManager` cannot add the required
     * `Authorization` header on the redirect, so the download silently fails.
     *
     * This implementation therefore does two things:
     * 1. If a GitHub token is available, it downloads the APK manually with
     *    an authenticated `HttpURLConnection`, following redirects.
     * 2. If no token is available (public repo), it falls back to the system
     *    `DownloadManager` for a nicer notification-based UX.
     */
    fun downloadUpdate(apkUrl: String) {
        val token = BuildConfig.GITHUB_TOKEN
        if (token.isNotBlank() && apkUrl.contains("github.com")) {
            // Private repo: manual authenticated download
            downloadWithAuth(apkUrl, token)
        } else {
            // Public repo: use system DownloadManager
            downloadWithDownloadManager(apkUrl)
        }
    }

    // ── Authenticated download (private repos) ────────────────────

    private fun downloadWithAuth(apkUrl: String, token: String) {
        Thread {
            try {
                val file = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "update.apk"
                )
                if (file.exists()) file.delete()

                // GitHub asset URLs need Accept: application/octet-stream
                // and Authorization header to get the actual binary.
                // We use the *API* URL format instead of browser_download_url.
                val downloadUrl = convertToApiUrl(apkUrl)
                Log.d(TAG, "Authenticated download from: $downloadUrl")

                val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/octet-stream")
                connection.setRequestProperty("User-Agent", "Spondon-Android-App")
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.connect()

                // GitHub may redirect — follow manually if needed
                var conn = connection
                var responseCode = conn.responseCode
                var redirectCount = 0

                while (responseCode in 301..303 || responseCode == 307 || responseCode == 308) {
                    if (++redirectCount > 5) {
                        Log.e(TAG, "Too many redirects")
                        return@Thread
                    }
                    val redirectUrl = conn.getHeaderField("Location")
                    Log.d(TAG, "Redirecting to: $redirectUrl")
                    conn.disconnect()

                    conn = URL(redirectUrl).openConnection() as HttpURLConnection
                    // Don't send auth header to S3 redirect — it will fail
                    conn.setRequestProperty("User-Agent", "Spondon-Android-App")
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 60_000
                    conn.connect()
                    responseCode = conn.responseCode
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errBody = try {
                        conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                    } catch (_: Exception) { "unreadable" }
                    Log.e(TAG, "Download failed with $responseCode: $errBody")
                    conn.disconnect()
                    return@Thread
                }

                // Stream to file
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
                conn.disconnect()

                Log.d(TAG, "Download complete: ${file.absolutePath} (${file.length()} bytes)")

                // Install on main thread
                android.os.Handler(context.mainLooper).post {
                    installApk()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Authenticated download failed", e)
            }
        }.start()
    }

    /**
     * Converts a `browser_download_url` to the GitHub API asset URL that
     * accepts `Accept: application/octet-stream` for binary download.
     *
     * Example:
     *   Input:  https://github.com/user/repo/releases/download/v1.0.9/App.apk
     *   Output: https://api.github.com/repos/user/repo/releases/assets/{id}
     *
     * Since we don't have the asset ID handy, we fall back to using the
     * browser URL directly with auth headers — GitHub's server will redirect
     * properly when Authorization is present.
     */
    private fun convertToApiUrl(browserUrl: String): String {
        // The browser_download_url already works with Authorization header,
        // so we just return it as-is. GitHub will authenticate and serve the file.
        return browserUrl
    }

    // ── DownloadManager fallback (public repos) ───────────────────

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

    private fun downloadWithDownloadManager(apkUrl: String) {
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

    // ── APK installation ──────────────────────────────────────────

    fun installApk(ctx: Context = context) {
        val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")

        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "APK file missing or empty: ${file.absolutePath}")
            return
        }

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
