package com.nicholasbergesen.gunsout.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nicholasbergesen.gunsout.data.entity.ProgramDay
import com.nicholasbergesen.gunsout.ui.components.ActionRow
import com.nicholasbergesen.gunsout.ui.components.AccentText
import com.nicholasbergesen.gunsout.ui.components.BigValue
import com.nicholasbergesen.gunsout.ui.components.ChipButton
import com.nicholasbergesen.gunsout.ui.components.DividerLine
import com.nicholasbergesen.gunsout.ui.components.MockupScreenColumn
import com.nicholasbergesen.gunsout.ui.components.ScreenTitle
import com.nicholasbergesen.gunsout.ui.components.SectionLabel
import com.nicholasbergesen.gunsout.ui.components.StatusChip
import com.nicholasbergesen.gunsout.ui.components.ThemedCard

@Composable
fun TodayScreen(
    onStartSession: (Long) -> Unit,
    onOpenHistory: () -> Unit = {},
    vm: TodayViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val scroll = rememberScrollState()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var dayPickerOpen by remember { mutableStateOf(false) }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MockupScreenColumn(modifier = Modifier.verticalScroll(scroll)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ScreenTitle("Today")
            StatusChip("N", selected = true)
        }

        if (state.loading) {
            ThemedCard { Text("Loading...") }
            return@MockupScreenColumn
        }

        if (state.baselineWeekActive) {
            ThemedCard(accent = true) {
                SectionLabel("Baseline week")
                Text(
                    "Collect numbers, no progression suggestions yet. Turn off in Settings after week 1.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        state.nextDay?.let { day ->
            ThemedCard(accent = true) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionLabel("Next up")
                    StatusChip(if (state.onSchedule) "On schedule" else "Off-day", selected = true)
                }
                BigValue(day.label)
                Text(
                    day.mockupSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ActionRow(
                    primaryText = "Start ${day.label}",
                    onPrimary = { vm.startSession(day) { id -> onStartSession(id) } }
                )
                state.alternativeForToday?.let { alt ->
                    OutlinedButton(
                        onClick = { vm.startSession(alt) { id -> onStartSession(id) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Or do ${alt.label} (today's hint)") }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipButton(
                text = "Pick a day",
                selected = dayPickerOpen,
                onClick = { dayPickerOpen = !dayPickerOpen }
            )
            ChipButton(text = "Mark rest", onClick = vm::markRestDay)
            ChipButton(text = "Skip next", onClick = vm::skipNextDay)
        }

        ThemedCard {
            SectionLabel("Last session")
            state.lastSessionLabel?.let { last ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(last)
                    Text(
                        state.daysSinceLastSession?.let { "${it} days ago" } ?: "Latest",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } ?: Text("No completed sessions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            DividerLine()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("This week")
                Text(
                    "${state.completedThisWeek} / ${state.sessionsTargetThisWeek} sessions",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        ThemedCard {
            SectionLabel("Streak")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccentText(state.completedThisWeek.toString())
                Text("sessions this week", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (dayPickerOpen && state.allNonRestDays.size > 1) {
            ThemedCard {
                SectionLabel("Pick a different day")
                state.allNonRestDays.forEach { day ->
                    OutlinedButton(
                        onClick = { vm.startSession(day) { id -> onStartSession(id) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(day.label) }
                }
            }
        }

        OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth()) {
            Text("Open history")
        }
    }
}

private fun ProgramDay.mockupSubtitle(): String =
    preferredDayOfWeek?.name
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?.let { "$it hint" }
        ?: "Ready for today's rotation"
