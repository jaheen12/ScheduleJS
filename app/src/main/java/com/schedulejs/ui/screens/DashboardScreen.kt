package com.schedulejs.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedulejs.ui.DashboardUiState
import com.schedulejs.ui.TimelineItem
import com.schedulejs.ui.TimelineItemState

@Composable
fun DashboardScreen(state: DashboardUiState) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progressPercent,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "blockProgress"
    )
    val listState = rememberLazyListState()
    val currentIndex = state.timelineItems.indexOfFirst { it.state == TimelineItemState.CURRENT }

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            // +2 accounts for header and section-title items in this LazyColumn.
            listState.animateScrollToItem(index = currentIndex + 2, scrollOffset = -80)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
        state = listState
    ) {
        item {
            HudHeader(state = state, animatedProgress = animatedProgress)
        }
        item {
            Column(
                modifier = Modifier.padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 8.dp
                )
            ) {
                Text(
                    text = "TODAY'S SCHEDULE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
        itemsIndexed(state.timelineItems) { index, item ->
            TimelineRow(
                item = item,
                isLast = index == state.timelineItems.lastIndex,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun HudHeader(state: DashboardUiState, animatedProgress: Float) {
    val pulse = rememberInfiniteTransition(label = "hud_pulse")
    val livePulse by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDot"
    )

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
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.dayType.isNotBlank()) {
                        Text(
                            text = state.dayType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = livePulse))
                    )
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            CurrentBlockCard(
                state = state,
                animatedProgress = animatedProgress
            )
        }
    }
}

@Composable
private fun CurrentBlockCard(state: DashboardUiState, animatedProgress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "CURRENT BLOCK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = state.currentTask.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = state.currentTask.timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.currentTask.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${(state.progressPercent * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "NEXT ->",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${state.nextTask.title} - ${state.nextTask.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    item: TimelineItem,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val isCurrent = item.state == TimelineItemState.CURRENT
    val isPast = item.state == TimelineItemState.PAST
    var expanded by rememberSaveable(item.title, item.timeLabel) { mutableStateOf(isCurrent) }

    LaunchedEffect(isCurrent) {
        if (isCurrent) expanded = true
    }

    val dotColor = when (item.state) {
        TimelineItemState.PAST -> MaterialTheme.colorScheme.outline
        TimelineItemState.CURRENT -> MaterialTheme.colorScheme.primary
        TimelineItemState.UPCOMING -> MaterialTheme.colorScheme.tertiary
    }
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val contentAlpha = if (isPast) 0.42f else 1f

    val pulse = rememberInfiniteTransition(label = "dot_${item.title}")
    val dotAlpha by pulse.animateFloat(
        initialValue = if (isCurrent) 0.55f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dotSize = if (isCurrent) 12.dp else 8.dp
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isCurrent) dotAlpha else contentAlpha))
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (isCurrent) {
                        Modifier
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp)
                    } else {
                        Modifier.padding(start = 4.dp, bottom = 14.dp)
                    }
                )
                .clickable { expanded = !expanded },
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = categoryIcon(item.category),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = when (item.state) {
                            TimelineItemState.CURRENT -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha)
                        }
                    )
                }
                Text(
                    text = item.timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = dotColor.copy(alpha = contentAlpha)
                )
            }

            AnimatedVisibility(
                visible = expanded && item.detail.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = item.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (item.state) {
                        TimelineItemState.CURRENT ->
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        else ->
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                    }
                )
            }
        }
    }
}

private fun categoryIcon(category: String): String {
    return when (category.lowercase()) {
        "study" -> "\uD83D\uDCDA"
        "workout" -> "\uD83D\uDCAA"
        "transit" -> "\uD83D\uDEB2"
        "college" -> "\uD83C\uDF93"
        "office" -> "\uD83D\uDCBC"
        "tuition" -> "\uD83D\uDCD6"
        "meal" -> "\uD83C\uDF7D"
        "prep" -> "\uD83D\uDCCB"
        "review" -> "\u270D"
        "rest" -> "\uD83D\uDECC"
        "personal" -> "\uD83D\uDE4B"
        "sleep" -> "\uD83D\uDE34"
        "routine" -> "\uD83C\uDF05"
        else -> "\uD83D\uDCCC"
    }
}
