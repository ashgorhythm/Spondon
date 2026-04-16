package com.spondon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spondon.app.feature.auth.*
import com.spondon.app.feature.community.*
import com.spondon.app.feature.donor.*
import com.spondon.app.feature.notification.NotificationScreen
import com.spondon.app.feature.profile.*
import com.spondon.app.feature.request.*
import com.spondon.app.feature.settings.SettingsScreen

@Composable
fun SpondonNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route
    ) {
        // ─── Auth Flow ───────────────────────────────────────
        composable(Routes.Splash.route) { SplashScreen(navController) }
        composable(Routes.Onboarding.route) { OnboardingScreen(navController) }
        composable(Routes.Login.route) { LoginScreen(navController) }
        composable(Routes.SignUp.route) { SignUpScreen(navController) }
        composable(Routes.DonorProfileSetup.route) { DonorProfileSetupScreen(navController) }
        composable(Routes.LocationSetup.route) { LocationSetupScreen(navController) }
        composable(Routes.Otp.route) { OtpScreen(navController) }
        composable(Routes.ForgotPassword.route) { ForgotPasswordScreen(navController) }

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