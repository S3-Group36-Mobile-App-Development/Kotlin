/**package com.zenmind.kotlin.features.breathing.view

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.features.breathing.data.BreathingCatalog
import com.zenmind.kotlin.features.breathing.model.BreathingExercise
import com.zenmind.kotlin.ui.components.ZenTopBar

private val Cream = Color(0xFFFFF9E2)
private val TextBrown = Color(0xFF8A6F5B)
private val Peach = Color(0xFFDCA278)
private val CardCream = Color(0xFFFEECD0)

/**
 * Lista de los ejercicios de respiracion disponibles.
 *
 * @param onBack vuelve a la pantalla anterior.
 * @param onSelect se llama con el ejercicio elegido al tocar "Empezar".
 */
@Composable
fun BreathingCatalogScreen(
    onBack: () -> Unit = {}, onSelect: (BreathingExercise) -> Unit = {}
) {
    val exercises = listOf(
        BreathingCatalog.default(),
        BreathingCatalog.equal(),
        BreathingCatalog.sevenEleven(),
        BreathingCatalog.relax()
    )

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
                items(exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onStart = { onSelect(exercise) }
                    )
                }
            }
        }
    }
}

/**
 * Ficha
 */
@Composable
private fun ExerciseCard(
    exercise: BreathingExercise,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardCream)
            .border(2.dp, Peach, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Text(
            text = exercise.name,
            color = TextBrown,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(4.dp))

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

**/