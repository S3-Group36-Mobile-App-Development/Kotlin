package com.zenmind.kotlin.features.home.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
                    .background(ZenCream)
            )
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

            GreetingCard(
                userName = data.userName,
                onContinueClick = onContinueClick
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
private fun GreetingCard(
    userName: String?,
    onContinueClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ZenSageLight,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = ZenGreenLine,
                shape = shape
            )
            .padding(
                horizontal = 20.dp,
                vertical = 18.dp
            )
    ) {

        Text(
            text = if (userName != null) {
                "Hola, $userName ¿Cómo te sientes hoy?"
            } else {
                "Hola ¿Cómo te sientes hoy?"
            },
            fontFamily = ShortStack,
            fontSize = 16.sp,
            color = ZenText
        )

        Text(
            text = "Reconocer lo que sientes es el primer paso",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = ZenText
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        val pill = RoundedCornerShape(50)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    color = ZenSage,
                    shape = pill
                )
                .border(
                    width = 1.dp,
                    color = ZenSageBorder,
                    shape = pill
                )
                .clickable(
                    onClick = onContinueClick
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Continuar",
                fontFamily = ShortStack,
                fontSize = 14.sp,
                color = ZenText
            )
        }
    }
}

@Composable
private fun FeatureGrid(
    onFeatureClick: (HomeFeature) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            18.dp
        )
    ) {

        HomeFeature.entries
            .chunked(2)
            .forEach { row ->

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(24.dp)
                ) {

                    row.forEach { feature ->

                        FeatureCard(
                            feature = feature,
                            onClick = onFeatureClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
    }
}

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

            Text(
                text = feature.label.uppercase(),
                fontFamily = ShortStack,
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                color = ZenLabel,
                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(
                        bottom = 12.dp
                    )
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
