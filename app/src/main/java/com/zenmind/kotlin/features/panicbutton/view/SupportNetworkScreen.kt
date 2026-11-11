package com.zenmind.kotlin.features.panicbutton.view

import androidx.compose.material3.Button
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenmind.kotlin.features.panicbutton.model.Helpline
import com.zenmind.kotlin.features.panicbutton.model.SupportContact
import com.zenmind.kotlin.features.panicbutton.viewmodel.ContactViewModel
import com.zenmind.kotlin.features.panicbutton.viewmodel.ContactViewModelFactory
import com.zenmind.kotlin.ui.theme.Livvic
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSage
import com.zenmind.kotlin.ui.theme.ZenSageLight

// Colores que no viven en el tema compartido
private val MessageBg = Color(0xFFFAF4D9)
private val MessageBorder = Color(0xFFE6DDB4)
private val ContactCallIcon = Color(0xFF725A48)
private val Background = Color(0xFFFFF9E2)

@Composable
fun SupportNetworkScreen(
    contacts: List<SupportContact> = emptyList(),
    helplines: List<Helpline> = defaultHelplines(),
    onBack: () -> Unit = {},
    onAddContact: () -> Unit = {},
    onCall: (String) -> Unit = {},
    onMessage: (String) -> Unit = {}
) {
    val viewModel: ContactViewModel = viewModel(
        factory = ContactViewModelFactory(LocalContext.current)
    )
    val context = LocalContext.current

    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val contacts by viewModel.contacts.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.call.collect { number ->
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onBack)
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
                text = "R E D   D E   A P O Y O",
                color = ZenLabel,
                fontFamily = ShortStack,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Recursos de apoyo inmediato cuando más lo necesitas.",
                color = ZenLabel.copy(alpha = 0.7f),
                fontFamily = Livvic,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Tus contactos de confianza",
                color = ZenLabel,
                fontFamily = Livvic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))

            contacts.forEach { contact ->
                ContactCard(
                    contact = contact,
                    onCall = { viewModel.onEmergencyPress(contact) },
                    onMessage = {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phone}")).apply {
                            putExtra("sms_body", "Hola, necesito apoyo en este momento.")
                        }
                        context.startActivity(intent)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            AddContactButton(onClick = onAddContact)

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(24.dp))

            // Líneas de ayuda profesional
            Text(
                text = "Líneas de ayuda profesional",
                color = ZenLabel,
                fontFamily = Livvic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))

            helplines.forEach { helpline ->
                HelplineCard(
                    helpline = helpline,
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${helpline.phone}"))
                        context.startActivity(intent)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ContactCard(
    contact: SupportContact,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ZenSageLight)
            .border(2.dp, ZenSage, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                color = ZenLabel,
                fontFamily = NunitoBold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = contact.phone,
                color = ZenLabel.copy(alpha = 0.7f),
                fontFamily = Nunito,
                fontSize = 13.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Enviar mensaje a ${contact.name}",
                background = MessageBg,
                borderColor = MessageBorder,
                borderWidth = 1.dp,
                iconTint = ZenLabel,
                onClick = onMessage
            )
            ActionButton(
                icon = Icons.Outlined.Call,
                contentDescription = "Llamar a ${contact.name}",
                background = ZenSage.copy(alpha = 0.87f),
                borderColor = ZenSage,
                borderWidth = 2.dp,
                iconTint = ContactCallIcon,
                onClick = onCall
            )
        }
    }
}

@Composable
private fun HelplineCard(
    helpline: Helpline,
    onCall: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZenPeach)
            .border(2.dp, ZenOrange, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = helpline.name,
                color = ZenLabel,
                fontFamily = NunitoBold,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = helpline.phone,
                color = ZenLabel.copy(alpha = 0.7f),
                fontFamily = Nunito,
                fontSize = 12.sp
            )
        }

        ActionButton(
            icon = Icons.Outlined.Call,
            contentDescription = "Llamar a ${helpline.name}",
            background = ZenOrange,
            borderColor = ZenOrange,
            borderWidth = 0.dp,
            iconTint = Color.White,
            onClick = onCall
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    background: Color,
    borderColor: Color,
    borderWidth: androidx.compose.ui.unit.Dp,
    iconTint: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .then(
                if (borderWidth.value > 0f) {
                    Modifier.border(borderWidth, borderColor, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AddContactButton(onClick: () -> Unit) {
    val cornerRadius = 16.dp
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                val stroke = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f), 0f
                    )
                )
                drawRoundRect(
                    color = ZenLabel,
                    style = stroke,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = ZenLabel,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Añadir contacto",
            color = ZenLabel,
            fontFamily = NunitoBold,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(ZenOrange.copy(alpha = 0.1f))
    )
}

/** Líneas de ayuda por defecto mostradas en el diseño. */
fun defaultHelplines(): List<Helpline> = listOf(
    Helpline("Emergencias Nacional Médicas y Psicológicas", "123"),
    Helpline("Línea Apoyo Emocional", "106"),
    Helpline("Prevención Suicidio", "018000912342")
)
