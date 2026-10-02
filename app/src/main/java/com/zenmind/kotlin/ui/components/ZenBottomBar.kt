package com.zenmind.kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenMuted
import com.zenmind.kotlin.ui.theme.ZenPeach

enum class ZenTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Outlined.Home),
    ACTIVITY("Actividad", Icons.Outlined.MonitorHeart),
    PROFILE("Perfil", Icons.Outlined.Person)
}

@Composable
fun ZenBottomBar(
    selected: ZenTab,
    onTabClick: (ZenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ZenPeach)
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ZenTab.entries.forEach { tab ->
            val color = if (tab == selected) ZenLabel else ZenMuted
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabClick(tab) }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(26.dp))
                Text(
                    tab.label,
                    color = color,
                    fontFamily = Nunito,
                    fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                )
            }
        }
    }
}
