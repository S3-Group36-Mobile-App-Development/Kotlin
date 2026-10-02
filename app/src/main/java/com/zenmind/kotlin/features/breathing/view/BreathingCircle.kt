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

private val TextBrown = Color(0xFF8A6F5B)
private val CirclePeach = Color(0xFFDCA278)

/**
 * Circulo guia de la respiracion, con tres aros concentricos.
 * Dentro del circulo solido se muestra el numero de segundos y,
 * justo debajo, el nombre de la fase (Inhala/Sosten/Exhala).
 *
 * @param phase fase actual (o null si no ha empezado).
 * @param phaseLabel texto de la fase para mostrar dentro del circulo.
 * @param phaseSeconds duracion de la fase, para que la animacion dure igual.
 * @param secondsLeft segundos restantes que se muestran dentro del circulo.
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

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = phaseSeconds.coerceAtLeast(1) * 1000),
        label = "breathing-scale"
    )

    Box(contentAlignment = Alignment.Center) {
        // Aro exterior muy tenue.
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(CirclePeach.copy(alpha = 0.15f))
        )
        // Aro medio.
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(CirclePeach.copy(alpha = 0.3f))
        )
        // Circulo solido central con el numero y el label debajo.
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(CirclePeach),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (secondsLeft > 0) "${secondsLeft}s" else "",
                    color = TextBrown,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
                if (phaseLabel.isNotEmpty()) {
                    Text(
                        text = phaseLabel,
                        color = TextBrown,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
