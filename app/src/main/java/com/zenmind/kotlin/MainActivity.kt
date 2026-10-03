package com.zenmind.kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepositoryImpl
import com.zenmind.kotlin.features.authentication.data.services.AuthServiceProvider
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthUiState
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModel
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModelFactory
import com.zenmind.kotlin.features.authentication.presentation.views.LoginScreen
import com.zenmind.kotlin.features.authentication.presentation.views.RegisterScreen
import com.zenmind.kotlin.ui.theme.ZenmindKotlinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            ZenmindKotlinTheme {

                /*
                 * Storage encargado de guardar los tokens
                 * de forma cifrada.
                 */
                val tokenStorage = remember {
                    TokenStorage(applicationContext)
                }

                /*
                 * Repository encargado de comunicarse con
                 * el backend y manejar los tokens.
                 */
                val repository = remember {
                    AuthRepositoryImpl(
                        authApiService = AuthServiceProvider.service,
                        tokenStorage = tokenStorage
                    )
                }

                /*
                 * Factory necesaria porque AuthViewModel
                 * recibe un AuthRepository en el constructor.
                 */
                val factory = remember {
                    AuthViewModelFactory(
                        authRepository = repository
                    )
                }

                /*
                 * ViewModel compartido por Login y Register.
                 */
                val authViewModel: AuthViewModel = viewModel(
                    factory = factory
                )

                /*
                 * Observamos el estado de autenticación.
                 */
                val authState by authViewModel.uiState.collectAsState()

                /*
                 * Navegación temporal entre Login y Register.
                 *
                 * Más adelante reemplazaremos esto por
                 * navegación real.
                 */
                var screen by remember {
                    mutableStateOf("login")
                }

                /*
                 * Se ejecuta una sola vez cuando inicia la app.
                 *
                 * Comprueba si existe una sesión guardada.
                 */
                LaunchedEffect(Unit) {
                    authViewModel.restoreSession()
                }

                /*
                 * Primero decidimos qué mostrar dependiendo
                 * del estado global de autenticación.
                 */
                when (val state = authState) {

                    /*
                     * La aplicación está comprobando si
                     * existe una sesión anterior.
                     */
                    is AuthUiState.CheckingSession -> {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    /*
                     * Usuario autenticado.
                     */
                    is AuthUiState.Success -> {

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = "Hola, ${state.user.nombreVisible}"
                            )

                            Spacer(
                                modifier = Modifier.height(24.dp)
                            )

                            Button(
                                onClick = {

                                    /*
                                     * Dejamos preparada la pantalla
                                     * que aparecerá después del logout.
                                     */
                                    screen = "login"

                                    /*
                                     * ViewModel -> Repository
                                     * -> TokenStorage -> elimina tokens.
                                     */
                                    authViewModel.logout()
                                }
                            ) {
                                Text(
                                    text = "Cerrar sesión"
                                )
                            }
                        }
                    }

                    /*
                     * Idle, Error o Loading de Login/Register.
                     *
                     * LoginScreen y RegisterScreen ya manejan
                     * Loading/Error internamente observando
                     * el mismo ViewModel.
                     */
                    else -> {

                        when (screen) {

                            "login" -> {

                                LoginScreen(
                                    viewModel = authViewModel,

                                    /*
                                     * Temporalmente usamos "Volver"
                                     * para llegar a Register.
                                     */
                                    onBack = {
                                        screen = "register"
                                    },

                                    /*
                                     * No necesitamos cambiar pantalla.
                                     *
                                     * Cuando el login funciona,
                                     * AuthUiState pasa a Success y
                                     * este MainActivity reacciona
                                     * automáticamente.
                                     */
                                    onLoginSuccess = {
                                        // AuthUiState.Success controla esto.
                                    }
                                )
                            }

                            "register" -> {

                                RegisterScreen(
                                    viewModel = authViewModel,

                                    onBack = {
                                        screen = "login"
                                    },

                                    /*
                                     * Igual que en Login:
                                     * AuthUiState.Success se encarga.
                                     */
                                    onRegisterSuccess = {
                                        // AuthUiState.Success controla esto.
                                    }
                                )
                            }

                            else -> {

                                /*
                                 * Protección por si screen llega
                                 * a tener un valor inesperado.
                                 */
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onBack = {
                                        screen = "register"
                                    },
                                    onLoginSuccess = {
                                        // AuthUiState.Success controla esto.
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}