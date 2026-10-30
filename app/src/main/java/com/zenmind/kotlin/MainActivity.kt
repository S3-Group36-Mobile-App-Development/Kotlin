package com.zenmind.kotlin

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zenmind.kotlin.navigation.ZenNavGraph
import com.zenmind.kotlin.ui.theme.ZenmindKotlinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        setContent {
            ZenmindKotlinTheme {
                ZenNavGraph()
            }
        }
    }
}