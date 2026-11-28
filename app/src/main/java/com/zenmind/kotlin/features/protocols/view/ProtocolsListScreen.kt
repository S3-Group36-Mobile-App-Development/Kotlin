package com.zenmind.kotlin.features.protocols.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenmind.kotlin.features.protocols.model.Protocol
import com.zenmind.kotlin.features.protocols.viewmodel.ProtocolsViewModel
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenIcon
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSubtle
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun ProtocolsListScreen(
    viewModel: ProtocolsViewModel,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            ZenTopBar(title = "Protocolos", onBack = onBack)
            Spacer(Modifier.height(12.dp))
            Text(
                "Rutinas para momentos difíciles.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = ZenSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            if (state.isOffline) {
                ProtocolOfflineBanner("Sin conexión. Estás viendo los protocolos guardados en el teléfono.")
                Spacer(Modifier.height(12.dp))
            }

            when {
                state.isGuest -> CenteredText("Inicia sesión para ver los protocolos y crear los tuyos.")
                state.isLoading && state.protocols.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZenOrange)
                }
                state.protocols.isEmpty() && state.isOffline -> CenteredText("Sin conexión y sin protocolos guardados. Conéctate una vez para descargarlos.")
                state.protocols.isEmpty() -> CenteredText("Aún no tienes protocolos. Crea el primero con el botón +.")
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(state.protocols, key = { it.id }) { protocol ->
                        ProtocolCard(protocol, onClick = { onOpen(protocol.id) })
                    }
                }
            }
        }

        if (!state.isGuest) {
            FloatingActionButton(
                onClick = onCreate,
                shape = RoundedCornerShape(16.dp),
                containerColor = ZenOrange,
                contentColor = ZenIcon,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Crear protocolo")
            }
        }

        ProtocolSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }
}

@Composable
private fun ProtocolCard(protocol: Protocol, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZenPeach, shape)
            .border(1.dp, ZenOrange, shape)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Text(protocol.titulo, fontFamily = NunitoBold, fontSize = 16.sp, color = ZenText)
        Spacer(Modifier.height(4.dp))
        val detail = when {
            !protocol.synced -> "${protocol.pasos.size} pasos · Pendiente de enviar"
            protocol.esPredefinido -> "${protocol.pasos.size} pasos · Predefinido"
            else -> "${protocol.pasos.size} pasos · Propio"
        }
        Text(detail, fontFamily = Nunito, fontSize = 13.sp, color = ZenSubtle)
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, fontFamily = Nunito, fontSize = 15.sp, color = ZenLabel, textAlign = TextAlign.Center)
    }
}
