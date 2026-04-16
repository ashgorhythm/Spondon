package com.spondon.app.core.domain.usecase.auth

import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.AuthRepository
import com.spondon.app.core.domain.model.User
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        user: User,
    ): Resource<User> {
        if (user.name.isBlank()) return Resource.Error("Full name is required")
        if (email.isBlank() || !email.contains("@")) return Resource.Error("Valid email is required")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters")
        if (user.phone.length < 11) return Resource.Error("Valid phone number is required")
        return authRepository.signUpWithEmail(email, password, user)
    }
}