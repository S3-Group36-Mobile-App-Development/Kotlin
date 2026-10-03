package com.zenmind.kotlin.features.breathing.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.breathing.data.BreathingCatalog
import com.zenmind.kotlin.features.breathing.viewmodel.BreathingViewModel
import com.zenmind.kotlin.features.breathing.viewmodel.SessionStatus
import androidx.compose.ui.platform.LocalContext
import com.zenmind.kotlin.features.breathing.viewmodel.BreathingViewModelFactory
import com.zenmind.kotlin.ui.components.ZenTopBar

private val Cream = Color(0xFFFFF9E2)
private val TextBrown = Color(0xFF8A6F5B)
private val Peach = Color(0xFFDCA278)
private val ProgressTrack = Color(0xFFEDE6C7)
private val CardCream = Color(0xFFFEECD0)

@Composable
fun BreathingSessionScreen(
    onBack: () -> Unit = {},
    viewModel: BreathingViewModel = viewModel(
        factory = BreathingViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exercise = BreathingCatalog.default()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZenTopBar(title = "Respiración", onBack = onBack)

            Spacer(Modifier.height(20.dp))

            Text(
                text = exercise.description,
                color = TextBrown.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(48.dp))

            BreathingCircle(
                phase = state.phase,
                phaseLabel = when (state.status) {
                    SessionStatus.Idle -> "Empieza"
                    SessionStatus.Finished -> "Listo"
                    else -> state.phase?.label ?: ""
                },
                phaseSeconds = state.secondsLeft.coerceAtLeast(1),
                secondsLeft = state.secondsLeft
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = if (state.secondsLeft > 0) "${state.secondsLeft}s" else "",
                color = TextBrown,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = when (state.status) {
                    SessionStatus.Idle -> "Listo para empezar"
                    SessionStatus.Finished -> "Sesion completada"
                    else -> state.phase?.label ?: ""
                },
                color = TextBrown,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(24.dp))

            Spacer(Modifier.height(32.dp))

            if (state.totalCycles > 0) {
                Text(
                    text = "Respiracion ${state.currentCycle} de ${state.totalCycles}",
                    color = TextBrown,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        if (state.totalCycles == 0) 0f
                        else state.currentCycle.toFloat() / state.totalCycles
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    color = Peach,
                    trackColor = ProgressTrack
                )
            }

            state.message?.let {
                Spacer(Modifier.height(8.dp))
                Text(text = it, color = Color(0xFFB3261E), fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))

            when (state.status) {
                SessionStatus.Idle -> {
                    Button(
                        onClick = { viewModel.start(exercise) },
                        colors = ButtonDefaults.buttonColors(containerColor = Peach),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Empezar", color = Color.White
                        )
                    }
                }
                SessionStatus.Running -> {
                    OutlinedButton(
                        onClick = { viewModel.pause() },
                        border = BorderStroke(2.dp, Peach),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Pausar", color = TextBrown
                        )
                    }
                }
                SessionStatus.Paused -> {
                    Button(
                        onClick = { viewModel.resume() },
                        colors = ButtonDefaults.buttonColors(containerColor = Peach),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Reanudar", color = Color.White
                        )
                    }
                }
                SessionStatus.Finished -> {
                    Button(
                        onClick = { viewModel.start(exercise) },
                        colors = ButtonDefaults.buttonColors(containerColor = Peach),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Repetir", color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardCream)
                    .border(
                        width = 3.dp,
                        color = Peach,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Escucha con calma",
                    color = TextBrown,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )

                Text(
                    text = "Audios para despejar tu mente",
                    color = TextBrown,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(64.dp))
        }
    }
}
