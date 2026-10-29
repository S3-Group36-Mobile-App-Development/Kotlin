package com.zenmind.kotlin.features.home.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenmind.kotlin.R
import com.zenmind.kotlin.features.home.model.HomeData
import com.zenmind.kotlin.features.home.model.HomeFeature
import com.zenmind.kotlin.features.home.model.HomeUiState
import com.zenmind.kotlin.features.home.viewmodel.HomeViewModel
import com.zenmind.kotlin.ui.components.ZenBottomBar
import com.zenmind.kotlin.ui.components.ZenTab
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenGreenLine
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenSage
import com.zenmind.kotlin.ui.theme.ZenSageBorder
import com.zenmind.kotlin.ui.theme.ZenSageLight
import com.zenmind.kotlin.ui.theme.ZenShadow
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onFeatureClick: (HomeFeature) -> Unit,
    onContinueClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onTabClick: (ZenTab) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {

        HomeUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ZenCream),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZenOrange)
            }
        }

        is HomeUiState.Error -> {
            HomeError(
                message = s.message,
                onRetry = viewModel::load
            )
        }

        is HomeUiState.Success -> {
            HomeContent(
                data = s.data,
                onFeatureClick = onFeatureClick,
                onContinueClick = onContinueClick,
                onCallClick = onCallClick,
                onTabClick = onTabClick,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

@Composable
fun HomeContent(
    data: HomeData,
    onFeatureClick: (HomeFeature) -> Unit,
    onContinueClick: () -> Unit = {},
    onCallClick: () -> Unit = {},
    onTabClick: (ZenTab) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Scaffold(
        containerColor = ZenCream,

        bottomBar = {
            ZenBottomBar(
                selected = ZenTab.HOME,
                onTabClick = onTabClick
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onCallClick,
                shape = CircleShape,
                containerColor = ZenOrange,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Outlined.Phone,
                    contentDescription = "Llamar a tu contacto de apoyo"
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 4.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            HomeHeader(
                onLogoutClick = onLogoutClick
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            CheckInCard(
                onClick = onContinueClick
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            FeatureGrid(
                onFeatureClick = onFeatureClick
            )

            Spacer(
                modifier = Modifier.height(80.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Cerrar sesión",
            fontFamily = NunitoBold,
            fontSize = 12.sp,
            color = ZenOrange,
            modifier = Modifier
                .align(Alignment.End)
                .clickable {
                    onLogoutClick()
                }
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                )
        )

        Image(
            painter = painterResource(
                R.drawable.zenmind_logo
            ),
            contentDescription = "ZenMind",
            modifier = Modifier.width(160.dp)
        )

        Text(
            text = "ZEN MIND",
            fontFamily = ShortStack,
            fontSize = 9.sp,
            letterSpacing = 8.sp,
            color = ZenOrange.copy(
                alpha = 0.6f
            )
        )
    }
}

@Composable
private fun CheckInCard(
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ZenSageLight)
            .border(
                width = 1.dp,
                color = ZenGreenLine,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 20.dp,
                vertical = 18.dp
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 32.dp)
        ) {
            Text(
                text = "C H E C K - I N",
                fontFamily = ShortStack,
                fontSize = 16.sp,
                color = ZenText
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "¿Cómo te sientes hoy? Recibe tu card diaria.",
                fontFamily = Nunito,
                fontSize = 14.sp,
                color = ZenText
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ir al check-in",
            tint = ZenText,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun FeatureGrid(
    onFeatureClick: (HomeFeature) -> Unit
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 3 else 2
    val spacing = 24.dp

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val fitWidth = (maxWidth - spacing * (columns - 1)) / columns
        val cardWidth = if (isLandscape) {
            minOf(FEATURE_CARD_MAX_WIDTH_LANDSCAPE, fitWidth)
        } else {
            minOf(FEATURE_CARD_WIDTH, fitWidth)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeFeature.entries
                .chunked(columns)
                .forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        row.forEach { feature ->
                            FeatureCard(
                                feature = feature,
                                onClick = onFeatureClick,
                                modifier = Modifier.width(cardWidth)
                            )
                        }
                    }
                }
        }
    }
}

private val FEATURE_CARD_WIDTH = 140.dp
private val FEATURE_CARD_MAX_WIDTH_LANDSCAPE = 200.dp

@Composable
private fun FeatureCard(
    feature: HomeFeature,
    onClick: (HomeFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    val borderColor =
        if (feature.warm) {
            ZenOrange
        } else {
            ZenSageBorder
        }

    Box(
        modifier = modifier.aspectRatio(0.97f)
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = 4.dp)
                .dropShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = 0.dp,
                        color = ZenShadow,
                        offset = DpOffset(
                            0.dp,
                            4.dp
                        )
                    )
                )
                .clip(shape)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = shape
                )
                .clickable {
                    onClick(feature)
                }
        ) {

            Image(
                painter = painterResource(
                    feature.image
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            BasicText(
                text = feature.label.uppercase(),
                style = TextStyle(
                    fontFamily = ShortStack,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    color = ZenLabel
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 8.dp, end = 8.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun HomeError(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenCream)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "No pudimos conectarnos",
            fontFamily = ShortStack,
            fontSize = 18.sp,
            color = ZenText
        )

        Text(
            text = message,
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = ZenText
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = ZenOrange
            )
        ) {
            Text(
                text = "Reintentar"
            )
        }
    }
}
