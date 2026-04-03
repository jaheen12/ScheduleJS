package com.schedulejs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import com.schedulejs.services.ScheduleAutomationCoordinator
import com.schedulejs.ui.ScheduleJsApp
import com.schedulejs.ui.theme.ScheduleJsTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScheduleJsTheme {
                ScheduleJsApp()
            }
        }
        window.decorView.post {
            ScheduleAutomationCoordinator.initialize(applicationContext)
        }
    }
}
