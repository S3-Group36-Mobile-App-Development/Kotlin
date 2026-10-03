package com.zenmind.kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.authentication.data.repositories.AuthRepositoryImpl
import com.zenmind.kotlin.features.authentication.data.services.AuthServiceProvider
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

                val repository = remember {
                    AuthRepositoryImpl(
                        authApiService = AuthServiceProvider.service
                    )
                }

                val factory = remember {
                    AuthViewModelFactory(
                        authRepository = repository
                    )
                }

                val authViewModel: AuthViewModel = viewModel(
                    factory = factory
                )

                var screen by remember {
                    mutableStateOf("login")
                }

                when (screen) {

                    "login" -> {
                        LoginScreen(
                            viewModel = authViewModel,
                            onBack = {
                                screen = "register"
                            },
                            onLoginSuccess = {
                                screen = "success"
                            }
                        )
                    }

                    "register" -> {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onBack = {
                                screen = "login"
                            },
                            onRegisterSuccess = {
                                screen = "success"
                            }
                        )
                    }

                    "success" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Autenticación exitosa")
                        }
                    }
                }
            }
        }
    }
}