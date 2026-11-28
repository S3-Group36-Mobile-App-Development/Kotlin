package com.zenmind.kotlin.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.zenmind.kotlin.R
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zenmind.kotlin.ZenMindApplication
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.core.network.ConnectivityObserver
import com.zenmind.kotlin.core.network.ApiClient
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
import com.zenmind.kotlin.features.breathing.view.BreathingCatalogScreen
import com.zenmind.kotlin.features.panicbutton.view.SupportNetworkScreen
import com.zenmind.kotlin.features.panicbutton.view.AddContactScreen
import com.zenmind.kotlin.features.flashcards.view.FlashcardsScreen
import com.zenmind.kotlin.features.flashcards.view.FlashcardDetailScreen
import com.zenmind.kotlin.features.dailycheckin.view.DailyCheckInScreen
import com.zenmind.kotlin.features.home.model.HomeFeature
import com.zenmind.kotlin.features.home.view.HomeScreen
import com.zenmind.kotlin.features.games.view.BubbleGameScreen
import com.zenmind.kotlin.features.panic.view.PanicScreen
import com.zenmind.kotlin.features.panic.viewmodel.PanicViewModel
import com.zenmind.kotlin.features.protocols.view.CreateProtocolScreen
import com.zenmind.kotlin.features.protocols.view.ProtocolDetailScreen
import com.zenmind.kotlin.features.protocols.view.ProtocolsListScreen
import com.zenmind.kotlin.features.protocols.viewmodel.ProtocolsViewModel
import com.zenmind.kotlin.features.profile.view.ProfileScreen
import com.zenmind.kotlin.features.profile.view.EditProfileScreen
import com.zenmind.kotlin.features.profile.viewmodel.ProfileViewModel
import com.zenmind.kotlin.features.games.data.GameSyncScheduler
import com.zenmind.kotlin.features.panicbutton.viewmodel.ContactViewModel
import com.zenmind.kotlin.features.panicbutton.viewmodel.ContactViewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenOrange

object Routes {

    const val AUTH_GATE = "auth_gate"
    const val WELCOME = "welcome"

    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile_edit"
    const val DAILY_CHECKIN = "daily_checkin"
    const val SUPPORT = "support"
    const val BREATHING = "breathing"
    const val BREATHING_CATALOG = "breathing_catalog"
    const val GAMES = "games"
    const val PANIC = "panic"
    const val PANIC_ADD = "panic_add"
    const val PANIC_BUTTON = "panic_button"
    const val PROTOCOLS = "protocols"
    const val PROTOCOL_CREATE = "protocol_create"
    const val PROTOCOL_DETAIL = "protocol/{id}"

    fun protocolDetail(id: Long) = "protocol/$id"
    const val FLASHCARDS = "flashcards"
    const val FLASHCARDS_DETAIL = "flashcards_detail?temaId={temaId}"

    fun breathingSession(exerciseId: Int = -1) = "breathing?exerciseId=$exerciseId"
    fun flashcardsDetail(temaId: String) = "flashcards_detail?temaId=$temaId"
}

