package com.zenmind.kotlin.features.authentication.presentation.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.R
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onGuestClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZenCream)
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.weight(1f)
        )

        Image(
            painter = painterResource(
                id = R.drawable.zenmind_logo
            ),
            contentDescription = "ZenMind",
            modifier = Modifier.width(200.dp)
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Z E N M I N D",
            fontFamily = ShortStack,
            fontSize = 17.sp,
            color = ZenOrange,
            letterSpacing = 5.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Tu espacio para cuidar tu bienestar.",
            fontFamily = Nunito,
            fontSize = 14.sp,
            color = ZenText,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(86.dp)
        )

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ZenOrange,
                contentColor = ZenText
            )
        ) {
            Text(
                text = "Iniciar sesión",
                fontFamily = NunitoBold,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedButton(
            onClick = onRegisterClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(
                width = 1.5.dp,
                color = ZenOrange
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ZenOrange
            )
        ) {
            Text(
                text = "Crear cuenta",
                fontFamily = NunitoBold,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier = Modifier.height(22.dp)
        )

        Text(
            text = "Continuar como invitado",
            fontFamily = NunitoBold,
            fontSize = 13.sp,
            color = ZenOrange,
            modifier = Modifier
                .clickable {
                    onGuestClick()
                }
                .padding(8.dp)
        )

        Spacer(
            modifier = Modifier.weight(1.15f)
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 380,
    heightDp = 830
)
@Composable
private fun WelcomeScreenPreview() {
    WelcomeScreen(
        onLoginClick = {},
        onRegisterClick = {},
        onGuestClick = {}
    )
}