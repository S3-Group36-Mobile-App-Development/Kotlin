package com.zenmind.kotlin.features.dailycheckin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.dailycheckin.model.DailyCheckInUiState
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.viewmodel.DailyCheckInViewModel
import com.zenmind.kotlin.ui.components.FlashcardCard
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenIcon
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenMuted
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSage
import com.zenmind.kotlin.ui.theme.ZenSageLight
import com.zenmind.kotlin.ui.theme.ZenShadow
import com.zenmind.kotlin.ui.theme.ZenSubtle

// Pantalla conectada al ViewModel
@Composable
fun DailyCheckInScreen(
    onBack: () -> Unit,
    onGoToBreathing: () -> Unit,
    checkInViewModel: DailyCheckInViewModel = viewModel(factory = DailyCheckInViewModel.Factory)
) {
    val state by checkInViewModel.uiState.collectAsStateWithLifecycle()
    DailyCheckInContent(
        state = state,
        onBack = onBack,
        onGoToBreathing = onGoToBreathing,
        onMoodSelected = checkInViewModel::onMoodSelected,
        onNoteChange = checkInViewModel::onNoteChange,
        onSave = checkInViewModel::onSave
    )
}

// UI sin estado
@Composable
fun DailyCheckInContent(
    state: DailyCheckInUiState,
    onBack: () -> Unit,
    onGoToBreathing: () -> Unit,
    onMoodSelected: (Mood) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val canEdit = !state.alreadyCheckedInToday && !state.isSaving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        ZenTopBar(title = "Check-in", onBack = onBack)

        // Sin sesión (invitado)
        if (state.notLoggedIn) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Inicia sesión para hacer tu check-in",
                    fontFamily = NunitoBold,
                    fontSize = 22.sp,
                    color = ZenLabel,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp)
            ) {
                Text("¿Cómo te sientes hoy?", fontFamily = NunitoBold, fontSize = 22.sp, color = ZenLabel)
                Text("Tómate un instante para evaluar tu estado interno.", fontFamily = Nunito, fontSize = 13.sp, color = ZenSubtle)

                state.streak?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "🔥 Racha actual: ${it.current} días   🏆 Mejor: ${it.longest} días",
                        fontFamily = NunitoBold, fontSize = 13.sp, color = ZenLabel
                    )
                }

                Spacer(Modifier.height(20.dp))
                if (state.isLoading) {
                    Text("Cargando...", fontFamily = Nunito, fontSize = 13.sp, color = ZenSubtle)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.moods.forEach { mood ->
                        MoodChip(
                            mood = mood,
                            selected = mood.id == state.selectedMoodId,
                            enabled = canEdit,
                            onClick = { onMoodSelected(mood) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("¿Quieres agregar algo más?", fontFamily = NunitoBold, fontSize = 14.sp, color = ZenLabel)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    enabled = canEdit,
                    placeholder = { Text("Describe brevemente cómo fluyen tus pensamientos hoy...") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ZenPeach,
                        unfocusedContainerColor = ZenPeach,
                        disabledContainerColor = ZenPeach,
                        focusedBorderColor = ZenOrange,
                        unfocusedBorderColor = ZenOrange,
                        disabledBorderColor = ZenOrange
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                state.message?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, fontFamily = NunitoBold, fontSize = 13.sp, color = ZenLabel)
                }

                // SMART FEATURE: flashcard recomendada según el mood (sale abajo)
                state.recommendedFlashcard?.let {
                    Spacer(Modifier.height(24.dp))
                    Text("Te recomendamos", fontFamily = NunitoBold, fontSize = 14.sp, color = ZenLabel)
                    Spacer(Modifier.height(8.dp))
                    FlashcardCard(title = it.category, content = it.content, source = it.source)
                }

                val selectedMood = state.moods.firstOrNull { it.id == state.selectedMoodId }
                if (selectedMood?.name == "Ansioso") {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onGoToBreathing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZenSage),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            "Hacer un ejercicio de respiración",
                            fontFamily = ShortStack,
                            fontSize = 15.sp,
                            color = ZenIcon
                        )
                    }
                }
            }

            Button(
                onClick = onSave,
                enabled = canEdit && state.selectedMoodId != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZenOrange, disabledContainerColor = ZenMuted),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(if (state.isSaving) "Guardando..." else "Guardar Check-in", fontFamily = ShortStack, fontSize = 16.sp, color = ZenIcon)
            }
        }
    }
}

@Composable
private fun MoodChip(
    mood: Mood,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(64.dp)
            .padding(bottom = 4.dp)
            .dropShadow(shape = shape, shadow = Shadow(radius = 0.dp, color = ZenShadow, offset = DpOffset(0.dp, 4.dp)))
            .clip(shape)
            .background(if (selected) ZenSage else ZenPeach)
            .border(1.dp, if (selected) ZenSage else ZenOrange, shape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(emojiFor(mood.name), fontSize = 18.sp)
        Text(mood.name, fontFamily = NunitoBold, fontSize = 10.sp, color = ZenSubtle, maxLines = 1)
    }
}

private fun emojiFor(moodName: String): String = when (moodName) {
    "Feliz" -> "😊"
    "Tranquilo" -> "😌"
    "Ansioso" -> "😟"
    "Triste" -> "😔"
    "Estresado" -> "😣"
    else -> "🙂"
}
