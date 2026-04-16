package com.spondon.app.core.domain.usecase.auth

import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.AuthRepository
import com.spondon.app.core.domain.model.User
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend fun withEmail(email: String, password: String): Resource<User> {
        if (email.isBlank()) return Resource.Error("Email is required")
        if (password.isBlank()) return Resource.Error("Password is required")
        return authRepository.signInWithEmail(email, password)
    }

    suspend fun withGoogle(idToken: String): Resource<User> {
        if (idToken.isBlank()) return Resource.Error("Invalid Google token")
        return authRepository.signInWithGoogle(idToken)
    }
}