package com.spondon.app.feature.update

import android.util.Log
import com.spondon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepository @Inject constructor() {

    companion object {
        private const val TAG = "UpdateRepository"
        private const val RELEASES_URL =
            "https://api.github.com/repos/ashanokoji/Spondon/releases/latest"
    }

    /**
     * Checks GitHub for a newer release.
     *
     * @param currentVersionName The app's current version name (e.g. "1.0.7").
     *        The leading "v" is stripped automatically.
     * @return [UpdateInfo] if a newer version is found, `null` otherwise.
     */
    suspend fun checkGitHubForUpdate(currentVersionName: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(RELEASES_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "Spondon-Android-App")

                // Authenticate for private repo access
                val token = BuildConfig.GITHUB_TOKEN
                if (token.isNotBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $token")
                }

                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000

                val responseCode = connection.responseCode
                Log.d(TAG, "GitHub API response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    val tagName = json.optString("tag_name", "") // e.g. "v1.0.8"
                    val releaseNotes = json.optString("body", "")

                    val remoteVersion = tagName.removePrefix("v").trim()
                    val localVersion = currentVersionName.removePrefix("v").trim()

                    Log.d(TAG, "Remote: $remoteVersion | Local: $localVersion")

                    if (isNewerVersion(remoteVersion, localVersion)) {
                        val assets = json.optJSONArray("assets")
                        Log.d(TAG, "Assets count: ${assets?.length() ?: 0}")

                        var apkUrl: String? = null
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val assetName = asset.optString("name", "")
                                val browserUrl = asset.optString("browser_download_url", "")
                                val apiUrl = asset.optString("url", "") // API URL for authenticated download
                                Log.d(TAG, "Asset[$i]: name=$assetName, browserUrl=$browserUrl, apiUrl=$apiUrl")

                                if (assetName.endsWith(".apk")) {
                                    // Prefer browser_download_url — UpdateManager handles auth
                                    apkUrl = browserUrl.ifBlank { null }
                                    break
                                }
                            }
                        }

                        if (apkUrl != null) {
                            Log.d(TAG, "Update available: $remoteVersion → $apkUrl")
                            return@withContext UpdateInfo(
                                version = remoteVersion,
                                downloadUrl = apkUrl,
                                releaseNotes = releaseNotes
                            )
                        } else {
                            Log.w(TAG, "Newer version found but no APK asset attached to release")
                        }
                    } else {
                        Log.d(TAG, "App is up to date")
                    }
                } else {
                    val errorBody = try {
                        connection.errorStream?.bufferedReader()?.readText() ?: "no error body"
                    } catch (_: Exception) { "unreadable" }
                    Log.w(TAG, "GitHub API returned $responseCode: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
            }
            return@withContext null
        }
    }

    /**
     * Semantic version comparison.
     * Returns `true` if [remote] is strictly newer than [local].
     */
    private fun isNewerVersion(remote: String, local: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false // equal
    }
}
