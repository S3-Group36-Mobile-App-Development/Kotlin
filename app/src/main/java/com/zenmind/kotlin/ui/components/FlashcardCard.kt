package com.zenmind.kotlin.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenmind.kotlin.ui.theme.Nunito
import com.zenmind.kotlin.ui.theme.NunitoBold
import com.zenmind.kotlin.ui.theme.ShortStack
import com.zenmind.kotlin.ui.theme.ZenFlashcard
import com.zenmind.kotlin.ui.theme.ZenLabel
import com.zenmind.kotlin.ui.theme.ZenOrange
import com.zenmind.kotlin.ui.theme.ZenShadow
import com.zenmind.kotlin.ui.theme.ZenText

@Composable
fun FlashcardCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    source: String? = null
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .dropShadow(
                shape = shape,
                shadow = Shadow(radius = 0.dp, color = ZenShadow, offset = DpOffset(0.dp, 4.dp))
            )
            .background(ZenFlashcard, shape)
            .border(2.dp, ZenOrange, shape)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = title.uppercase(),
            fontFamily = ShortStack,
            fontSize = 14.sp,
            letterSpacing = 6.sp,
            color = ZenText
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = content,
            fontFamily = Nunito,
            fontSize = 13.sp,
            color = ZenLabel,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        source?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                it,
                fontFamily = NunitoBold,
                fontSize = 13.sp,
                color = ZenText,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
