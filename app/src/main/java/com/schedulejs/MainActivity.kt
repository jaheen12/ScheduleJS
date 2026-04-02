package com.schedulejs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.schedulejs.ui.ScheduleJsApp
import com.schedulejs.ui.theme.ScheduleJsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScheduleJsTheme {
                ScheduleJsApp()
            }
        }
    }
}
