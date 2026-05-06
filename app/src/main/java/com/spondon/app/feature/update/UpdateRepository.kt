package com.spondon.app.feature.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

class UpdateRepository @Inject constructor() {

    suspend fun checkGitHubForUpdate(currentVersion: String): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/ashanokoji/Spondon/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                // A timeout prevents hanging
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val latestVersion = json.getString("tag_name") // e.g. "v1.0.0"

                    // Use proper version comparison or simple string mismatch for now
                    // To be safe, let's just use string mismatch as per the guide.
                    if (latestVersion != currentVersion) {
                        val assets = json.getJSONArray("assets")

                        val apkUrl = (0 until assets.length())
                            .map { assets.getJSONObject(it) }
                            .firstOrNull { it.getString("name").endsWith(".apk") }
                            ?.getString("browser_download_url")

                        if (apkUrl != null) {
                            return@withContext UpdateInfo(latestVersion, apkUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext null
        }
    }
}
