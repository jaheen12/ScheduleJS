package com.schedulejs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schedulejs.data.local.ScheduleDatabase
import com.schedulejs.data.repository.NightlyChecklistRepository
import com.schedulejs.ui.theme.ScheduleJsTheme
import kotlinx.coroutines.launch

class NightChecklistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = NightlyChecklistRepository(
            ScheduleDatabase.getInstance(applicationContext).nightlyChecklistDao()
        )
        setContent {
            ScheduleJsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NightChecklistScreen(
                        repository = repository,
                        onComplete = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun NightChecklistScreen(
    repository: NightlyChecklistRepository,
    onComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var checklistDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var clothes by remember { mutableStateOf(false) }
    var lunch by remember { mutableStateOf(false) }
    var bag by remember { mutableStateOf(false) }
    val canFinish = clothes && lunch && bag

    LaunchedEffect(Unit) {
        val state = repository.getForDate()
        checklistDate = state.date
        clothes = state.clothesLaidOut
        lunch = state.lunchPrepped
        bag = state.bagPacked
    }

    fun updateState(
        clothesLaidOut: Boolean = clothes,
        lunchPrepped: Boolean = lunch,
        bagPacked: Boolean = bag
    ) {
        val date = checklistDate ?: return
        coroutineScope.launch {
            repository.update(
                date = date,
                clothesLaidOut = clothesLaidOut,
                lunchPrepped = lunchPrepped,
                bagPacked = bagPacked
            )
        }
    }

    BackHandler(enabled = !canFinish) {
        // Keep the checklist visible until all items are checked.
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = stringResource(R.string.night_checklist_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.night_checklist_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ChecklistRow(
            checked = clothes,
            label = stringResource(R.string.night_checklist_clothes),
            onCheckedChange = {
                clothes = it
                updateState(clothesLaidOut = it)
            }
        )
        ChecklistRow(
            checked = lunch,
            label = stringResource(R.string.night_checklist_lunch),
            onCheckedChange = {
                lunch = it
                updateState(lunchPrepped = it)
            }
        )
        ChecklistRow(
            checked = bag,
            label = stringResource(R.string.night_checklist_bag),
            onCheckedChange = {
                bag = it
                updateState(bagPacked = it)
            }
        )

        Button(
            onClick = onComplete,
            enabled = canFinish,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(stringResource(R.string.night_checklist_complete))
        }
    }
}

@Composable
private fun ChecklistRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
