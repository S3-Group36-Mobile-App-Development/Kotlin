package com.zenmind.kotlin.features.games.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.games.model.GamePhase
import com.zenmind.kotlin.features.games.viewmodel.BubbleGameViewModel
import com.zenmind.kotlin.features.games.viewmodel.SaveStatus
import com.zenmind.kotlin.features.games.data.GameSyncStatus
import com.zenmind.kotlin.features.games.model.GameStatistics
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.Livvic
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenFlashcard
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSage
import com.zenmind.kotlin.ui.theme.ZenSageLight
import com.zenmind.kotlin.ui.theme.ZenText
import kotlinx.coroutines.delay

@Composable
fun BubbleGameScreen(
    onBack: () -> Unit,
    viewModel: BubbleGameViewModel = viewModel(factory = BubbleGameViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.pause() }
    LaunchedEffect(state.phase, lifecycleOwner) {
        if (state.phase == GamePhase.Running) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (viewModel.uiState.value.phase == GamePhase.Running) {
                    delay(250)
                    viewModel.tick()
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(ZenCream).safeDrawingPadding()) {
        // Insets are applied before measuring the playable height. On shorter
        // screens the field keeps its minimum size and the content can scroll.
        val boardHeight = (maxHeight - if (state.phase == GamePhase.Running || state.phase == GamePhase.Paused) 185.dp else 80.dp)
            .coerceAtLeast(320.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.padding(horizontal = 24.dp)) {
                ZenTopBar(title = "Juegos", onBack = onBack)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.phase == GamePhase.Running || state.phase == GamePhase.Paused) {
                    Spacer(Modifier.height(16.dp))
                    GameTitle()
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Siguiente: ${state.nextNumber}   ·   Aciertos: ${state.correctTaps}/20   ·   Errores: ${state.errors}",
                        fontFamily = NunitoBold,
                        color = ZenText,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Text("Tiempo: ${formatTime(state.displayMs)}", fontFamily = Livvic, color = ZenText)
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(boardHeight)
                        .background(ZenCream)
                ) {
                    val width = maxWidth.value
                    val height = maxHeight.value
                    LaunchedEffect(width, height) { viewModel.resize(width, height) }

                    val palette = listOf(ZenOrange, ZenSage, ZenPeach, ZenFlashcard, ZenSageLight)
                    if (state.phase == GamePhase.Running) state.bubbles.forEach { bubble ->
                        Box(
                            modifier = Modifier
                                .offset(bubble.x.dp, bubble.y.dp)
                                .size(state.diameter.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(palette[(bubble.number - 1) % palette.size])
                                .border(1.dp, ZenText.copy(alpha = 0.25f), CircleShape)
                                .semantics { contentDescription = "Burbuja número ${bubble.number}" }
                                .clickable(role = Role.Button, onClick = { viewModel.tap(bubble.number) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${bubble.number}",
                                fontFamily = ShortStack,
                                color = ZenText,
                                fontSize = (state.diameter * 0.32f).sp
                            )
                        }
                    }

                    if (state.phase != GamePhase.Running) Column(
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (state.phase) {
                            GamePhase.Ready -> {
                                GameTitle()
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Toca las burbujas del 1 al 20 en orden.\nSi te equivocas, se reacomodarán.",
                                    fontFamily = Livvic, color = ZenText, fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (records.owner == null) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(records.message ?: "Abriendo historial local…",
                                        fontFamily = Livvic, color = ZenText, textAlign = TextAlign.Center)
                                    if (records.message != null) {
                                        Button(onClick = viewModel::loadHistory) { Text("Reintentar") }
                                    }
                                }
                                Spacer(Modifier.height(32.dp))
                            }
                            GamePhase.Paused -> {
                                Text("Partida pausada", fontFamily = ShortStack, color = ZenText, fontSize = 20.sp)
                                Spacer(Modifier.height(20.dp))
                            }
                            GamePhase.Finished -> {
                                Text("¡JUEGO COMPLETADO!", fontFamily = ShortStack, color = ZenText,
                                    fontSize = 20.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(20.dp))
                                ResultRow("Tiempo", formatTime(state.elapsedMs))
                                ResultRow("Aciertos", "${state.correctTaps}")
                                ResultRow("Errores", "${state.errors}")
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    when (records.saveStatus) {
                                        SaveStatus.Saving -> "Guardando partida localmente…"
                                        SaveStatus.Saved -> "Partida guardada en este dispositivo"
                                        SaveStatus.Failed -> records.message ?: "No se pudo guardar la partida."
                                        else -> "Preparando guardado…"
                                    },
                                    fontFamily = Livvic, color = ZenText, textAlign = TextAlign.Center
                                )
                                if (records.saveStatus == SaveStatus.Failed) {
                                    Button(onClick = viewModel::retrySave) { Text("Reintentar guardado") }
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            else -> Unit
                        }

                        Button(
                            onClick = {
                                if (state.phase == GamePhase.Paused) viewModel.resume() else viewModel.start()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ZenOrange, contentColor = ZenText),
                            enabled = records.owner != null && records.saveStatus != SaveStatus.Saving &&
                                records.saveStatus != SaveStatus.Failed,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                when (state.phase) {
                                    GamePhase.Ready -> "Empezar"
                                    GamePhase.Paused -> "Reanudar"
                                    else -> "Jugar de nuevo"
                                },
                                fontFamily = ShortStack, fontSize = 18.sp
                            )
                        }
                        if (state.phase == GamePhase.Finished) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text("Volver al inicio", fontFamily = ShortStack, color = ZenText)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (records.owner != null && state.phase != GamePhase.Running) {
                    GameStatisticsPanel(records.statistics, records.calculatingStatistics,
                        records.refreshingHistory, records.refreshMessage)
                    Spacer(Modifier.height(16.dp))
                    Text("Historial local (últimas 5) · ${if (records.owner!!.isGuest) "Invitado" else "Mi cuenta"}",
                        fontFamily = ShortStack, color = ZenText, fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth())
                    if (records.syncing) {
                        Text("Sincronizando partidas…", fontFamily = Livvic, color = ZenText)
                    }
                    if (records.history.isEmpty()) {
                        Text("Todavía no hay partidas guardadas.", fontFamily = Livvic, color = ZenText)
                    } else {
                        records.history.take(5).forEach { session ->
                            Text(
                                "${session.completedAt.take(16).replace('T', ' ')} UTC · " +
                                    "${formatTime(session.durationMs.toLong())} · ${session.errors} errores · " +
                                    when (session.syncStatus) {
                                        GameSyncStatus.SYNCED -> "Sincronizada"
                                        GameSyncStatus.AUTH_REQUIRED -> "Inicia sesión de nuevo"
                                        GameSyncStatus.CONSENT_REQUIRED -> "Consentimiento requerido"
                                        GameSyncStatus.REJECTED -> "Rechazada por el servidor"
                                        else -> if (session.isGuest) "Solo local" else "Pendiente"
                                    },
                                fontFamily = Livvic, color = ZenText, fontSize = 14.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                            Text("ID: ${session.id}", fontFamily = Livvic, color = ZenText,
                                fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun GameStatisticsPanel(
    statistics: GameStatistics,
    calculating: Boolean,
    refreshing: Boolean,
    refreshMessage: String?
) {
    Text("Estadísticas de juegos", fontFamily = ShortStack, color = ZenText,
        fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
    when {
        calculating -> Text("Calculando estadísticas…", fontFamily = Livvic, color = ZenText)
        statistics.completedGames == 0 -> Text("Todavía no hay partidas completadas para este perfil.",
            fontFamily = Livvic, color = ZenText)
        else -> {
            ResultRow("Partidas completadas", "${statistics.completedGames}")
            ResultRow("Tiempo promedio", formatStatsTime(statistics.averageDurationMs!!))
            ResultRow("Mejor tiempo", formatStatsTime(statistics.bestDurationMs!!.toLong()))
            ResultRow("Errores totales", "${statistics.totalErrors}")
            ResultRow("Precisión global", "%.1f %%".format(java.util.Locale.getDefault(),
                statistics.accuracy * 100))
        }
    }
    if (refreshing) Text("Actualizando historial…", fontFamily = Livvic, color = ZenText)
    if (refreshMessage != null) Text(refreshMessage, fontFamily = Livvic, color = ZenText)
}

private fun formatStatsTime(milliseconds: Long): String {
    if (milliseconds < 60_000L) return formatTime(milliseconds)
    return "${milliseconds / 60_000L} min %.1f s".format(java.util.Locale.getDefault(),
        (milliseconds % 60_000L) / 1000.0)
}

@Composable
private fun GameTitle() {
    Text("BURBUJAS EN ORDEN", fontFamily = ShortStack, color = ZenText,
        fontSize = 18.sp, letterSpacing = 1.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    Text("Tócalas del 1 al 20 en orden", fontFamily = Livvic, color = ZenText,
        fontSize = 13.sp, textAlign = TextAlign.Center)
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = Livvic, fontSize = 16.sp, color = ZenText)
        Spacer(Modifier.weight(1f))
        Text(value, fontFamily = ShortStack, fontSize = 18.sp, color = ZenText)
    }
}

private fun formatTime(milliseconds: Long): String =
    "%.1f s".format(java.util.Locale.getDefault(), milliseconds / 1000.0)
