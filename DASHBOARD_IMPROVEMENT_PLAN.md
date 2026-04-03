# Dashboard Improvement Plan

> **Screen:** `DashboardScreen` · `screens/DashboardScreen.kt`
> **Blueprint reference:** Section 3 – Screen 1: The Dashboard (Home)

---

## Status Overview

| Area | Status | Notes |
|---|---|---|
| HUD header rebuild | ✅ Done | Gradient card, LIVE pill, date + day type |
| Animated progress bar | ✅ Done | `animateFloatAsState` 1.2 s ease-in |
| Timeline rail | ✅ Done | Dot + connector line, state-based colours |
| Current block highlight | ✅ Done | `primaryContainer` card with bold title |
| Pulsing dot animation | ✅ Done | `infiniteRepeatable` alpha for CURRENT row |
| `dateLabel` demo fix | ✅ Done | Was missing from `DemoData.dashboard` |
| `dayType` field | ✅ Done | Added to `DashboardUiState` + demo data |
| Live countdown (HUD) | ⬜ Planned | Phase 3 — tick every second from `System.currentTimeMillis` |
| Tap-to-expand timeline row | ⬜ Planned | `AnimatedVisibility` detail dropdown |
| Category icon chips | ⬜ Planned | Per-task emoji/icon from category string |
| Past tasks auto-scroll | ⬜ Planned | `LazyListState.animateScrollToItem` on screen open |

---

## Completed Changes (Phase 1 UI Shell)

### 1. HUD Header (`HudHeader`)

**Before:** Generic `ScreenFrame` title "Dashboard" with a dev-note subtitle. No visual hierarchy.

**After:**
- Full-width `verticalGradient` banner: `primaryContainer@85% → background`
- Top row: `dateLabel` + `dayType` on the left; pulsing **LIVE** pill on the right
- Pulsing dot uses `infiniteRepeatable` alpha oscillation (0.45 → 1.0, 950 ms)

```
┌─────────────────────────────────────────────┐
│  Thursday, April 3                 ● LIVE   │  ← gradient fades to bg
│  Class Day                                  │
│ ┌─────────────────────────────────────────┐ │
│ │ CURRENT BLOCK                           │ │  ← CurrentBlockCard
│ │ Office                    14:00–17:40   │ │
│ │ ████████████░░░░░░░   Remaining: 1h 15m │ │  ← animated bar
│ │ NEXT → Transit to Home · 17:40          │ │  ← next-up chip
│ └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

---

### 2. Current Block Card (`CurrentBlockCard`)

- `linearGradient` fill: `primary → primary@75%`
- **ExtraBold** headline for task name; time range right-aligned at baseline
- Progress bar: white bar on `onPrimary@20%` track, width animated via `animateFloatAsState`
- `${progress}%` label right-aligned; subtitle (remaining time) left-aligned
- **NEXT →** chip: `onPrimary@15%` pill — `next.title · next.timeLabel`

---

### 3. Timeline Rail (`TimelineRow`)

**Before:** Flat coloured `Row` per state. Time label and title competed for space.

**After:** True vertical rail layout:

```
  ●   Wake + Belly Routine          06:30   ← PAST  (42 % alpha)
  │
  ●   Morning Study             07:30–08:30  ← PAST
  │
  ◉  ┌─── Office ──────────── 14:00–17:40 ─┐ ← CURRENT (primaryContainer card)
  │  └────────────────────────────────────── ┘   pulsing dot
  │
  ○   Transit to Home              17:40    ← UPCOMING (tertiary dot)
