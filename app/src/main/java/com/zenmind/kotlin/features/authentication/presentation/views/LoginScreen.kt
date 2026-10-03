package com.zenmind.kotlin.features.authentication.presentation.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthUiState
import com.zenmind.kotlin.features.authentication.presentation.viewmodels.AuthViewModel

private val ZenMindBackground = Color(0xFFFFF9E7)
private val ZenMindBrown = Color(0xFF4A3B2A)
private val ZenMindGreen = Color(0xFFD0D8AF)
private val ZenMindBorder = Color(0xFFB9AF94)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onGoogleClick: (() -> Unit)? = null
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {

            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetState()
            }

            is AuthUiState.Success -> {
                onLoginSuccess()
                viewModel.resetState()
            }

            else -> Unit
        }
    }

    val isLoading = uiState is AuthUiState.Loading

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        containerColor = ZenMindBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ZenMindBackground)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                IconButton(
                    onClick = onBack
                ) {
                    Text(
                        text = "←",
                        color = ZenMindBrown,
                        fontSize = 22.sp
                    )
                }

                Text(
                    text = "Volver",
                    color = ZenMindBrown,
                    fontSize = 14.sp
                )
            }

            Spacer(
                modifier = Modifier.height(42.dp)
            )

            Text(
                text = "B I E N V E N I D O",
                color = ZenMindBrown,
                fontSize = 18.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Inicia sesión para continuar.",
                color = ZenMindBrown,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(
                modifier = Modifier.height(36.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Correo electrónico")
                },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenMindBrown,
                    unfocusedBorderColor = ZenMindBorder,
                    focusedLabelColor = ZenMindBrown,
                    cursorColor = ZenMindBrown
                )
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Contraseña")
                },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Text(
                            text = "👁",
                            fontSize = 16.sp
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (!isLoading) {
                            viewModel.login(
                                email = email,
                                password = password
                            )
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenMindBrown,
                    unfocusedBorderColor = ZenMindBorder,
                    focusedLabelColor = ZenMindBrown,
                    cursorColor = ZenMindBrown
                )
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = {
                    viewModel.login(
                        email = email,
                        password = password
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZenMindGreen,
                    contentColor = ZenMindBrown
                )
            ) {
                Text(
                    text =
                        if (isLoading) {
                            "Iniciando..."
                        } else {
                            "Iniciar sesión"
                        }
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ZenMindBorder
                )

                Text(
                    text = "o",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = ZenMindBrown
                )

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = ZenMindBorder
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedButton(
                onClick = {
                    onGoogleClick?.invoke()
                },
                enabled = onGoogleClick != null && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    text = "Continuar con Google"
                )
            }
        }
    }
}