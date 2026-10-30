package com.zenmind.kotlin.features.dailycheckin.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.dailycheckin.model.DailyCheckInUiState
import com.zenmind.kotlin.features.dailycheckin.model.Mood
import com.zenmind.kotlin.features.dailycheckin.model.Streak
import com.zenmind.kotlin.features.dailycheckin.viewmodel.DailyCheckInViewModel
import com.zenmind.kotlin.ui.components.FlashcardCard
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenBrown
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenGreenLine
import com.zenmind.kotlin.ui.theme.ZenText
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.savedMessage) {
        state.savedMessage?.let {
            snackbarHostState.showSnackbar(it)
            checkInViewModel.onSavedMessageShown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        DailyCheckInContent(
            state = state,
            onBack = onBack,
            onGoToBreathing = onGoToBreathing,
            onMoodSelected = checkInViewModel::onMoodSelected,
            onNoteChange = checkInViewModel::onNoteChange,
            onSave = checkInViewModel::onSave
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) { data ->
            Snackbar(
                containerColor = ZenBrown,
                contentColor = ZenCream,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(data.visuals.message, fontFamily = Nunito, fontSize = 13.sp)
            }
        }
    }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        ZenTopBar(title = "Check-in", onBack = onBack)

        var offlineDismissed by remember(state.isOffline) { mutableStateOf(false) }
        if (state.isOffline && !offlineDismissed) {
            OfflineBanner(onClose = { offlineDismissed = true })
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZenOrange)
            }
        } else if (state.alreadyCheckedInToday) {
            CheckInResult(state = state, onGoToBreathing = onGoToBreathing)
        } else {
            CheckInForm(
                state = state,
                onMoodSelected = onMoodSelected,
                onNoteChange = onNoteChange,
                onSave = onSave
            )
        }
    }
}

