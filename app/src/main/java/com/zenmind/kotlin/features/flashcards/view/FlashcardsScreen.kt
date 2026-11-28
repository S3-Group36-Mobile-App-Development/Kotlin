package com.zenmind.kotlin.features.flashcards.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.flashcards.model.FlashcardTemaDto
import com.zenmind.kotlin.features.flashcards.viewmodel.FlashCardsViewModel
import com.zenmind.kotlin.features.flashcards.viewmodel.FlashCardsViewModelFactory
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSubtle
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun FlashcardsScreen(
    onBack: () -> Unit = {},
    onCardClick: (String) -> Unit = {},
    viewModel: FlashCardsViewModel = viewModel(
        factory = FlashCardsViewModelFactory(LocalContext.current)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadTemas(null) // Carga todos los temas directamente desde el backend
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenCream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            ZenTopBar(
                title = "FLASHCARDS",
                onBack = onBack
            )

            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZenOrange)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = state.temas,
                        key = { tema -> tema.id }
                    ) { tema ->
                        FlashcardListItemCard(
                            tema = tema,
                            onClick = { onCardClick(tema.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardListItemCard(
    tema: FlashcardTemaDto,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZenPeach)
            .border(2.dp, ZenOrange.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .background(ZenCream, CircleShape)
                .border(1.dp, ZenOrange.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = if (tema.iconType == "heart") Icons.Outlined.FavoriteBorder else Icons.Outlined.Search,
                contentDescription = null,
                tint = ZenOrange,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = tema.categoria,
                fontFamily = ShortStack,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = ZenSubtle
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = tema.titulo,
                color = ZenText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
