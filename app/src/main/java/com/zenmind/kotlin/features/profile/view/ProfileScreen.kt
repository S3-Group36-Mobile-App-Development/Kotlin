package com.zenmind.kotlin.features.profile.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
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
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenPeach
import com.zenmind.kotlin.ui.theme.ZenSageLight
import com.zenmind.kotlin.ui.theme.ZenText
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    userId: String?,
    offlineSession: Boolean,
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onVerify: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionUnverified = userId != null && (offlineSession || !viewModel.isSessionVerified())
    BackHandler(enabled = sessionUnverified) { onBack() }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(userId) { viewModel.load(userId) }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(containerColor = ZenCream, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().background(ZenCream).padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ZenTopBar("Perfil", onBack)
            Spacer(Modifier.height(34.dp))

            if (userId == null) {
                ProfileIdentity("I", "Invitado", "Tu progreso solo se guarda en este teléfono")
                state.guestStreak?.let {
                    Spacer(Modifier.height(28.dp))
                    StreakCard(it.current, it.longest)
                }
                Spacer(Modifier.height(24.dp))
                GuestAccountCard()
                Spacer(Modifier.height(24.dp))
                ProfileAction("INICIAR SESIÓN", onLogin)
            } else if (state.isLoading) {
                CircularProgressIndicator(color = ZenOrange)
            } else {
                val profile = state.profile
                if (profile == null) {
                    Text("Aún no hay un perfil guardado. Puedes descargarlo cuando tengas conexión.",
                        fontFamily = Nunito, color = ZenText, textAlign = TextAlign.Center)
                } else {
                    val joined = remember(profile.createdAt) { joinedDate(profile.createdAt) }
                    ProfileIdentity(
                        initial = profile.name.trim().firstOrNull()?.uppercase() ?: "?",
                        name = profile.name,
                        subtitle = profile.email,
                        joined = joined
                    )
                    Spacer(Modifier.height(28.dp))
                    StreakCard(profile.currentStreak, profile.bestStreak)
                    Spacer(Modifier.height(24.dp))
                    InformationCard(
                        name = profile.name,
                        institution = state.institutions.find { it.id == profile.institutionId }?.name
                            ?: if (profile.institutionId == null) "Sin institución" else "No disponible",
                        career = state.departments.find { it.id == profile.departmentId }?.name
                            ?: if (profile.departmentId == null) "Sin carrera" else "No disponible",
                        onEdit = onEdit
                    )
                    state.pending?.takeIf { it.hasChanges }?.let { pending ->
                        Spacer(Modifier.height(20.dp))
                        Column(Modifier.fillMaxWidth()) {
                            Text("Cambios guardados en este teléfono, pendientes de sincronización.",
                                fontFamily = NunitoBold, color = ZenBrown)
                            pending.pendingName?.let { Text("Nombre pendiente: $it", fontFamily = Nunito, color = ZenText) }
                            if (pending.pendingInstitutionId != null) {
                                val institution = state.institutions.find { it.id == pending.pendingInstitutionId }?.name
                                    ?: "Pendiente de sincronizar"
                                val career = pending.pendingDepartmentId?.let { id ->
                                    state.pendingDepartments.find { it.id == id }?.name ?: "Pendiente de sincronizar"
                                } ?: "Sin carrera"
                                Text("Institución pendiente: $institution", fontFamily = Nunito, color = ZenText)
                                Text("Carrera pendiente: $career", fontFamily = Nunito, color = ZenText)
                            }
                            OutlinedButton(onClick = viewModel::retryPending,
                                enabled = !state.isSaving && state.isOnline && !sessionUnverified,
                                modifier = Modifier.fillMaxWidth()) {
                                Text("Reintentar sincronización", color = ZenText)
                            }
                        }
                    }
                }
                if (sessionUnverified) {
                    Spacer(Modifier.height(18.dp))
                    Text("Sesión local sin verificar. Tus datos se muestran desde este teléfono.",
                        fontFamily = NunitoBold, color = ZenBrown, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = onVerify, enabled = state.isOnline, modifier = Modifier.fillMaxWidth()) {
                        Text("Verificar sesión", color = ZenText)
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = viewModel::refresh, enabled = !state.isRefreshing && !state.isSaving,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isRefreshing) "Actualizando..." else "Actualizar perfil", color = ZenText)
                }
                Spacer(Modifier.height(26.dp))
                ProfileAction("CERRAR SESIÓN", onLogout)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ProfileIdentity(initial: String, name: String, subtitle: String, joined: String? = null) {
    Box(Modifier.size(96.dp).clip(CircleShape).background(ZenOrange.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center) {
        Text(initial, fontFamily = ShortStack, fontSize = 38.sp, color = Color.White)
    }
    Spacer(Modifier.height(18.dp))
    Text(name, fontFamily = ShortStack, fontSize = 25.sp, color = ZenText, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, fontFamily = Nunito, fontSize = 15.sp, color = ZenBrown, textAlign = TextAlign.Center)
    joined?.let {
        Spacer(Modifier.height(8.dp))
        Text("En ZenMind desde $it", fontFamily = Nunito, fontSize = 14.sp,
            color = ZenLabel, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StreakCard(current: Int?, best: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZenPeach)
            .padding(horizontal = 14.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreakValue(Icons.Filled.LocalFireDepartment, current, "Racha actual", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(58.dp).background(ZenOrange.copy(alpha = 0.45f)))
        StreakValue(Icons.Outlined.EmojiEvents, best, "Mejor racha", Modifier.weight(1f))
    }
}

@Composable
private fun StreakValue(icon: ImageVector, value: Int?, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = ZenOrange, modifier = Modifier.size(26.dp))
            Text(value?.toString() ?: "—", fontFamily = ShortStack, fontSize = 28.sp, color = ZenText)
        }
        Spacer(Modifier.height(8.dp))
        Text("$label · ${if (value == 1) "día" else "días"}", fontFamily = Nunito,
            fontSize = 12.sp, color = ZenText, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InformationCard(name: String, institution: String, career: String, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZenSageLight)
        .padding(horizontal = 20.dp, vertical = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("INFORMACIÓN", fontFamily = ShortStack, fontSize = 15.sp,
                letterSpacing = 3.sp, color = ZenText)
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Editar perfil", tint = ZenOrange)
            }
        }
        Spacer(Modifier.height(14.dp))
        InformationRow(Icons.Outlined.PersonOutline, "Nombre visible", name)
        Spacer(Modifier.height(18.dp))
        InformationRow(Icons.Outlined.AccountBalance, "Institución", institution)
        Spacer(Modifier.height(18.dp))
        InformationRow(Icons.Outlined.School, "Carrera", career)
    }
}

@Composable
private fun InformationRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ZenOrange, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, fontFamily = Nunito, fontSize = 13.sp, color = ZenLabel)
            Text(value, fontFamily = Nunito, fontSize = 16.sp, color = ZenText)
        }
    }
}

@Composable
private fun GuestAccountCard() {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZenSageLight)
        .padding(22.dp)) {
        Text("CREA TU CUENTA", fontFamily = ShortStack, fontSize = 15.sp,
            letterSpacing = 3.sp, color = ZenText)
        Spacer(Modifier.height(12.dp))
        Text("Con una cuenta puedes guardar tu progreso en la nube, elegir tu carrera y conservar tu racha si cambias de teléfono.",
            fontFamily = Nunito, fontSize = 15.sp, color = ZenText)
    }
}

@Composable
private fun ProfileAction(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ZenOrange, contentColor = ZenText)) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, fontFamily = ShortStack, fontSize = 14.sp, letterSpacing = 3.sp)
    }
}

private fun joinedDate(isoDate: String): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate.take(10)) ?: return isoDate.take(10)
    return SimpleDateFormat("MMMM 'de' yyyy", Locale.forLanguageTag("es-CO")).format(date)
}
