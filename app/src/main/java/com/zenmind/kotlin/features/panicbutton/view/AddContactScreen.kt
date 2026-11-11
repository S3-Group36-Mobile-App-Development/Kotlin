package com.zenmind.kotlin.features.panicbutton.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.features.panicbutton.model.SupportContact
import com.zenmind.kotlin.ui.theme.Livvic
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenSage
import com.zenmind.kotlin.ui.theme.ZenSageLight

private val Background = Color(0xFFFFF9E2)

@Composable
fun AddContactScreen(
    onBack: () -> Unit = {},
    onSave: (SupportContact) -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val canSave = name.isNotBlank() && phone.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Encabezado: botón volver
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBack)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver",
                    tint = ZenLabel,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Volver",
                    color = ZenLabel,
                    fontFamily = Livvic,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "N U E V O   C O N T A C T O",
                color = ZenLabel,
                fontFamily = ShortStack,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Agrega a alguien de confianza para tenerlo a mano.",
                color = ZenLabel.copy(alpha = 0.7f),
                fontFamily = Livvic,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            FieldLabel("Nombre")
            Spacer(Modifier.height(8.dp))
            ZenTextField(
                value = name,
                onValueChange = { text ->
                    if (text.length <= 30) name = text
                },
                placeholder = "Ej. Mamá",
                keyboardType = KeyboardType.Text
            )

            Spacer(Modifier.height(20.dp))

            FieldLabel("Teléfono")
            Spacer(Modifier.height(8.dp))
            ZenTextField(
                value = phone,
                onValueChange = { input ->
                    val digitsOnly = input.filter { it.isDigit() }
                    if (digitsOnly.length <= 10) phone = digitsOnly
                },
                placeholder = "Ej. 3452867458",
                keyboardType = KeyboardType.Phone
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(SupportContact(name = name.trim(), phone = phone.trim()))
                },
                enabled = canSave,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ZenOrange,
                    contentColor = Color.White,
                    disabledContainerColor = ZenOrange.copy(alpha = 0.4f),
                    disabledContentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Guardar contacto",
                    fontFamily = NunitoBold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = ZenLabel,
        fontFamily = Livvic,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
}

@Composable
private fun ZenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = ZenLabel.copy(alpha = 0.5f),
                fontFamily = Nunito,
                fontSize = 15.sp
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZenOrange,
            unfocusedBorderColor = ZenSage,
            focusedContainerColor = ZenSageLight.copy(alpha = 0.4f),
            unfocusedContainerColor = ZenSageLight.copy(alpha = 0.4f),
            cursorColor = ZenOrange,
            focusedTextColor = ZenLabel,
            unfocusedTextColor = ZenLabel
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
