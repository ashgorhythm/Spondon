package com.spondon.app.feature.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.spondon.app.MainActivity
import com.spondon.app.R
import com.spondon.app.core.common.Constants
import com.spondon.app.core.data.repository.NotificationRepositoryImpl
import com.spondon.app.core.domain.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpondonFCMService : FirebaseMessagingService() {

    @Inject lateinit var notificationRepository: NotificationRepositoryImpl
    @Inject lateinit var firestore: FirebaseFirestore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val CHANNEL_ID = "spondon_notifications"
        private const val CHANNEL_NAME = "Spondon Notifications"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "Spondon"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val typeStr = message.data["type"] ?: "REQUEST"
        val deepLink = message.data["deepLink"] ?: ""

        val type = try {
            NotificationType.valueOf(typeStr)
        } catch (_: Exception) {
            NotificationType.REQUEST
        }

        // Save to Firestore via repository
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            serviceScope.launch {
                notificationRepository.createNotification(
                    userId = uid,
                    type = type,
                    title = title,
                    body = body,
                    deepLink = deepLink,
                )
            }
        }

        // Show system notification
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Blood donation alerts and community notifications"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}