package com.zenmind.kotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zenmind.kotlin.navigation.ZenNavGraph
import com.zenmind.kotlin.ui.theme.ZenmindKotlinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZenmindKotlinTheme {
                ZenNavGraph()
            }
        }
    }
}