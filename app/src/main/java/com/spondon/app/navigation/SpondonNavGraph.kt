package com.spondon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.spondon.app.feature.auth.*
import com.spondon.app.feature.community.*
import com.spondon.app.feature.donor.*
import com.spondon.app.feature.notification.NotificationScreen
import com.spondon.app.feature.profile.*
import com.spondon.app.feature.request.*
import com.spondon.app.feature.settings.SettingsScreen

@Composable
fun SpondonNavGraph(
    onGoogleSignIn: () -> Unit = {},
    onSendOtp: (String) -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavHost(
        navController = navController,
        startDestination = "auth_flow"
    ) {
        // ─── Auth Flow (nested graph — shares a single AuthViewModel) ───
        navigation(
            startDestination = Routes.Splash.route,
            route = "auth_flow",
        ) {
            composable(Routes.Splash.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                SplashScreen(navController, sharedViewModel)
            }
            composable(Routes.Onboarding.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                OnboardingScreen(navController, sharedViewModel)
            }
            composable(Routes.Login.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                LoginScreen(
                    navController = navController,
                    onGoogleSignIn = onGoogleSignIn,
                    viewModel = sharedViewModel,
                )
            }
            composable(Routes.SignUp.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                SignUpScreen(
                    navController = navController,
                    onGoogleSignIn = onGoogleSignIn,
                    viewModel = sharedViewModel,
                )
            }
            composable(Routes.DonorProfileSetup.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                DonorProfileSetupScreen(navController, sharedViewModel)
            }
            composable(Routes.LocationSetup.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                LocationSetupScreen(navController, sharedViewModel)
            }
            composable(Routes.Otp.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                OtpScreen(
                    navController = navController,
                    onSendOtp = onSendOtp,
                    viewModel = sharedViewModel,
                )
            }
            composable(Routes.ForgotPassword.route) { entry ->
                val parentEntry = navController.getBackStackEntry("auth_flow")
                val sharedViewModel: AuthViewModel = hiltViewModel(parentEntry)
                ForgotPasswordScreen(navController, sharedViewModel)
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