package com.spondon.app.core.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Utility for biometric authentication.
 *
 * Call [canAuthenticate] to check hardware availability, and
 * [showBiometricPrompt] to present the fingerprint/face dialog.
 */
object BiometricHelper {

    private const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /**
     * Returns true if the device has enrolled biometric credentials
     * (fingerprint or face) that are ready to use.
     */
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the system biometric prompt.
     *
     * @param activity Must be a [FragmentActivity] (ComponentActivity extends it).
     * @param title Prompt title text.
     * @param subtitle Prompt subtitle text.
     * @param negativeButtonText Text for the cancel button.
     * @param onSuccess Called when authentication succeeds.
     * @param onError Called when authentication fails or is cancelled.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Biometric Login",
        subtitle: String = "Verify your identity to continue",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User cancelled — don't treat as hard failure
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onError("CANCELLED")
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // This is a "soft" failure (e.g. finger not recognized) — don't dismiss
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
