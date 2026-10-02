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
import com.hallzyx.detengue.feature.results.ResultsScreen
import com.hallzyx.detengue.feature.scan.ScanScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULTS = "results"
}

@Composable
fun DetengueApp() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val app = context.applicationContext as DetengueApplication
    val detectorHolder = app.detectorHolder
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
                onStartScan = {
                    app.inspectionStore.begin()
                    navController.navigate(Routes.SCAN)
                },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onViewResults = { navController.navigate(Routes.RESULTS) },
            )
        }
        composable(Routes.RESULTS) {
            ResultsScreen(
                // Vuelve al escáner en vivo para el reescaneo; la sesión sigue abierta.
                onRecheck = { navController.popBackStack() },
                onFinish = {
                    app.inspectionStore.finish()
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
    }
}
