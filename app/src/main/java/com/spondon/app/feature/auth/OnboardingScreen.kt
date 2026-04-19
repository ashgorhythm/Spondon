package com.spondon.app.feature.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.spondon.app.core.ui.components.BloodDropLoader
import com.spondon.app.core.ui.theme.*
import com.spondon.app.navigation.Routes
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val titleBn: String,
    val description: String,
    val accentColor: Color,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    viewModel: AuthViewModel,
) {
    val pages = listOf(
        OnboardingPage(
            title = "Give Blood, Save Lives",
            titleBn = "রক্ত দিন, জীবন বাঁচান",
            description = "Join a community of heroes. Your single donation can save up to three lives. Every drop counts.",
            accentColor = BloodRed,
        ),
        OnboardingPage(
            title = "Find Your Community",
            titleBn = "আপনার কমিউনিটি খুঁজুন",
            description = "Connect with local blood donor communities. Get matched with nearby donors and requests in your area.",
            accentColor = SoftRose,
        ),
        OnboardingPage(
            title = "Donate with Trust",
            titleBn = "বিশ্বাসের সাথে দান করুন",
            description = "Verified communities, transparent processes, and safe donation practices. Your safety and privacy are our priority.",
            accentColor = AvailableGreen,
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Skip button
        TextButton(
            onClick = {
                viewModel.completeOnboarding()
                navController.navigate(Routes.Login.route) {
                    popUpTo(Routes.Onboarding.route) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
        ) {
            Text(
                text = "Skip",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Dot indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp),
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) pages[index].accentColor
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            val isLastPage = pagerState.currentPage == pages.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        viewModel.completeOnboarding()
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accentColor,
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AnimatedContent(
                        targetState = isLastPage,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "button_text",
                    ) { last ->
                        Text(
                            text = if (last) "Get Started" else "Next",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Animated icon area
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            page.accentColor.copy(alpha = 0.15f),
                            page.accentColor.copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            BloodDropLoader(
                size = 100.dp,
                color = page.accentColor,
                accentColor = page.accentColor.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Bangla title
        Text(
            text = page.titleBn,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
            ),
            color = page.accentColor,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // English title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Description
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}