```

- **Dot sizes:** CURRENT = 12 dp (alpha pulse), others = 8 dp
- **Connector:** 2 dp wide, `outline@30%`, fills rail height via `Modifier.weight(1f)`
- **PAST rows:** 42 % alpha on text and dot — visually recede
- Uses `IntrinsicSize.Min` on outer `Row` so the rail column can `fillMaxHeight()`

---

### 4. Data Model Fixes

| File | Change |
|---|---|
| `ui/UiState.kt` | Added `dayType: String = ""` to `DashboardUiState` |
| `ui/DemoData.kt` | Added `dateLabel = "Thursday, April 3"` and `dayType = "Class Day"` |

---

## Planned Improvements

### Phase 2 — Live Countdown

> **Goal:** HUD remaining-time text updates every second using real device time.

```kotlin
// ScheduleJsViewModel
private val ticker = flow {
    while (true) { emit(System.currentTimeMillis()); delay(1000L) }
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0L)

val dashboardState = ticker.map { now ->
    scheduleRepository.resolveCurrentState(now)   // computes progress + subtitle
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), DemoData.dashboard)
```

- `resolveCurrentState(now)` compares `now` to task start/end epoch millis
- Progress = `(now - start) / (end - start).toFloat()`
- `currentTask.subtitle` → *"Remaining: 1h 14m"* recomputed each tick

---

### Phase 3 — Tap-to-Expand Timeline Row

> **Goal:** Tap a row to reveal its detail string (matches Blueprint spec).

```kotlin
var expanded by remember { mutableStateOf(isCurrent) }

Column(modifier = Modifier.clickable { expanded = !expanded }) {
    // title + time row always visible

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit  = shrinkVertically() + fadeOut()
    ) {
        Text(text = item.detail, style = MaterialTheme.typography.bodySmall)
    }
}
```

- CURRENT row starts expanded; all others start collapsed
- Collapsed state shows title + time only — cleaner at a glance

---

### Phase 4 — Category Icon Chips

> **Goal:** Small icon left of each timeline row title to aid visual scanning.

```kotlin
// Add to TimelineItem data class:
val category: String  // "Study", "Workout", "Transit", "Office", "Routine"

fun categoryIcon(category: String) = when (category) {
    "Study"   -> "📚"
    "Workout" -> "💪"
    "Transit" -> "🚲"
    "Office"  -> "💼"
    "Routine" -> "🌅"
    else      -> "📌"
}
```

---

### Phase 5 — Auto-Scroll to Current Item

> **Goal:** On open, scroll so the CURRENT row is near the top of the visible area.

```kotlin
val listState = rememberLazyListState()
val currentIndex = state.timelineItems.indexOfFirst {
    it.state == TimelineItemState.CURRENT
}
LaunchedEffect(currentIndex) {
    if (currentIndex >= 0)
        listState.animateScrollToItem(index = currentIndex + 1, scrollOffset = -80)
}
LazyColumn(state = listState) { ... }
```

Requires migrating `forEachIndexed` inside proper `LazyColumn` `items { }` blocks.

---

## Design Token Reference

| Token | Light | Dark | Usage |
|---|---|---|---|
| `primary` | `BlueSteel #1C4E80` | `Sun #F4C95D` | Card gradient, progress fill, CURRENT dot |
| `primaryContainer` | `Skywash #D8E7F5` | `BlueNight #21476D` | Header gradient, CURRENT row background |
| `onPrimary` | `IvoryMist` | `Night` | Card text, progress bar, chip text |
| `tertiary` | `Olive #7F8C3A` | `Mint #B8D889` | UPCOMING dot colour |
| `outline` | `SlateSoft` | `SlateSoft` | PAST dot, connector line |
| `onSurfaceVariant` | `Slate` | `Fog` | Detail text, section header label |

---

## Files Changed

| File | Change |
|---|---|
| `ui/screens/DashboardScreen.kt` | Full rewrite — HUD header, current block card, timeline rail |
| `ui/UiState.kt` | Added `dayType: String = ""` field |
| `ui/DemoData.kt` | Added `dateLabel` and `dayType` to demo dashboard state |
| `ui/screens/Common.kt` | No changes — `ScreenFrame` is intentionally bypassed by Dashboard |
