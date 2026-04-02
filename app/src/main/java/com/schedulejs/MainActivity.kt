package com.schedulejs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.schedulejs.services.ScheduleAutomationCoordinator
import com.schedulejs.ui.ScheduleJsApp
import com.schedulejs.ui.theme.ScheduleJsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScheduleAutomationCoordinator.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            ScheduleJsTheme {
                ScheduleJsApp()
            }
        }
    }
}
