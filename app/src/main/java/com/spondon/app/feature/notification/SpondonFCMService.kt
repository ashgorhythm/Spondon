package com.spondon.app.feature.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SpondonFCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // TODO: Handle incoming FCM messages in Phase 6
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Update FCM token in Firestore
    }
}