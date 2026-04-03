package com.schedulejs.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedulejs.ui.StepType
import com.schedulejs.ui.WorkoutUiState
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onBellyRoutineAction: () -> Unit,
    onCancelBellyRoutine: () -> Unit,
    onToggleWorkoutComplete: () -> Unit,
    onSetChecked: (String, Int) -> Unit,
    onBellyRoutineRepTap: () -> Unit
) {
    var showCelebration by rememberSaveable { mutableStateOf(false) }
    var previousComplete by remember { mutableStateOf(state.isWorkoutComplete) }

    LaunchedEffect(state.isWorkoutComplete) {
        if (state.isWorkoutComplete && !previousComplete) {
            showCelebration = true
            delay(1600)
            showCelebration = false
        }
        previousComplete = state.isWorkoutComplete
    }

    val flashColor by animateColorAsState(
        targetValue = if (showCelebration) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(500),
        label = "workout_completion_flash"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(flashColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WorkoutHeaderCard(state = state)
            }
            if (state.weekDays.isNotEmpty()) {
                item {
                    WeekCalendarStrip(days = state.weekDays)
                }
            }
            item {
                SectionCard("Core Reset") {
                    Text(
                        text = state.bellyRoutineState.statusLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onBellyRoutineAction, modifier = Modifier.fillMaxWidth()) {
                        Text(state.bellyRoutineState.ctaLabel)
                    }
                    state.bellyRoutineState.secondaryCtaLabel?.let { label ->
                        OutlinedButton(onClick = onCancelBellyRoutine, modifier = Modifier.fillMaxWidth()) {
                            Text(label)
                        }
                    }
                }
            }
            item {
                SectionCard("Routine") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.routineItems.forEach { item ->
                            ExerciseCard(
                                item = item,
                                onSetChecked = { setNumber -> onSetChecked(item.id, setNumber) }
                            )
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = showCelebration,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Workout done - streak: ${state.weeklyStreak} days",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            item {
                if (state.isWorkoutComplete) {
                    Button(onClick = onToggleWorkoutComplete, modifier = Modifier.fillMaxWidth()) {
                        Text("Workout Completed")
                    }
                } else {
                    OutlinedButton(onClick = onToggleWorkoutComplete, modifier = Modifier.fillMaxWidth()) {
                        Text("Mark Workout Complete")
                    }
                }
            }
        }

        if (state.bellyRoutineState.isTimerVisible) {
            BellyRoutineOverlay(
                state = state,
                onPrimaryAction = onBellyRoutineAction,
                onCancel = onCancelBellyRoutine,
                onRepTap = onBellyRoutineRepTap
            )
        }
    }
}

@Composable
private fun WorkoutHeaderCard(state: WorkoutUiState) {
    val pulse = rememberInfiniteTransition(label = "workout_header_pulse")
    val activeAlpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "workout_active_pulse"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${muscleGroupEmoji(state.muscleGroup)} ${state.dayOfWeek}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = activeAlpha))
                        )
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                }
                Text(
                    text = state.dayLabel.replace("Today:", "").trim().ifBlank { state.muscleGroup },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.purposeNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun BellyRoutineOverlay(
    state: WorkoutUiState,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit,
    onRepTap: () -> Unit
) {
    val currentStep = state.bellyRoutineState.steps.getOrNull(state.bellyRoutineState.currentStepIndex) ?: return
    val progress = when (currentStep.type) {
        StepType.TIMED -> {
            val total = currentStep.durationSeconds.coerceAtLeast(1)
            ((total - state.bellyRoutineState.secondsRemaining).toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
        StepType.REPS -> {
            val total = currentStep.targetReps.coerceAtLeast(1)
            (state.bellyRoutineState.repsCompleted.toFloat() / total.toFloat()).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Step ${state.bellyRoutineState.currentStepIndex + 1}/${state.bellyRoutineState.steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentStep.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (currentStep.type == StepType.TIMED) {
                    Text(
                        text = formatTimer(state.bellyRoutineState.secondsRemaining),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(
                        text = "${state.bellyRoutineState.repsCompleted}/${currentStep.targetReps} reps",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(currentStep.targetReps) { index ->
                            SetDot(
                                checked = index < state.bellyRoutineState.repsCompleted,
                                onClick = onRepTap
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = onPrimaryAction, modifier = Modifier.weight(1f)) {
                        Text(state.bellyRoutineState.ctaLabel)
                    }
                }
            }
        }
    }
}

private fun muscleGroupEmoji(muscleGroup: String): String {
    return when {
        muscleGroup.contains("Chest", ignoreCase = true) -> "\uD83D\uDCAA"
        muscleGroup.contains("Leg", ignoreCase = true) -> "\uD83E\uDDB5"
        muscleGroup.contains("Shoulder", ignoreCase = true) -> "\uD83E\uDDD8"
        muscleGroup.contains("Back", ignoreCase = true) -> "\uD83D\uDD19"
        muscleGroup.contains("Full Body", ignoreCase = true) -> "\uD83D\uDD25"
        else -> "\uD83C\uDFCB"
    }
}

private fun formatTimer(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mm = safe / 60
    val ss = safe % 60
    return "%02d:%02d".format(mm, ss)
}
