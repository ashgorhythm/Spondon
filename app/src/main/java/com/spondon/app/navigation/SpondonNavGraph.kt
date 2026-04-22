package com.spondon.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.spondon.app.feature.auth.*
import com.spondon.app.feature.community.*
import com.spondon.app.feature.donor.*
import com.spondon.app.feature.notification.NotificationScreen
import com.spondon.app.feature.profile.*
import com.spondon.app.feature.request.*
import com.spondon.app.feature.settings.SettingsScreen

/**
 * Root navigation graph for Spondon.
 *
 * **Critical design decision:** [authViewModel] is the single Activity-scoped
 * AuthViewModel instance created in MainActivity via `by viewModels()`.
 * It is passed in here so that every auth screen, as well as the
 * Activity-level Google Sign-In and OTP callbacks, all share the SAME instance.
 *
 * Previous bug: Using `hiltViewModel(parentEntry)` inside the nav graph created
 * a second ViewModel scoped to the "auth_flow" back-stack entry. Callbacks from
 * MainActivity updated the Activity's instance while Compose screens observed
 * the nav-graph's instance → events were lost, navigation never fired.
 */
@Composable
fun SpondonNavGraph(
    authViewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit = {},
    onSendOtp: (String) -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "auth_flow"
    ) {
        // ─── Auth Flow (nested graph) ─────────────────────────────
        // All screens share the single [authViewModel] passed from MainActivity.
        navigation(
            startDestination = Routes.Splash.route,
            route = "auth_flow",
        ) {
            composable(Routes.Splash.route) {
                SplashScreen(navController, authViewModel)
            }
            composable(Routes.Onboarding.route) {
                OnboardingScreen(navController, authViewModel)
            }
            composable(Routes.Login.route) {
                LoginScreen(
                    navController = navController,
                    onGoogleSignIn = onGoogleSignIn,
                    onSendOtp = onSendOtp,
                    viewModel = authViewModel,
                )
            }
            composable(Routes.SignUp.route) {
                SignUpScreen(
                    navController = navController,
                    onGoogleSignIn = onGoogleSignIn,
                    viewModel = authViewModel,
                )
            }
            composable(Routes.DonorProfileSetup.route) {
                DonorProfileSetupScreen(navController, authViewModel)
            }
            composable(Routes.LocationSetup.route) {
                LocationSetupScreen(navController, authViewModel)
            }
            composable(Routes.PhoneLogin.route) {
                PhoneLoginScreen(
                    navController = navController,
                    onSendOtp = onSendOtp,
                    viewModel = authViewModel,
                )
            }
            composable(Routes.Otp.route) { entry ->
                // Extract phone from route argument
                val phone = entry.arguments?.getString("phone") ?: ""
                if (phone.isNotEmpty()) {
                    authViewModel.setOtpPhone(phone)
                }
                OtpScreen(
                    navController = navController,
                    onSendOtp = onSendOtp,
                    viewModel = authViewModel,
                )
            }
            composable(Routes.ForgotPassword.route) {
                ForgotPasswordScreen(navController, authViewModel)
            }
        }

        // ─── Main ────────────────────────────────────────────
        composable(Routes.Home.route) { HomeScreen(navController) }

        // ─── Community ───────────────────────────────────────
        composable(Routes.CommunityList.route) { CommunityListScreen(navController) }
        composable(Routes.CommunityDetail.route) { CommunityDetailScreen(navController) }
        composable(Routes.CreateCommunity.route) { CreateCommunityScreen(navController) }
        composable(Routes.JoinRequest.route) { JoinRequestScreen(navController) }
        composable(Routes.AdminDashboard.route) { AdminDashboardScreen(navController) }

        // ─── Blood Request ───────────────────────────────────
        composable(Routes.CreateRequest.route) { CreateRequestScreen(navController) }
        composable(Routes.RequestDetail.route) { RequestDetailScreen(navController) }
        composable(Routes.RequestFeed.route) { RequestFeedScreen(navController) }

        // ─── Donor ───────────────────────────────────────────
        composable(Routes.FindDonor.route) { FindDonorScreen(navController) }
        composable(Routes.DonorProfile.route) { DonorProfileScreen(navController) }
        composable(Routes.DonationHistory.route) { DonationHistoryScreen(navController) }
        composable(Routes.Achievements.route) { AchievementsScreen(navController) }

        // ─── Profile ─────────────────────────────────────────
        composable(Routes.Profile.route) { ProfileScreen(navController) }
        composable(Routes.EditProfile.route) { EditProfileScreen(navController) }

        // ─── Settings & Notifications ────────────────────────
        composable(Routes.Settings.route) { SettingsScreen(navController) }
        composable(Routes.Notifications.route) { NotificationScreen(navController) }
    }
}