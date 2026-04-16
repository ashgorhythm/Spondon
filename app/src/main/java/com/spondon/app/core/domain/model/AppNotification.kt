package com.spondon.app.core.domain.model

import java.util.Date

data class AppNotification(
    val id: String = "",
    val type: NotificationType = NotificationType.REQUEST,
    val title: String = "",
    val body: String = "",
    val deepLink: String = "",
    val isRead: Boolean = false,
    val createdAt: Date? = null,
)