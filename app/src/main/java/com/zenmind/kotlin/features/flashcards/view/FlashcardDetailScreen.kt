package com.zenmind.kotlin.features.flashcards.view

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.core.storage.TokenStorage
import com.zenmind.kotlin.features.flashcards.model.FlashcardCardDto
import com.zenmind.kotlin.features.flashcards.viewmodel.FlashcardDetailViewModel
import com.zenmind.kotlin.features.flashcards.viewmodel.FlashcardDetailViewModelFactory
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenFlashcard
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSage
import com.zenmind.kotlin.ui.theme.ZenSageLight
import com.zenmind.kotlin.ui.theme.ZenText

private val cardColors = listOf(
    ZenFlashcard,
    ZenPeach,
    ZenSageLight,
    ZenSage
)

@Composable
fun FlashcardDetailScreen(
    temaId: String,
    onBack: () -> Unit = {},
    viewModel: FlashcardDetailViewModel = viewModel(
        factory = FlashcardDetailViewModelFactory(LocalContext.current, temaId)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cards = state.tema?.cards ?: emptyList()
    val pagerState = rememberPagerState(initialPage = 0) { cards.size }
    val context = LocalContext.current
    val token = remember { TokenStorage(context).getAccessToken() ?: "" }

    LaunchedEffect(token) {
        viewModel.loadDetail(token)
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
                .padding(vertical = 12.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                ZenTopBar(title = "", onBack = onBack)
            }

            Spacer(Modifier.height(24.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZenOrange)
                }
            } else if (cards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No hay tarjetas disponibles", color = ZenLabel)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    beyondViewportPageCount = 1
                ) { page ->
                    val card = cards[page]
                    val backgroundColor = cardColors[page % cardColors.size]
                    val reaction = state.userReactions[card.id]

                    FlashcardContentCard(
                        card = card,
                        backgroundColor = backgroundColor,
                        initialReaction = reaction,
                        token = token,
                        viewModel = viewModel
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FlashcardContentCard(
    card: FlashcardCardDto,
    backgroundColor: Color,
    initialReaction: String?,
    token: String,
    viewModel: FlashcardDetailViewModel
) {
    val shape = RoundedCornerShape(28.dp)
    val liked = initialReaction == "LIKE"
    val disliked = initialReaction == "DISLIKE"

    val borderColor = when (backgroundColor) {
        ZenFlashcard -> Color(0xFFD49A6A)
        ZenPeach -> Color(0xFFE8B68E)
        ZenSageLight -> Color(0xFFB5BE92)
        else -> Color(0xFF9EAA75)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(2.dp, borderColor, shape)
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = card.titulo.uppercase(),
                color = ZenText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = card.contenido,
                color = ZenText.copy(alpha = 0.85f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (disliked) {
                            viewModel.removeReaction(card.id, token)
                        } else {
                            viewModel.reactToCard(card.id, "DISLIKE", token)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (disliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "No me gusta",
                        tint = if (disliked) ZenOrange else ZenLabel,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (liked) {
                            viewModel.removeReaction(card.id, token)
                        } else {
                            viewModel.reactToCard(card.id, "LIKE", token)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Me gusta",
                        tint = if (liked) ZenOrange else ZenLabel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = card.fuente,
                color = ZenText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
