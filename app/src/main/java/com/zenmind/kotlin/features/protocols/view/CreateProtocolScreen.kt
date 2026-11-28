package com.zenmind.kotlin.features.protocols.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenmind.kotlin.features.protocols.viewmodel.ProtocolsViewModel
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun CreateProtocolScreen(
    viewModel: ProtocolsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var title by rememberSaveable { mutableStateOf("") }
    val steps = remember { mutableStateListOf("") }
    val isValid = title.isNotBlank() && steps.any { it.isNotBlank() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ZenCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            ZenTopBar(title = "Nuevo protocolo", onBack = onBack)
            Spacer(Modifier.height(16.dp))

            if (state.isOffline) {
                ProtocolOfflineBanner("Sin conexión. El protocolo se guardará en el teléfono y se enviará cuando vuelvas a tener internet.")
                Spacer(Modifier.height(16.dp))
            }

            Text("Dale un nombre", fontFamily = NunitoBold, fontSize = 15.sp, color = ZenLabel)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(MAX_TITLE) },
                placeholder = { Text("Ej. Crisis de ansiedad", fontFamily = Nunito) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("Pasos", fontFamily = NunitoBold, fontSize = 15.sp, color = ZenLabel)
            Spacer(Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = step,
                        onValueChange = { steps[index] = it.take(MAX_STEP) },
                        placeholder = { Text("Paso ${index + 1}", fontFamily = Nunito) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    if (steps.size > 1) {
                        IconButton(onClick = { steps.removeAt(index) }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Quitar paso", tint = ZenLabel)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            TextButton(onClick = { steps.add("") }) {
                Text("+ Agregar paso", fontFamily = NunitoBold, color = ZenText)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.createProtocol(title, steps.toList(), onDone = onBack) },
                enabled = isValid && !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZenOrange, contentColor = ZenText),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = ZenText, modifier = Modifier.height(20.dp))
                } else {
                    Text("Guardar protocolo", fontFamily = NunitoBold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        ProtocolSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

private const val MAX_TITLE = 80
private const val MAX_STEP = 300
