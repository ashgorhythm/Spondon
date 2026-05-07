package com.spondon.app.feature.update

data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String = ""
)
