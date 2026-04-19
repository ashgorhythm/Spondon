package com.spondon.app.core.data.repository

import com.google.firebase.auth.PhoneAuthCredential
import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.User

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Resource<User>
    suspend fun signUpWithEmail(email: String, password: String, user: User): Resource<User>
    suspend fun signInWithGoogle(idToken: String): Resource<User>
    suspend fun sendOtp(phoneNumber: String): Resource<String>
    suspend fun verifyOtp(verificationId: String, code: String): Resource<Boolean>
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Resource<Boolean>
    suspend fun resetPassword(email: String): Resource<Unit>
    fun getCurrentUserId(): String?
    fun isLoggedIn(): Boolean
    suspend fun signOut()
}