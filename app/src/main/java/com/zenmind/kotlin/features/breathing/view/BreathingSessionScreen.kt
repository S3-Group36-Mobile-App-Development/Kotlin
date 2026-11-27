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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import com.zenmind.kotlin.core.network.ConnectivityObserver
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CloudOff

private val Cream = Color(0xFFFFF9E2)
private val TextBrown = Color(0xFF8A6F5B)
private val Peach = Color(0xFFDCA278)
private val ProgressTrack = Color(0xFFEDE6C7)
private val CardCream = Color(0xFFFEECD0)

@Composable
fun BreathingSessionScreen(
    exerciseId: Int = -1,
    onBack: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    viewModel: BreathingViewModel = viewModel(
        factory = BreathingViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isOnline by remember(context) {
        ConnectivityObserver(context).onlineFlow
    }.collectAsState(initial = true)

    var wasOffline by remember { mutableStateOf(false) }
    LaunchedEffect(isOnline) {
        if (!isOnline) {
            wasOffline = true
        } else if (wasOffline) {
            android.widget.Toast.makeText(
                context,
                "¡Conexión recuperada! Sincronizando sesiones pendientes con el servidor...",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            wasOffline = false
        }
    }

    val exercise by androidx.compose.runtime.produceState(
        initialValue = BreathingCatalog.default(),
        key1 = exerciseId
    ) {
        val repo = com.zenmind.kotlin.features.breathing.data.BreathingRepository(
            dao = com.zenmind.kotlin.core.database.AppDatabase.getInstance(context).breathingSessionDao(),
            exerciseDao = com.zenmind.kotlin.core.database.AppDatabase.getInstance(context).breathingExerciseDao(),
            preferences = com.zenmind.kotlin.features.breathing.data.local.BreathingPreferences(context),
            context = context.applicationContext
        )
        val catalog = repo.getExercises()
        val chosenId = if (exerciseId != -1) exerciseId else repo.getFavorite()
        value = catalog.firstOrNull { it.id == chosenId }
            ?: catalog.firstOrNull()
            ?: BreathingCatalog.default()
    }

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
            Box(modifier = Modifier.fillMaxWidth()) {
                ZenTopBar(title = "Respiración", onBack = onBack)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardCream)
                        .border(2.dp, Peach, CircleShape)
                        .clickable(onClick = onOpenCatalog)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = "Ver ejercicios",
                        tint = TextBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (!isOnline) {
                Spacer(Modifier.height(8.dp))
                OfflineNoticeBanner("Modo Offline: Al finalizar, tu sesión se guardará localmente en Room y se sincronizará cuando vuelva la red.")
            }

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
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
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

@Composable
private fun OfflineNoticeBanner(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Peach.copy(alpha = 0.2f))
            .border(1.dp, Peach, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = TextBrown,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = TextBrown,
            fontWeight = FontWeight.Medium
        )
    }
}