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
import com.hallzyx.detengue.core.network.ConnectivityObserver
import com.hallzyx.detengue.core.prefs.AppPrefs
import com.hallzyx.detengue.feature.diagnostics.DiagnosticsScreen
import com.hallzyx.detengue.feature.history.HistoryScreen
import com.hallzyx.detengue.feature.home.HomeScreen
import com.hallzyx.detengue.feature.onboarding.OnboardingScreen
import com.hallzyx.detengue.feature.privacy.PrivacyScreen
import com.hallzyx.detengue.feature.results.ResultsScreen
import com.hallzyx.detengue.feature.scan.ScanScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULTS = "results"
    const val HISTORY = "history"
    const val PRIVACY = "privacy"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun DetengueApp() {
    val context = LocalContext.current
    val app = context.applicationContext as DetengueApplication
    val prefs = remember { AppPrefs(context) }
    val navController = rememberNavController()
    val start = if (prefs.onboardingDone) Routes.HOME else Routes.ONBOARDING

    val detectorState by app.detectorHolder.state.collectAsStateWithLifecycle()
    // Valor inicial `true`: evita mostrar "Sin conexión" un instante al abrir la app.
    val online by remember { ConnectivityObserver(context) }.isOnline.collectAsStateWithLifecycle(initialValue = true)

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
                online = online,
                onStartScan = {
                    app.inspectionStore.begin()
                    navController.navigate(Routes.SCAN)
                },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                // El diagnóstico es evidencia técnica para la demo, no una pantalla de usuario final.
                onOpenDiagnostics = if (BuildConfig.DEMO_MODE) {
                    { navController.navigate(Routes.DIAGNOSTICS) }
                } else {
                    null
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
                    app.finishInspection()
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
            )
        }
        composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.PRIVACY) { PrivacyScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.DIAGNOSTICS) { DiagnosticsScreen(onBack = { navController.popBackStack() }) }
    }
}
