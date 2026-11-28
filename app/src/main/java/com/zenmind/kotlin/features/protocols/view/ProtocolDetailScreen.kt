package com.zenmind.kotlin.features.protocols.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSubtle
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun ProtocolDetailScreen(
    viewModel: ProtocolsViewModel,
    protocolId: Long,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(protocolId) {
        viewModel.openProtocol(protocolId)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(ZenCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ZenTopBar(title = "Protocolo", onBack = onBack)
        Spacer(Modifier.height(16.dp))

        val protocol = state.selected
        when {
            state.isLoadingDetail -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZenOrange)
            }
            protocol == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontró este protocolo en el teléfono.", fontFamily = Nunito, color = ZenLabel)
            }
            else -> {
                Text(
                    protocol.titulo,
                    fontFamily = NunitoBold,
                    fontSize = 22.sp,
                    color = ZenText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    if (protocol.synced) "${protocol.pasos.size} pasos" else "${protocol.pasos.size} pasos · Pendiente de enviar",
                    fontFamily = Nunito,
                    fontSize = 14.sp,
                    color = ZenSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(protocol.pasos.sortedBy { it.ordenPaso }) { step ->
                        StepRow(step.ordenPaso, step.descripcion)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(order: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ZenPeach, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .background(ZenOrange, CircleShape)
        ) {
            Text("$order", fontFamily = NunitoBold, color = ZenText)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, fontFamily = Nunito, fontSize = 15.sp, color = ZenText)
    }
}
