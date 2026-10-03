package com.zenmind.kotlin.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepositoryImpl
import com.zenmind.kotlin.features.authentication.data.services.AuthServiceProvider
import com.zenmind.kotlin.features.authentication.data.services.GoogleSignInService
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthUiState
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModel
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModelFactory
import com.zenmind.kotlin.features.authentication.presentation.views.LoginScreen
import com.zenmind.kotlin.features.authentication.presentation.views.RegisterScreen
import com.zenmind.kotlin.features.authentication.presentation.views.WelcomeScreen
import com.zenmind.kotlin.features.breathing.view.BreathingSessionScreen
import com.zenmind.kotlin.features.dailycheckin.view.DailyCheckInScreen
import com.zenmind.kotlin.features.home.model.HomeFeature
import com.zenmind.kotlin.features.home.view.HomeScreen
import com.zenmind.kotlin.features.home.viewmodel.HomeViewModel
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenOrange

object Routes {

    const val AUTH_GATE = "auth_gate"
    const val WELCOME = "welcome"

    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val DAILY_CHECKIN = "daily_checkin"
    const val SUPPORT = "support"
    const val BREATHING = "breathing"
}

@Composable
fun ZenNavGraph() {

    val navController = rememberNavController()

    val context = LocalContext.current
    val googleSignInService = remember { GoogleSignInService() }

    val tokenStorage = remember {
        TokenStorage(
            context = context.applicationContext
        )
    }

    val authRepository = remember {
        AuthRepositoryImpl(
            authApiService = AuthServiceProvider.service,
            tokenStorage = tokenStorage
        )
    }

    val authViewModelFactory = remember {
        AuthViewModelFactory(
            authRepository = authRepository
        )
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = authViewModelFactory
    )

    val authState by authViewModel.uiState.collectAsState()

    val telemetry = (context.applicationContext as ZenMindApplication).container.telemetryRepository
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            entry.destination.route?.let { telemetry.screenView(it) }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.restoreSession()
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AUTH_GATE
    ) {

        composable(Routes.AUTH_GATE) {

            when (authState) {

                is AuthUiState.CheckingSession -> {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ZenCream),
                        contentAlignment = Alignment.Center
                    ) {

                        CircularProgressIndicator(
                            color = ZenOrange
                        )
                    }
                }

                is AuthUiState.Success -> {

                    LaunchedEffect(authState) {

                        navController.navigate(
                            Routes.HOME
                        ) {

                            popUpTo(
                                Routes.AUTH_GATE
                            ) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    }
                }

                else -> {

                    LaunchedEffect(authState) {

                        navController.navigate(
                            Routes.WELCOME
                        ) {

                            popUpTo(
                                Routes.AUTH_GATE
                            ) {
                                inclusive = true
                            }

                            launchSingleTop = true
                        }
                    }
                }
            }
        }

        composable(Routes.WELCOME) {

            WelcomeScreen(

                onLoginClick = {

                    navController.navigate(
                        Routes.LOGIN
                    )
                },

                onRegisterClick = {

                    navController.navigate(
                        Routes.REGISTER
                    )
                },

                onGuestClick = {

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(
                            Routes.WELCOME
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.LOGIN) {

            LoginScreen(
                viewModel = authViewModel,

                onBack = {
                    navController.popBackStack()
                },

                onLoginSuccess = {

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(
                            Routes.WELCOME
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },
                onGoogleClick = {
                    authViewModel.loginWithGoogle {
                        googleSignInService.getIdToken(context)
                    }
                }
            )
        }

        composable(Routes.REGISTER) {

            RegisterScreen(
                viewModel = authViewModel,

                onBack = {
                    navController.popBackStack()
                },

                onRegisterSuccess = {

                    navController.navigate(
                        Routes.HOME
                    ) {

                        popUpTo(
                            Routes.WELCOME
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },
                onGoogleClick = {
                    authViewModel.loginWithGoogle {
                        googleSignInService.getIdToken(context)
                    }
                }
            )
        }

        composable(Routes.HOME) {

            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory
            )

            HomeScreen(
                viewModel = homeViewModel,

                onLogoutClick = {

                    authViewModel.logout()

                    navController.navigate(
                        Routes.WELCOME
                    ) {

                        popUpTo(
                            Routes.HOME
                        ) {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                },

                onContinueClick = {

                    navController.navigate(Routes.DAILY_CHECKIN)
                },

                onFeatureClick = { feature ->

                    when (feature) {

                        HomeFeature.SUPPORT -> {

                        }

                        HomeFeature.PANIC -> {

                        }

                        HomeFeature.BREATHING -> {

                            navController.navigate(
                                Routes.BREATHING
                            )
                        }

                        HomeFeature.PROTOCOLS -> {

                        }

                        HomeFeature.FLASHCARDS -> {

                        }

                        HomeFeature.GAMES -> {

                        }
                    }
                }
            )
        }

        composable(Routes.DAILY_CHECKIN) {

            DailyCheckInScreen(
                onBack = {
                    navController.popBackStack()
                },
                onGoToBreathing = {
                    navController.navigate(Routes.BREATHING)
                }
            )
        }

        composable(Routes.BREATHING) {

            BreathingSessionScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

    }
}