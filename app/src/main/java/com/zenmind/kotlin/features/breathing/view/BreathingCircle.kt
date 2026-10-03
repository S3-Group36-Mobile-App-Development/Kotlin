package com.zenmind.kotlin.features.breathing.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.features.breathing.model.BreathingPhase
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.zenmind.kotlin.R
import androidx.compose.animation.animateColorAsState

private val TextBrown = Color(0xFF8A6F5B)
private val CirclePeach = Color(0xFFDCA278)
private val Images = Color(0xFFFFF9E2)
private val MatchaColor = Color(0xC0D17B)

/**
 * Circulo guia de la respiracion.
 * Se muestra el numero de segundos y,
 * el nombre de la fase (Inhala/Sosten/Exhala).
 *
 * @param phase fase actual (o null si no ha empezado).
 * @param phaseLabel texto de la fase para mostrar.
 * @param phaseSeconds duracion de la fase, para que la animacion dure igual.
 * @param secondsLeft segundos restantes.
 */
@Composable
fun BreathingCircle(
    phase: BreathingPhase?,
    phaseLabel: String,
    phaseSeconds: Int,
    secondsLeft: Int
) {
    val targetScale = when (phase) {
        BreathingPhase.Inhale -> 1.0f
        BreathingPhase.Hold -> 1.0f
        BreathingPhase.Exhale -> 0.6f
        null -> 0.8f
    }

    val targetColor = when (phase) {
        BreathingPhase.Inhale -> MatchaColor
        BreathingPhase.Hold -> MatchaColor
        BreathingPhase.Exhale -> CirclePeach
        null -> CirclePeach
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = phaseSeconds.coerceAtLeast(1) * 1000),
        label = "breathing-scale"
    )

    val colorAnimate by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = phaseSeconds.coerceAtLeast(1) * 1000),
        label = "breathing-color"
    )

    Box(contentAlignment = Alignment.Center) {
        // Aro exterior.
        Box(
            modifier = Modifier
                .size(340.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(colorAnimate.copy(alpha = 0.15f))
        )
        // Aro medio.
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(colorAnimate.copy(alpha = 0.3f))
        )
        // Circulo solido central.
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(colorAnimate),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (phase){
                    BreathingPhase.Inhale -> {
                        Image(
                            painter = painterResource(id = R.drawable.inhale),
                            contentDescription = "Nube de respiracion",
                            colorFilter = ColorFilter.tint(Images),
                            modifier = Modifier.size(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    BreathingPhase.Exhale -> {
                        Image(
                            painter = painterResource(id = R.drawable.exhale),
                            contentDescription = "Nube de respiracion",
                            colorFilter = ColorFilter.tint(Images),
                            modifier = Modifier.size(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    BreathingPhase.Hold -> {
                        Image(
                            painter = painterResource(id = R.drawable.hold),
                            contentDescription = "Nube de respiracion",
                            colorFilter = ColorFilter.tint(Images),
                            modifier = Modifier.size(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    null -> {}
                }
            }
        }
    }
}
