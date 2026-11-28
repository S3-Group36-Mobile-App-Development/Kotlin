package com.zenmind.kotlin.features.protocols.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.ZenBrown
import com.zenmind.kotlin.ui.theme.ZenCream

@Composable
fun ProtocolOfflineBanner(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(ZenBrown, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = ZenCream, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = Nunito, fontSize = 13.sp, color = ZenCream)
    }
}

@Composable
fun ProtocolSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            containerColor = ZenBrown,
            contentColor = ZenCream,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(data.visuals.message, fontFamily = Nunito, fontSize = 13.sp)
        }
    }
}