@Composable
fun ZenNavGraph() {

    val navController = rememberNavController()

    val context = LocalContext.current
    val app = context.applicationContext as ZenMindApplication
    val scheduleGamesAfterLogin: () -> Unit = {
        app.applicationScope.launch {
            try { GameSyncScheduler.enqueuePending(app, reauthenticated = true) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { }
        }
    }
    val googleSignInService = remember { GoogleSignInService() }

    val tokenStorage = remember {
        TokenStorage(
            context = context.applicationContext
        )
    }

    fun authorizeVerifiedAccount(userId: String) {
        if (tokenStorage.getSessionUserId() == userId && app.container.connectivityObserver.isOnline()) {
            ApiClient.setVerifiedAccessToken(tokenStorage.getAccessToken())
        }
    }

    val authRepository = remember {
        AuthRepositoryImpl(
            authApiService = AuthServiceProvider.service,
            tokenStorage = tokenStorage
        )
    }

    val authViewModelFactory = remember {
        AuthViewModelFactory(
            authRepository = authRepository,
            isOnline = ConnectivityObserver(context.applicationContext)::isOnline,
            onAuthenticated = { user ->
                if (app.container.connectivityObserver.isOnline()) {
                    authorizeVerifiedAccount(user.id)
                    app.container.onVerifiedSession(user.id)
                    try {
                        app.container.profileRepository.refreshProfile(user.id)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) { }
                }
            },
            findOfflineUserId = app.container.profileRepository::offlineSessionUserId,
            onSessionUnverified = {
                ApiClient.setVerifiedAccessToken(null)
                app.container.profileRepository.markUnverified()
            }
        )
    }

    val authViewModel: AuthViewModel = viewModel(
        factory = authViewModelFactory
    )

    val authState by authViewModel.uiState.collectAsState()
    val activeUserId by authViewModel.activeUserId.collectAsState()
    val offlineSession by authViewModel.offlineSession.collectAsState()
    val connectionMessages = remember { SnackbarHostState() }

    val telemetry = (context.applicationContext as ZenMindApplication).container.telemetryRepository
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            entry.destination.route?.let { telemetry.screenView(it) }
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.restoreSession()
    }

    LaunchedEffect(Unit) {
        var previous: Boolean? = null
        app.container.connectivityObserver.onlineFlow.collect { online ->
            authViewModel.onConnectivityChanged(online)
            if (previous != null && previous != online) {
                launch {
                    connectionMessages.showSnackbar(
                        if (online) "Conexión restablecida" else "Sin conexión a internet"
                    )
                }
            }
            previous = online
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.SessionInvalidated) {
            navController.navigate(Routes.WELCOME) {
                popUpTo(Routes.HOME) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = Routes.AUTH_GATE
    ) {

        composable(Routes.AUTH_GATE) {

            when (val state = authState) {

                is AuthUiState.CheckingSession -> {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ZenCream),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.zenmind_logo),
                                contentDescription = "ZenMind Logo",
                                modifier = Modifier.width(180.dp)
                            )
                            Spacer(Modifier.height(24.dp))
                            CircularProgressIndicator(
                                color = ZenOrange
                            )
                        }
                    }
                }

                is AuthUiState.Success -> {

                    LaunchedEffect(authState) {
                        scheduleGamesAfterLogin()

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

                is AuthUiState.OfflineSession -> {
                    LaunchedEffect(state.userId) {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.AUTH_GATE) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                is AuthUiState.SessionUnavailable -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ZenCream)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Text(state.message)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = authViewModel::restoreSession) {
                            Text("Reintentar")
                        }
                        TextButton(onClick = {
                            navController.navigate(Routes.WELCOME) {
                                popUpTo(Routes.AUTH_GATE) { inclusive = true }
                                launchSingleTop = true
                            }
                        }) {
                            Text("Ir a bienvenida")
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

        composable(Routes.BREATHING_CATALOG) { entry ->
            BreathingCatalogScreen(
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onSelect = { exercise ->
                    if (entry.isResumed()) {
                        navController.navigate(Routes.breathingSession(exercise.id)) {
                            popUpTo(Routes.BREATHING_CATALOG) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.PANIC) { entry ->
            SupportNetworkScreen(
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onAddContact = { if (entry.isResumed()) navController.navigate(Routes.PANIC_ADD) }
            )
        }

        composable(Routes.PANIC_ADD) { entry ->
            val context = LocalContext.current
            val viewModel: ContactViewModel = viewModel(
                factory = ContactViewModelFactory(context)
            )
            AddContactScreen(
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onSave = { contact ->
                    if (entry.isResumed()) {
                        viewModel.save(contact)
                        navController.popBackStack()
                    }
                })
        }

        composable(Routes.FLASHCARDS) { entry ->
            FlashcardsScreen(
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onCardClick = { temaId ->
                    if (entry.isResumed()) navController.navigate(Routes.flashcardsDetail(temaId))
                }
            )
        }

        composable(
            route = "flashcards_detail?temaId={temaId}",
            arguments = listOf(
                androidx.navigation.navArgument("temaId") {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = "1"
                }
            )
        ) { backStackEntry ->
            val temaId = backStackEntry.arguments?.getString("temaId") ?: "1"
            FlashcardDetailScreen(
                temaId = temaId,
                onBack = { if (backStackEntry.isResumed()) navController.popBackStack() }
            )
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

                    authViewModel.logout()

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
                    val user = (authViewModel.uiState.value as? AuthUiState.Success)?.user
                    user?.let { authorizeVerifiedAccount(it.id) }
                    scheduleGamesAfterLogin()

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
                onLoginClick = { navController.navigate(Routes.LOGIN) },

                onBack = {
                    navController.popBackStack()
                },

                onRegisterSuccess = {
                    val user = (authViewModel.uiState.value as? AuthUiState.Success)?.user
                    user?.let { authorizeVerifiedAccount(it.id) }
                    scheduleGamesAfterLogin()

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

        composable(Routes.HOME) { entry ->

            HomeScreen(
                offlineSession = offlineSession,

                onProfileClick = {
                    if (entry.isResumed()) navController.navigate(Routes.PROFILE)
                },

                onContinueClick = {
                    if (entry.isResumed()) navController.navigate(Routes.DAILY_CHECKIN)
                },

                onFeatureClick = { feature ->
                    if (entry.isResumed()) {
                        when (feature) {
                            HomeFeature.BREATHING -> navController.navigate(Routes.BREATHING)
                            HomeFeature.FLASHCARDS -> navController.navigate(Routes.FLASHCARDS)
                            HomeFeature.GAMES -> navController.navigate(Routes.GAMES)
                            HomeFeature.PROTOCOLS -> navController.navigate(Routes.PROTOCOLS)
                            HomeFeature.SUPPORT -> navController.navigate(Routes.PANIC)
                            HomeFeature.PANIC -> navController.navigate(Routes.PANIC_BUTTON)
                        }
                    }
                }
            )
        }

        composable(Routes.PROFILE) { entry ->
            val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
            ProfileScreen(
                userId = activeUserId,
                offlineSession = offlineSession,
                viewModel = profileViewModel,
                onBack = {
                    if (entry.isResumed()) navController.popBackStack()
                },
                onLogout = {
                    if (entry.isResumed()) {
                        authViewModel.logout()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onEdit = { if (entry.isResumed()) navController.navigate(Routes.PROFILE_EDIT) },
                onVerify = authViewModel::verifySessionAgain,
                onLogin = {
                    if (entry.isResumed()) {
                        authViewModel.logout()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                        navController.navigate(Routes.LOGIN)
                    }
                }

            )
        }

        composable(Routes.PROFILE_EDIT) { entry ->
            val parent = remember(entry) { navController.getBackStackEntry(Routes.PROFILE) }
            val profileViewModel: ProfileViewModel = viewModel(parent, factory = ProfileViewModel.Factory)
            EditProfileScreen(
                userId = activeUserId,
                viewModel = profileViewModel,
                onBack = { if (entry.isResumed()) navController.popBackStack() }
            )
        }

        composable(Routes.DAILY_CHECKIN) { entry ->

            DailyCheckInScreen(
                onBack = {
                    if (entry.isResumed()) navController.popBackStack()
                },
                onGoToBreathing = {
                    if (entry.isResumed()) navController.navigate(Routes.BREATHING)
                }
            )
        }

        composable(
            route = "breathing?exerciseId={exerciseId}",
            arguments = listOf(
                androidx.navigation.navArgument("exerciseId") {
                    type = androidx.navigation.NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getInt("exerciseId") ?: -1
            BreathingSessionScreen(
                exerciseId = exerciseId,
                onBack = { if (backStackEntry.isResumed()) navController.popBackStack() },
                onOpenCatalog = { if (backStackEntry.isResumed()) navController.navigate(Routes.BREATHING_CATALOG) }
            )
        }

        composable(Routes.GAMES) { entry ->
            BubbleGameScreen(onBack = { if (entry.isResumed()) navController.popBackStack() })
        }

        composable(Routes.PANIC_BUTTON) { entry ->
            val panicViewModel: PanicViewModel = viewModel(factory = PanicViewModel.Factory)
            PanicScreen(
                viewModel = panicViewModel,
                onBack = { if (entry.isResumed()) navController.popBackStack() }
            )
        }

        composable(Routes.PROTOCOLS) { entry ->
            val protocolsViewModel: ProtocolsViewModel = viewModel(factory = ProtocolsViewModel.Factory)
            ProtocolsListScreen(
                viewModel = protocolsViewModel,
                onBack = { if (entry.isResumed()) navController.popBackStack() },
                onOpen = { id -> if (entry.isResumed()) navController.navigate(Routes.protocolDetail(id)) },
                onCreate = { if (entry.isResumed()) navController.navigate(Routes.PROTOCOL_CREATE) }
            )
        }

        composable(
            route = Routes.PROTOCOL_DETAIL,
            arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.LongType })
        ) { entry ->
            val parent = remember(entry) { navController.getBackStackEntry(Routes.PROTOCOLS) }
            val protocolsViewModel: ProtocolsViewModel = viewModel(parent, factory = ProtocolsViewModel.Factory)
            ProtocolDetailScreen(
                viewModel = protocolsViewModel,
                protocolId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { if (entry.isResumed()) navController.popBackStack() }
            )
        }

        composable(Routes.PROTOCOL_CREATE) { entry ->
            val parent = remember(entry) { navController.getBackStackEntry(Routes.PROTOCOLS) }
            val protocolsViewModel: ProtocolsViewModel = viewModel(parent, factory = ProtocolsViewModel.Factory)
            CreateProtocolScreen(
                viewModel = protocolsViewModel,
                onBack = { if (entry.isResumed()) navController.popBackStack() }
            )
        }

    }
    SnackbarHost(connectionMessages, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

private fun NavBackStackEntry.isResumed(): Boolean =
    lifecycle.currentState == Lifecycle.State.RESUMED
