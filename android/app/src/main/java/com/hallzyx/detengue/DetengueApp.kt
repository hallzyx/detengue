package com.hallzyx.detengue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hallzyx.detengue.core.inference.DetectorState
import com.hallzyx.detengue.core.prefs.AppPrefs
import com.hallzyx.detengue.feature.home.HomeScreen
import com.hallzyx.detengue.feature.onboarding.OnboardingScreen
import com.hallzyx.detengue.feature.scan.ScanScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCAN = "scan"
}

@Composable
fun DetengueApp() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val detectorHolder = (context.applicationContext as DetengueApplication).detectorHolder
    val detectorState by detectorHolder.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val start = if (prefs.onboardingDone) Routes.HOME else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    prefs.onboardingDone = true
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                aiReady = detectorState is DetectorState.Ready,
                onStartScan = { navController.navigate(Routes.SCAN) },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(onBack = { navController.popBackStack() })
        }
    }
}
