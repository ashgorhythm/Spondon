package com.spondon.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.spondon.app.core.ui.theme.SpondonTheme
import com.spondon.app.feature.auth.AuthViewModel
import com.spondon.app.navigation.SpondonNavGraph
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ── Firebase Auth ──────────────────────────────────────────
    @Inject lateinit var firebaseAuth: FirebaseAuth

    // Activity-scoped ViewModel shared with all screens via hiltViewModel()
    private val authViewModel: AuthViewModel by viewModels()

    // ── Google Sign-In ─────────────────────────────────────────
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupGoogleSignIn()

        setContent {
            SpondonTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SpondonNavGraph(
                        onGoogleSignIn = { launchGoogleSignIn() },
                        onSendOtp      = { phone -> sendOtp(phone) },
                    )
                }
            }
        }
    }

    // ── Google Sign-In Setup ───────────────────────────────────

    private fun setupGoogleSignIn() {
        // default_web_client_id is auto-generated from google-services.json
        // by the google-services plugin. It requires an OAuth 2.0 Web client ID
        // to be configured in your Firebase project (see setup guide artifact).
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        .getResult(ApiException::class.java)
                    val idToken = account.idToken
                    if (idToken != null) {
                        authViewModel.signInWithGoogle(idToken)
                    } else {
                        authViewModel.setError("Google Sign-In failed: no ID token")
                    }
                } catch (e: ApiException) {
                    Log.e("GoogleSignIn", "signInResult:failed code=${e.statusCode}", e)
                    authViewModel.setError("Google Sign-In failed (${e.statusCode})")
                }
            }
        }
    }

    private fun launchGoogleSignIn() {
        // Sign out first so the account-picker is always shown
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
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