@Composable
private fun CheckInForm(
    state: DailyCheckInUiState,
    onMoodSelected: (Mood) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val canEdit = !state.isSaving

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp)
        ) {
            Text("¿Cómo te sientes hoy?", fontFamily = NunitoBold, fontSize = 22.sp, color = ZenLabel)
            Text("Tómate un instante para evaluar tu estado interno.", fontFamily = Nunito, fontSize = 13.sp, color = ZenSubtle)

            state.streak?.let {
                Spacer(Modifier.height(20.dp))
                StreakPanel(streak = it)
            }

            Spacer(Modifier.height(20.dp))
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
            Text("¿Quieres contarnos algo? (opcional)", fontFamily = NunitoBold, fontSize = 14.sp, color = ZenLabel)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                enabled = canEdit,
                placeholder = {
                    Text("Describe brevemente cómo fluyen tus pensamientos hoy...", fontFamily = Nunito, fontSize = 14.sp)
                },
                textStyle = TextStyle(fontFamily = Nunito, fontSize = 14.sp),
                supportingText = {
                    Text(
                        "${state.note.length}/${DailyCheckInViewModel.MAX_NOTE_LENGTH}",
                        fontFamily = Nunito,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ZenText,
                    unfocusedTextColor = ZenText,
                    disabledTextColor = ZenText,
                    focusedPlaceholderColor = ZenSubtle,
                    unfocusedPlaceholderColor = ZenSubtle,
                    disabledPlaceholderColor = ZenSubtle,
                    focusedSupportingTextColor = ZenSubtle,
                    unfocusedSupportingTextColor = ZenSubtle,
                    disabledSupportingTextColor = ZenSubtle,
                    cursorColor = ZenOrange,
                    focusedContainerColor = ZenPeach,
                    unfocusedContainerColor = ZenPeach,
                    disabledContainerColor = ZenPeach,
                    focusedBorderColor = ZenOrange,
                    unfocusedBorderColor = ZenOrange,
                    disabledBorderColor = ZenOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            state.message?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, fontFamily = NunitoBold, fontSize = 13.sp, color = ZenLabel)
            }
        }

        val showMoodHint = canEdit && state.moods.isNotEmpty() && state.selectedMoodId == null
        if (!showMoodHint && state.isGuest) {
            NoticeBanner("Modo invitado: tu check-in se guardará solo en este teléfono.")
        }
        if (showMoodHint) {
            Text(
                "Elige cómo te sientes para guardar",
                fontFamily = Nunito,
                fontSize = 12.sp,
                color = ZenSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
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

@Composable
private fun CheckInResult(
    state: DailyCheckInUiState,
    onGoToBreathing: () -> Unit
) {
    val mood = state.moods.firstOrNull { it.id == state.selectedMoodId }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp)
        ) {
            mood?.let {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hoy registraste", fontFamily = NunitoBold, fontSize = 16.sp, color = ZenLabel)
                    Spacer(Modifier.height(10.dp))
                    MoodBadge(mood = it)
                }
            }

            state.streak?.let {
                Spacer(Modifier.height(20.dp))
                StreakPanel(streak = it)
            }

            Spacer(Modifier.height(24.dp))
            Text("Tu card de hoy", fontFamily = NunitoBold, fontSize = 22.sp, color = ZenLabel)
            Spacer(Modifier.height(12.dp))

            val flashcard = state.recommendedFlashcard
            when {
                flashcard != null ->
                    FlashcardCard(title = flashcard.category, content = flashcard.content, source = flashcard.source)
                state.isLoadingCard ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ZenOrange, modifier = Modifier.size(28.dp))
                    }
                state.isOffline && mood?.name in MOODS_WITH_CARD ->
                    Text(
                        "Tu card aparecerá cuando vuelvas a tener internet.",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        color = ZenLabel
                    )
                else ->
                    Text(
                        "Gracias por registrar cómo te sientes. Vuelve mañana para seguir tu racha.",
                        fontFamily = Nunito,
                        fontSize = 14.sp,
                        color = ZenLabel
                    )
            }

            if (mood?.name == "Ansioso") {
                Spacer(Modifier.height(24.dp))
                Text("Te recomendamos", fontFamily = NunitoBold, fontSize = 14.sp, color = ZenLabel)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onGoToBreathing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZenSage),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    BasicText(
                        text = "Hacer un ejercicio de respiración",
                        style = TextStyle(fontFamily = ShortStack, color = ZenIcon, textAlign = TextAlign.Center),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 15.sp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (state.isGuest) {
            NoticeBanner("Modo invitado: tu check-in quedó guardado solo en este teléfono.")
        } else if (state.pendingSync) {
            NoticeBanner("Pendiente de sincronizar: se enviará cuando vuelvas a tener internet.")
        }
        if (state.streak == null && state.isOffline) {
            NoticeBanner("Tu racha aparecerá cuando vuelvas a tener internet.")
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
        BasicText(
            text = mood.displayName,
            style = TextStyle(fontFamily = NunitoBold, color = ZenSubtle, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 11.sp),
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun StreakPanel(streak: Streak) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        StreakItem("Días seguidos", streak.current, Modifier.weight(1f))
        Box(
            Modifier
                .width(1.dp)
                .height(56.dp)
                .background(ZenGreenLine)
        )
        StreakItem("Tu mejor marca", streak.longest, Modifier.weight(1f))
    }
}

@Composable
private fun StreakItem(label: String, days: Int, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, fontFamily = NunitoBold, fontSize = 12.sp, color = ZenLabel)
        Spacer(Modifier.height(6.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .background(ZenPeach, CircleShape)
                .border(2.dp, ZenOrange, CircleShape)
        ) {
            Text("$days", fontFamily = ShortStack, fontSize = 17.sp, color = ZenText)
        }
        Spacer(Modifier.height(2.dp))
        Text(if (days == 1) "día" else "días", fontFamily = Nunito, fontSize = 10.sp, color = ZenSubtle)
    }
}

@Composable
private fun MoodBadge(mood: Mood) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emojiFor(mood.name), fontSize = 32.sp)
        Text(mood.displayName, fontFamily = NunitoBold, fontSize = 16.sp, color = ZenLabel)
    }
}

private val MOODS_WITH_CARD = setOf("Ansioso", "Estresado")

@Composable
private fun OfflineBanner(onClose: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(ZenBrown, shape)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 32.dp)
        ) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = ZenCream, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Sin conexión. Tus check-ins se guardan en este teléfono y se envían cuando vuelvas a tener internet.",
                fontFamily = Nunito,
                fontSize = 13.sp,
                color = ZenCream
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Cerrar aviso", tint = ZenCream, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun NoticeBanner(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(bottom = 6.dp)
            .fillMaxWidth()
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = ZenSubtle, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontFamily = Nunito, fontSize = 12.sp, color = ZenSubtle)
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
