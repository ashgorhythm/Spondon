package com.spondon.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.local.PreferencesManager
import com.spondon.app.core.data.repository.AuthRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class AuthState(
    // ── Sign Up Step 1: Basic Info ───
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val agreedToTerms: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,

    // ── Sign Up Step 2: Health Profile ───
    val selectedBloodGroup: String = "",
    val dateOfBirth: Long? = null,
    val weight: String = "",
    val lastDonationDate: Long? = null,
    val wantsToBeDonor: Boolean = true,

    // ── Sign Up Step 3: Location ───
    val selectedDistrict: String = "",
    val selectedUpazila: String = "",

    // ── Login ───
    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,

    // ── OTP ───
    val otpDigits: List<String> = List(6) { "" },
    val verificationId: String = "",
    val otpTimerSeconds: Int = 60,
    val otpPhone: String = "",

    // ── Forgot Password ───
    val forgotPasswordStep: Int = 0,
    val resetEmail: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val newPasswordVisible: Boolean = false,

    // ── General State ───
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUpComplete: Boolean = false,
    val isLoginComplete: Boolean = false,
    val isPasswordResetSuccess: Boolean = false,
    val isOnboardingComplete: Boolean = false,
    val isLoggedIn: Boolean = false,
    val needsProfileSetup: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            val isOnboarded = preferencesManager.isOnboardingComplete.first()
            val isLoggedIn = authRepository.isLoggedIn()
            // If user has blood group set, profile setup is complete
            var needsSetup = false
            if (isLoggedIn) {
                val uid = authRepository.getCurrentUserId()
                if (uid != null) {
                    when (val result = userRepository.getUser(uid)) {
                        is Resource.Success -> {
                            needsSetup = result.data.bloodGroup.isEmpty()
                        }
                        else -> {}
                    }
                }
            }
            _state.update {
                it.copy(
                    isOnboardingComplete = isOnboarded,
                    isLoggedIn = isLoggedIn,
                    needsProfileSetup = needsSetup,
                )
            }
        }
    }

    // ─── Onboarding ──────────────────────────────────────────

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingComplete(true)
            _state.update { it.copy(isOnboardingComplete = true) }
        }
    }

    // ─── Sign Up Step 1 ──────────────────────────────────────

    fun updateFullName(name: String) = _state.update { it.copy(fullName = name, error = null) }
    fun updatePhone(phone: String) = _state.update { it.copy(phone = phone, error = null) }
    fun updateEmail(email: String) = _state.update { it.copy(email = email, error = null) }
    fun updatePassword(pw: String) = _state.update { it.copy(password = pw, error = null) }
    fun updateConfirmPassword(pw: String) = _state.update { it.copy(confirmPassword = pw, error = null) }
    fun togglePasswordVisibility() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleConfirmPasswordVisibility() = _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    fun toggleTermsAgreement() = _state.update { it.copy(agreedToTerms = !it.agreedToTerms) }

    fun isStep1Valid(): Boolean {
        val s = _state.value
        return s.fullName.isNotBlank() &&
            s.phone.length >= 11 &&
            s.email.contains("@") &&
            s.password.length >= 6 &&
            s.password == s.confirmPassword &&
            s.agreedToTerms
    }

    fun getPasswordStrength(): PasswordStrength {
        val pw = _state.value.password
        if (pw.length < 6) return PasswordStrength.WEAK
        val hasUpper = pw.any { it.isUpperCase() }
        val hasDigit = pw.any { it.isDigit() }
        val hasSpecial = pw.any { !it.isLetterOrDigit() }
        val score = listOf(pw.length >= 8, hasUpper, hasDigit, hasSpecial).count { it }
        return when {
            score >= 3 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    // ─── Sign Up Step 2 ──────────────────────────────────────

    fun selectBloodGroup(group: String) = _state.update { it.copy(selectedBloodGroup = group, error = null) }
    fun updateDateOfBirth(millis: Long) = _state.update { it.copy(dateOfBirth = millis, error = null) }
    fun updateWeight(w: String) = _state.update { it.copy(weight = w, error = null) }
    fun updateLastDonationDate(millis: Long?) = _state.update { it.copy(lastDonationDate = millis, error = null) }
    fun toggleDonorWillingness() = _state.update { it.copy(wantsToBeDonor = !it.wantsToBeDonor) }

    fun isStep2Valid(): Boolean {
        val s = _state.value
        return s.selectedBloodGroup.isNotBlank() && s.dateOfBirth != null
    }

    fun getAge(): Int? {
        val dob = _state.value.dateOfBirth ?: return null
        val diff = System.currentTimeMillis() - dob
        return (diff / (365.25 * 24 * 60 * 60 * 1000)).toInt()
    }

    // ─── Sign Up Step 3 ──────────────────────────────────────

    fun selectDistrict(district: String) = _state.update { it.copy(selectedDistrict = district, selectedUpazila = "", error = null) }
    fun selectUpazila(upazila: String) = _state.update { it.copy(selectedUpazila = upazila, error = null) }

    fun isStep3Valid(): Boolean {
        val s = _state.value
        return s.selectedDistrict.isNotBlank() && s.selectedUpazila.isNotBlank()
    }

    fun completeSignUp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            val user = User(
                name = s.fullName,
                phone = s.phone,
                email = s.email,
                bloodGroup = s.selectedBloodGroup,
                dob = s.dateOfBirth?.let { Date(it) },
                weight = s.weight.toFloatOrNull() ?: 0f,
                isDonor = s.wantsToBeDonor,
                lastDonationDate = s.lastDonationDate?.let { Date(it) },
                district = s.selectedDistrict,
                upazila = s.selectedUpazila,
            )
            when (val result = authRepository.signUpWithEmail(s.email, s.password, user)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, isSignUpComplete = true) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Login ───────────────────────────────────────────────

    fun updateLoginEmail(email: String) = _state.update { it.copy(loginEmail = email, error = null) }
    fun updateLoginPassword(pw: String) = _state.update { it.copy(loginPassword = pw, error = null) }
    fun toggleLoginPasswordVisibility() = _state.update { it.copy(loginPasswordVisible = !it.loginPasswordVisible) }
    fun toggleRememberMe() = _state.update { it.copy(rememberMe = !it.rememberMe) }

    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            when (val result = authRepository.signInWithEmail(s.loginEmail, s.loginPassword)) {
                is Resource.Success -> {
                    if (s.rememberMe) {
                        preferencesManager.setRememberMe(true)
                        preferencesManager.setSavedEmail(s.loginEmail)
                    }
                    _state.update { it.copy(isLoading = false, isLoginComplete = true) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> {
                    val needsSetup = result.data.bloodGroup.isEmpty()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoginComplete = !needsSetup,
                            needsProfileSetup = needsSetup,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── OTP ─────────────────────────────────────────────────

    fun updateOtpDigit(index: Int, digit: String) {
        val digits = _state.value.otpDigits.toMutableList()
        digits[index] = digit.take(1)
        _state.update { it.copy(otpDigits = digits, error = null) }
    }

    fun setVerificationId(id: String) = _state.update { it.copy(verificationId = id) }
    fun setOtpPhone(phone: String) = _state.update { it.copy(otpPhone = phone) }
    fun updateOtpTimer(seconds: Int) = _state.update { it.copy(otpTimerSeconds = seconds) }

    fun verifyOtp() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val code = _state.value.otpDigits.joinToString("")
            if (code.length != 6) {
                _state.update { it.copy(isLoading = false, error = "Please enter a valid 6-digit code") }
                return@launch
            }
            when (val result = authRepository.verifyOtp(_state.value.verificationId, code)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, isLoginComplete = true) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Forgot Password ─────────────────────────────────────

    fun updateResetEmail(email: String) = _state.update { it.copy(resetEmail = email, error = null) }
    fun updateNewPassword(pw: String) = _state.update { it.copy(newPassword = pw, error = null) }
    fun updateConfirmNewPassword(pw: String) = _state.update { it.copy(confirmNewPassword = pw, error = null) }
    fun toggleNewPasswordVisibility() = _state.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.resetPassword(_state.value.resetEmail)) {
                is Resource.Success -> {
                    _state.update { it.copy(isLoading = false, isPasswordResetSuccess = true) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun advanceForgotPasswordStep() = _state.update { it.copy(forgotPasswordStep = it.forgotPasswordStep + 1) }

    // ─── Utility ─────────────────────────────────────────────

    fun clearError() = _state.update { it.copy(error = null) }

    // ─── Activity-bridge helpers ─────────────────────────────

    /** Called by MainActivity when Google Sign-In fails before we get an id token. */
    fun setError(msg: String) = _state.update { it.copy(error = msg, isLoading = false) }

    /** Shows the loading spinner while OTP is being sent (Activity layer). */
    fun setOtpLoading(loading: Boolean) = _state.update { it.copy(isLoading = loading) }

    /** Allows MainActivity to run a suspend block on the ViewModel's scope. */
    fun launchOnScope(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    /**
     * Signs in using a [PhoneAuthCredential] obtained from auto-verification
     * (called from MainActivity's PhoneAuthProvider callbacks).
     */
    fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Delegate to the repository's verifyOtp by reusing the credential directly
                when (val result = authRepository.signInWithPhoneCredential(credential)) {
                    is Resource.Success -> _state.update { it.copy(isLoading = false, isLoginComplete = true) }
                    is Resource.Error   -> _state.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Sign in failed") }
            }
        }
    }
}

enum class PasswordStrength { WEAK, FAIR, STRONG }