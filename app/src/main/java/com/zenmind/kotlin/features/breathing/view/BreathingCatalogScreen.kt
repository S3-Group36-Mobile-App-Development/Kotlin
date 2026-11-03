package com.zenmind.kotlin.features.breathing.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.features.breathing.viewmodel.BreathingCatalogViewModel
import com.zenmind.kotlin.features.breathing.viewmodel.BreathingViewModelFactory
import com.zenmind.kotlin.ui.components.ZenTopBar

private val Cream = Color(0xFFFFF9E2)
private val TextBrown = Color(0xFF8A6F5B)
private val Peach = Color(0xFFDCA278)
private val CardCream = Color(0xFFFEECD0)

@Composable
fun BreathingCatalogScreen(
    onBack: () -> Unit = {},
    onSelect: (BreathingExercise) -> Unit = {},
    viewModel: BreathingCatalogViewModel = viewModel(
        factory = BreathingViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            ZenTopBar(title = "Ejercicios", onBack = onBack)

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        isFavorite = exercise.id == state.favoriteId,
                        onStart = { onSelect(exercise) },
                        onToggleFavorite = { viewModel.toggleFavorite(exercise.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: BreathingExercise,
    isFavorite: Boolean,
    onStart: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardCream)
            .border(2.dp, Peach, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                color = TextBrown,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Marcar favorito",
                    tint = Peach
                )
            }
        }

        Text(
            text = exercise.steps.joinToString(" - ") { it.seconds.toString() },
            color = Peach,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "${exercise.cycles} ciclos",
            color = TextBrown.copy(alpha = 0.6f),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = Peach),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Empezar", color = Color.White)
        }
    }
}