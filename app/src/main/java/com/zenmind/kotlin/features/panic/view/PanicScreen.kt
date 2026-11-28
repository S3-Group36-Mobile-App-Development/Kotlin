package com.zenmind.kotlin.features.panic.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenmind.kotlin.R
import com.zenmind.kotlin.features.panic.model.EmergencyContact
import com.zenmind.kotlin.features.panic.model.PanicUiState
import com.zenmind.kotlin.features.panic.viewmodel.PanicViewModel
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenBrown
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenIcon
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenMuted
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSubtle
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun PanicScreen(
    viewModel: PanicViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.onMessageShown()
        }
    }

    fun call(phone: String) {
        viewModel.onCall()
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        } catch (e: ActivityNotFoundException) {
            viewModel.onCallFailed()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ZenCream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            ZenTopBar(title = "Pánico", onBack = onBack)
            Spacer(Modifier.height(12.dp))
            if (!state.isLoading && state.contact != null && !editing) {
                Text(
                    "Alguien a quien llamar cuando lo necesites.",
                    fontFamily = Nunito,
                    fontSize = 13.sp,
                    color = ZenSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))

            if (state.isOffline) {
                PanicOfflineBanner()
                Spacer(Modifier.height(16.dp))
            }

            val contact = state.contact
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
            when {
                state.isLoading -> CircularProgressIndicator(color = ZenOrange)
                contact == null || editing -> ContactForm(
                    state = state,
                    initial = contact,
                    showCancel = contact != null,
                    onSave = { nombre, telefono, relacion ->
                        viewModel.saveContact(nombre, telefono, relacion, onDone = { editing = false })
                    },
                    onCancel = {
                        viewModel.onFormShown()
                        editing = false
                    }
                )
                else -> ContactView(
                    contact = contact,
                    onCall = { call(contact.telefono) }
                )
            }
            }

            if (!state.isLoading && contact != null && !editing) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ActionBox(Icons.Outlined.Edit, "Editar") {
                        viewModel.onFormShown()
                        editing = true
                    }
                    Spacer(Modifier.width(12.dp))
                    ActionBox(Icons.Outlined.Delete, "Eliminar") { confirmDelete = true }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) { data ->
            Snackbar(
                containerColor = ZenBrown,
                contentColor = ZenCream,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text(data.visuals.message, fontFamily = Nunito, fontSize = 13.sp)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.padding(horizontal = 40.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = ZenCream,
            title = { Text("Eliminar contacto", fontFamily = NunitoBold, color = ZenText) },
            text = { Text("¿Quieres quitar a esta persona de tu botón de pánico?", fontFamily = Nunito, color = ZenText) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteContact()
                }) { Text("Eliminar", fontFamily = NunitoBold, color = ZenOrange) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", fontFamily = Nunito, color = ZenLabel) }
            }
        )
    }
}

@Composable
private fun ContactView(
    contact: EmergencyContact,
    onCall: () -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(ZenOrange)
                .clickable(onClick = onCall)
        ) {
            Image(
                painter = painterResource(R.drawable.panic_button),
                contentDescription = "Llamar a ${contact.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "LLAMAR",
                fontFamily = ShortStack,
                fontSize = 16.sp,
                letterSpacing = 6.sp,
                color = ZenIcon
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(contact.nombre, fontFamily = NunitoBold, fontSize = 22.sp, color = ZenLabel)
        contact.relacion?.let {
            Text(it, fontFamily = Nunito, fontSize = 14.sp, color = ZenSubtle)
        }
        Text(contact.telefono, fontFamily = Nunito, fontSize = 15.sp, color = ZenText)
    }
}

@Composable
private fun ActionBox(icon: ImageVector, text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(ZenPeach)
            .border(1.dp, ZenOrange, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = ZenLabel, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontFamily = NunitoBold, fontSize = 14.sp, color = ZenLabel)
    }
}

@Composable
private fun ContactForm(
    state: PanicUiState,
    initial: EmergencyContact?,
    showCancel: Boolean,
    onSave: (String, String, String) -> Unit,
    onCancel: () -> Unit
) {
    var nombre by rememberSaveable { mutableStateOf(initial?.nombre ?: "") }
    var telefono by rememberSaveable { mutableStateOf(initial?.telefono ?: "") }
    var relacion by rememberSaveable { mutableStateOf(initial?.relacion ?: "") }
    val isValid = nombre.isNotBlank() && telefono.isNotBlank()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            if (initial == null) "Tu contacto de emergencia" else "Editar contacto",
            fontFamily = NunitoBold,
            fontSize = 22.sp,
            color = ZenLabel
        )
        Text(
            "Alguien de confianza a quien llamar con un toque cuando lo necesites.",
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = ZenSubtle
        )
        Spacer(Modifier.height(20.dp))

        PanicField("Nombre", nombre, "Ej. Mamá", KeyboardType.Text, MAX_NAME) { nombre = it.take(MAX_NAME) }
        Spacer(Modifier.height(12.dp))
        PanicField("Teléfono", telefono, "Ej. 3001234567", KeyboardType.Phone, MAX_PHONE, telefono.count { it.isDigit() }) { telefono = cleanPhoneInput(it) }
        Spacer(Modifier.height(12.dp))
        PanicField("Relación (opcional)", relacion, "Ej. Familia", KeyboardType.Text, MAX_RELATION) { relacion = it.take(MAX_RELATION) }

        state.formError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontFamily = NunitoBold, fontSize = 13.sp, color = ZenLabel)
        }

        Spacer(Modifier.height(20.dp))
        if (!isValid) {
            Text(
                "Escribe un nombre y un teléfono para guardar",
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
            onClick = { onSave(nombre, telefono, relacion) },
            enabled = isValid && !state.isSaving,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ZenOrange, disabledContainerColor = ZenMuted),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                if (state.isSaving) "Guardando..." else "Guardar contacto",
                fontFamily = ShortStack,
                fontSize = 16.sp,
                color = ZenIcon
            )
        }
        if (showCancel) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancelar", fontFamily = Nunito, fontSize = 14.sp, color = ZenLabel)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PanicField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    maxLength: Int,
    count: Int = value.length,
    onValueChange: (String) -> Unit
) {
    Text(label, fontFamily = NunitoBold, fontSize = 14.sp, color = ZenLabel)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontFamily = Nunito, fontSize = 14.sp) },
        textStyle = TextStyle(fontFamily = Nunito, fontSize = 14.sp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = {
            Text(
                "$count/$maxLength",
                fontFamily = Nunito,
                fontSize = 11.sp,
                color = ZenSubtle,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ZenText,
            unfocusedTextColor = ZenText,
            focusedPlaceholderColor = ZenSubtle,
            unfocusedPlaceholderColor = ZenSubtle,
            cursorColor = ZenOrange,
            focusedContainerColor = ZenPeach,
            unfocusedContainerColor = ZenPeach,
            focusedBorderColor = ZenOrange,
            unfocusedBorderColor = ZenOrange
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PanicOfflineBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ZenBrown, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = ZenCream, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Sin conexión. Puedes llamar igual, la llamada no usa internet.", fontFamily = Nunito, fontSize = 13.sp, color = ZenCream)
    }
}

private const val MAX_NAME = 30
private const val MAX_PHONE = 15
private const val MAX_RELATION = 20

private fun cleanPhoneInput(input: String): String {
    val plus = if (input.trimStart().startsWith("+")) "+" else ""
    return plus + input.filter { it.isDigit() }.take(MAX_PHONE)
}
