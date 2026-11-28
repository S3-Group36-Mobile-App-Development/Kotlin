package com.zenmind.kotlin.features.profile.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenmind.kotlin.features.profile.viewmodel.ProfileViewModel
import com.zenmind.kotlin.ui.components.ZenTopBar
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenBrown
import com.zenmind.kotlin.ui.theme.ZenCream
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenSageLight
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun EditProfileScreen(userId: String?, viewModel: ProfileViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var initialized by remember(userId) { mutableStateOf(false) }
    LaunchedEffect(userId) { viewModel.load(userId) }
    LaunchedEffect(userId, state.profile?.userId) {
        if (!initialized && userId != null && state.profile?.userId == userId) {
            viewModel.openEditor()
            initialized = true
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    val cancel = {
        if (!state.isSaving) {
            viewModel.cancelEdit()
            onBack()
        }
    }
    BackHandler { cancel() }

    Scaffold(containerColor = ZenCream, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(ZenCream).padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZenTopBar("Editar perfil", cancel)
            Spacer(Modifier.height(20.dp))
            if (userId == null || state.profile == null) {
                Text("El perfil no está disponible.", fontFamily = Nunito, color = ZenText)
            } else {
                if (state.pending?.hasChanges == true) {
                    Text("Hay cambios guardados en este teléfono pendientes de sincronización.",
                        fontFamily = Nunito, color = ZenText)
                }
                Column(Modifier.fillMaxWidth().background(ZenSageLight, RoundedCornerShape(20.dp))
                    .padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("INFORMACIÓN", fontFamily = ShortStack, color = ZenText, letterSpacing = 3.sp)
                    OutlinedTextField(
                        value = state.name, onValueChange = viewModel::changeName,
                        label = { Text("Nombre visible") },
                        supportingText = { Text("${state.name.length}/50") },
                        textStyle = TextStyle(fontFamily = Nunito, fontSize = 16.sp, color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black, cursorColor = Color.Black,
                            focusedBorderColor = ZenOrange, unfocusedBorderColor = ZenBrown,
                            focusedLabelColor = ZenText, unfocusedLabelColor = ZenText
                        ),
                        singleLine = true, enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                    ProfileSelector(
                        label = "Institución",
                        selected = state.institutions.find { it.id == state.institutionId }?.name ?: "Selecciona una institución",
                        options = state.institutions.map { it.id to it.name },
                        enabled = !state.isSaving && state.institutions.isNotEmpty(),
                        onSelect = viewModel::selectInstitution
                    )
                    ProfileSelector(
                        label = "Carrera",
                        selected = state.departments.find { it.id == state.departmentId }?.name ?: "Sin carrera",
                        options = listOf(null to "Sin carrera") + state.departments.map { it.id to it.name },
                        enabled = !state.isSaving && state.institutionId != null,
                        onSelect = viewModel::selectDepartment
                    )
                }
                Button(
                    onClick = { viewModel.save(onBack) }, enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZenOrange)
                ) {
                    Text(if (state.isSaving) "Guardando..." else "GUARDAR", color = ZenText, fontFamily = NunitoBold)
                }
                OutlinedButton(onClick = cancel, enabled = !state.isSaving,
                    shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, ZenOrange),
                    modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("CANCELAR", color = ZenText, fontFamily = NunitoBold)
                }
            }
        }
    }
}

@Composable
private fun ProfileSelector(
    label: String,
    selected: String,
    options: List<Pair<String?, String>>,
    enabled: Boolean,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column {
            Text(label, fontFamily = Nunito, color = ZenText)
            OutlinedButton(onClick = { expanded = true }, enabled = enabled,
                border = BorderStroke(1.dp, ZenOrange), shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()) {
                Text(selected, color = ZenText)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    expanded = false
                    onSelect(id)
                })
            }
        }
    }
}
