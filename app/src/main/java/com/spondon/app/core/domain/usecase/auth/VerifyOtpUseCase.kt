package com.spondon.app.core.domain.usecase.auth

import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(verificationId: String, code: String): Resource<Boolean> {
        if (code.length != Constants.OTP_LENGTH) {
            return Resource.Error("Please enter a valid ${Constants.OTP_LENGTH}-digit code")
        }
        if (verificationId.isBlank()) {
            return Resource.Error("Verification session expired. Please request a new code.")
        }
        return authRepository.verifyOtp(verificationId, code)
    }
}