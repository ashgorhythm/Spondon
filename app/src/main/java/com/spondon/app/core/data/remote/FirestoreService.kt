package com.spondon.app.core.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
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

    /**
     * Fetches multiple users by their IDs.
     */
    suspend fun getUsers(userIds: List<String>): Resource<List<Map<String, Any>>> {
        if (userIds.isEmpty()) return Resource.Success(emptyList())
        return try {
            // Firestore `whereIn` supports max 30 items at a time
            val results = mutableListOf<Map<String, Any>>()
            userIds.chunked(30).forEach { chunk ->
                val docs = firestore.collection(Constants.USERS_COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                docs.documents.forEach { doc ->
                    if (doc.exists()) {
                        results.add((doc.data ?: emptyMap()) + ("uid" to doc.id))
                    }
                }
            }
            Resource.Success(results)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get users", e)
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

    /**
     * Fetches all public communities, ordered by creation date.
     */
    suspend fun getAllCommunities(): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get communities", e)
        }
    }

    /**
     * Fetches communities that the user is a member of.
     */
    suspend fun getMyCommunities(userId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .whereArrayContains("memberIds", userId)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user communities", e)
        }
    }

    /**
     * Adds a user to a public community.
     */
    suspend fun joinCommunity(communityId: String, userId: String): Resource<Unit> {
        return try {
            val ref = firestore.collection(Constants.COMMUNITIES_COLLECTION).document(communityId)
            ref.update(
                mapOf(
                    "memberIds" to FieldValue.arrayUnion(userId),
                    "memberCount" to FieldValue.increment(1),
                )
            ).await()
            // Also add community to user's communityIds
            firestore.collection(Constants.USERS_COLLECTION).document(userId)
                .update("communityIds", FieldValue.arrayUnion(communityId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to join community", e)
        }
    }

    /**
     * Removes a user from a community.
     */
    suspend fun leaveCommunity(communityId: String, userId: String): Resource<Unit> {
        return try {
            val ref = firestore.collection(Constants.COMMUNITIES_COLLECTION).document(communityId)
            ref.update(
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(userId),
                    "adminIds" to FieldValue.arrayRemove(userId),
                    "moderatorIds" to FieldValue.arrayRemove(userId),
                    "memberCount" to FieldValue.increment(-1),
                )
            ).await()
            firestore.collection(Constants.USERS_COLLECTION).document(userId)
                .update("communityIds", FieldValue.arrayRemove(communityId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to leave community", e)
        }
    }

    /**
     * Adds a user to the pending list for a private community.
     */
    suspend fun addPendingMember(communityId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update("pendingIds", FieldValue.arrayUnion(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add pending member", e)
        }
    }

    /**
     * Removes a user from the pending list.
     */
    suspend fun removePendingMember(communityId: String, userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update("pendingIds", FieldValue.arrayRemove(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove pending member", e)
        }
    }

    // ─── Join Requests (subcollection) ─────────────────────────────

    /**
     * Creates a join request document in communities/{id}/joinRequests
     */
    suspend fun createJoinRequest(
        communityId: String,
        data: Map<String, Any?>,
    ): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .collection(Constants.JOIN_REQUESTS_COLLECTION)
                .add(data)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create join request", e)
        }
    }

    /**
     * Fetches pending join requests for a community.
     */
    suspend fun getPendingJoinRequests(communityId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .collection(Constants.JOIN_REQUESTS_COLLECTION)
                .whereEqualTo("status", "PENDING")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get join requests", e)
        }
    }

    /**
     * Updates a join request's status (APPROVED / REJECTED).
     */
    suspend fun updateJoinRequestStatus(
        communityId: String,
        requestId: String,
        status: String,
        rejectionNote: String? = null,
    ): Resource<Unit> {
        return try {
            val data = mutableMapOf<String, Any>("status" to status)
            if (rejectionNote != null) data["rejectionNote"] = rejectionNote
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .collection(Constants.JOIN_REQUESTS_COLLECTION)
                .document(requestId)
                .update(data)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update join request", e)
        }
    }

    /**
     * Promotes a member to a role (admin or moderator).
     */
    suspend fun promoteMember(
        communityId: String,
        userId: String,
        roleField: String,
    ): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update(roleField, FieldValue.arrayUnion(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to promote member", e)
        }
    }

    /**
     * Demotes a member from a role.
     */
    suspend fun demoteMember(
        communityId: String,
        userId: String,
        roleField: String,
    ): Resource<Unit> {
        return try {
            firestore.collection(Constants.COMMUNITIES_COLLECTION)
                .document(communityId)
                .update(roleField, FieldValue.arrayRemove(userId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to demote member", e)
        }
    }

    /**
     * Observes a single community document in real-time.
     */
    fun observeCommunity(communityId: String): Flow<Map<String, Any>?> = callbackFlow {
        val listener = firestore.collection(Constants.COMMUNITIES_COLLECTION)
            .document(communityId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend((snapshot.data ?: emptyMap()) + ("id" to snapshot.id))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Updates a member's donation status and last donation date.
     */
    suspend fun updateMemberDonationStatus(
        userId: String,
        lastDonationDate: com.google.firebase.Timestamp,
        totalDonations: Int,
    ): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "lastDonationDate" to lastDonationDate,
                        "totalDonations" to totalDonations,
                        "availabilityOverride" to false,
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update donation status", e)
        }
    }

    /**
     * Overrides a member's availability (admin sets available early at ≥90 days).
     */
    suspend fun overrideMemberAvailability(userId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("availabilityOverride", true)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to override availability", e)
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

    /**
     * Fetches requests for the given community IDs.
     * Firestore `whereArrayContainsAny` supports max 30 values.
     */
    suspend fun getRequestsForCommunities(
        communityIds: List<String>,
    ): Resource<List<Map<String, Any>>> {
        if (communityIds.isEmpty()) return Resource.Success(emptyList())
        return try {
            val results = mutableListOf<Map<String, Any>>()
            communityIds.chunked(30).forEach { chunk ->
                val docs = firestore.collection(Constants.REQUESTS_COLLECTION)
                    .whereArrayContainsAny("communityIds", chunk)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                docs.documents.forEach { doc ->
                    if (doc.exists()) {
                        results.add((doc.data ?: emptyMap()) + ("id" to doc.id))
                    }
                }
            }
            Resource.Success(results)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get requests", e)
        }
    }

    /**
     * Fetches requests created by a specific user.
     */
    suspend fun getRequestsByUser(userId: String): Resource<List<Map<String, Any>>> {
        return try {
            val docs = firestore.collection(Constants.REQUESTS_COLLECTION)
                .whereEqualTo("requesterId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val list = docs.documents.mapNotNull { doc ->
                if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
            }
            Resource.Success(list)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user requests", e)
        }
    }

    /**
     * Observes requests from the given communities in real-time.
     */
    fun observeRequestsForCommunities(communityIds: List<String>): Flow<List<Map<String, Any>>> =
        callbackFlow {
            if (communityIds.isEmpty()) {
                trySend(emptyList())
                awaitClose()
                return@callbackFlow
            }
            // Use the first 10 communities for the listener (Firestore limit)
            val limitedIds = communityIds.take(10)
            val listener = firestore.collection(Constants.REQUESTS_COLLECTION)
                .whereArrayContainsAny("communityIds", limitedIds)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        if (doc.exists()) (doc.data ?: emptyMap()) + ("id" to doc.id) else null
                    } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }

    /**
     * Adds a respondent (donor) to a request.
     */
    suspend fun addRespondent(requestId: String, donorId: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .update("respondents", FieldValue.arrayUnion(donorId))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to respond to request", e)
        }
    }

    /**
     * Updates request status.
     */
    suspend fun updateRequestStatus(requestId: String, status: String): Resource<Unit> {
        return try {
            firestore.collection(Constants.REQUESTS_COLLECTION)
                .document(requestId)
                .update("status", status)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update request status", e)
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