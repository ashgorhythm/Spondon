package com.spondon.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.spondon.app.core.ui.theme.SpondonTheme
import com.spondon.app.feature.auth.AuthViewModel
import com.spondon.app.navigation.SpondonNavGraph
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ── Firebase Auth ──────────────────────────────────────────
    @Inject lateinit var firebaseAuth: FirebaseAuth

    // Activity-scoped ViewModel shared with all screens via hiltViewModel()
    private val authViewModel: AuthViewModel by viewModels()

    // ── Credential Manager (modern Google Sign-In replacement) ─
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        credentialManager = CredentialManager.create(this)

        setContent {
            SpondonTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpondonNavGraph(
                        authViewModel  = authViewModel,
                        onGoogleSignIn = { launchGoogleSignIn() },
                        onSendOtp      = { phone -> sendOtp(phone) },
                    )
                }
            }
        }
    }

    // ── Google Sign-In via Credential Manager ──────────────────

    private fun launchGoogleSignIn() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = this@MainActivity,
                )
                handleGoogleSignInResult(result)
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignIn", "Credential request failed", e)
                val userMessage = when {
                    e is androidx.credentials.exceptions.NoCredentialException ->
                        "No Google account found. Please add a Google account in device settings."
                    e.message?.contains("cancel", ignoreCase = true) == true ->
                        "Google Sign-In was cancelled."
                    else ->
                        "Google Sign-In failed. Please try again."
                }
                authViewModel.setError(userMessage)
            }
        }
    }

    private fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        authViewModel.signInWithGoogle(idToken)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("GoogleSignIn", "Invalid Google ID token", e)
                        authViewModel.setError("Google Sign-In failed: invalid token")
                    }
                } else {
                    Log.e("GoogleSignIn", "Unexpected credential type: ${credential.type}")
                    authViewModel.setError("Google Sign-In failed: unexpected credential type")
                }
            }
            else -> {
                Log.e("GoogleSignIn", "Unexpected credential class: ${credential.javaClass}")
                authViewModel.setError("Google Sign-In failed: unexpected credential")
            }
        }
    }

    // ── Phone (OTP) Auth ───────────────────────────────────────

    /**
     * Sends an OTP to [phoneNumber] using Firebase PhoneAuthProvider.
     * The verification ID is stored in AuthViewModel for later verifyOtp() use.
     */
    private fun sendOtp(phoneNumber: String) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification (instant verify on device) — sign in directly
                authViewModel.signInWithPhoneCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("PhoneAuth", "onVerificationFailed", e)
                authViewModel.setError(e.message ?: "OTP send failed")
                authViewModel.setOtpLoading(false)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken,
            ) {
                authViewModel.setVerificationId(verificationId)
                authViewModel.setOtpPhone(phoneNumber)
                authViewModel.setOtpLoading(false)
            }
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()

        authViewModel.setOtpLoading(true)
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}