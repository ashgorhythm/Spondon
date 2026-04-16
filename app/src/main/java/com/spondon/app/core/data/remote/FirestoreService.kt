package com.spondon.app.core.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    // ─── Users ───────────────────────────────────────────────────

    suspend fun createUser(userId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(data)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create user", e)
        }
    }

    suspend fun getUser(userId: String): Resource<Map<String, Any>> {
        return try {
            val doc = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            if (doc.exists()) {
                Resource.Success(doc.data ?: emptyMap())
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user", e)
        }
    }

    suspend fun updateUser(userId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(data, SetOptions.merge())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update user", e)
        }
    }

    suspend fun deleteUser(userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete user", e)
        }
    }

    fun observeUser(userId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = firestore.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.data)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateFcmToken(userId: String, token: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", token)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update FCM token", e)
        }
    }

    // ─── Communities ──────────────────────────────────────────────

    suspend fun createCommunity(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create community", e)
        }
    }

    suspend fun getCommunity(communityId: String): Resource<Map<String, Any>> {
        return try {
            val doc = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .get()
                .await()
            if (doc.exists()) {
                Resource.Success((doc.data ?: emptyMap()) + ("id" to doc.id))
            } else {
                Resource.Error("Community not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get community", e)
        }
    }

    suspend fun updateCommunity(communityId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .set(data, SetOptions.merge())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update community", e)
        }
    }

    // ─── Blood Requests ───────────────────────────────────────────

    suspend fun createRequest(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.REQUESTS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create request", e)
        }
    }

    suspend fun getRequest(requestId: String): Resource<Map<String, Any>> {
        return try {
            val doc = firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .get()
                .await()
            if (doc.exists()) {
                Resource.Success((doc.data ?: emptyMap()) + ("id" to doc.id))
            } else {
                Resource.Error("Request not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get request", e)
        }
    }

    suspend fun updateRequest(requestId: String, data: Map<String, Any?>): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .set(data, SetOptions.merge())
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update request", e)
        }
    }

    // ─── Notifications ───────────────────────────────────────────

    suspend fun createNotification(userId: String, data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .collection(Constants.NOTIFICATIONS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create notification", e)
        }
    }

    // ─── Donations ───────────────────────────────────────────────

    suspend fun createDonation(data: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.DONATIONS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record donation", e)
        }
    }
}