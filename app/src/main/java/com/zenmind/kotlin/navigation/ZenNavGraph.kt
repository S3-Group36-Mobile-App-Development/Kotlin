package com.zenmind.kotlin.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zenmind.kotlin.features.home.model.HomeFeature
import com.zenmind.kotlin.features.home.view.HomeScreen
import com.zenmind.kotlin.features.home.viewmodel.HomeViewModel
import com.zenmind.kotlin.features.breathing.view.BreathingSessionScreen
// import com.zenmind.kotlin.features.supportnetwork.view.SupportNetworkScreen
import com.zenmind.kotlin.ui.components.ZenTab

// Rutas de la app
object Routes {
    const val HOME = "home"
    const val SUPPORT = "support"
    const val BREATHING = "breathing"
}

// Navegación de la app con Navigation Compose
@Composable
fun ZenNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            HomeScreen(
                viewModel = homeViewModel,
                onFeatureClick = { feature ->
                    when (feature) {
                        HomeFeature.SUPPORT -> {}//navController.navigate(Routes.SUPPORT)
                        HomeFeature.PANIC -> {}
                        HomeFeature.BREATHING -> navController.navigate(Routes.BREATHING)
                        HomeFeature.PROTOCOLS -> {}
                        HomeFeature.FLASHCARDS -> {}
                        HomeFeature.GAMES -> {}
                    }
                }
            )
        }
        composable(Routes.BREATHING) {
            BreathingSessionScreen()
        }

        // composable(Routes.SUPPORT) {
        //     SupportNetworkScreen(
        //         onBack = { navController.popBackStack() },
        //         onTabClick = { tab ->
        //             if (tab == ZenTab.HOME) {
        //                 navController.popBackStack()
        //             }
        //         }
        //     )
        // }
    }
}
