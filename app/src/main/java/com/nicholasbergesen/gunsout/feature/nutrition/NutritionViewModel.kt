package com.nicholasbergesen.gunsout.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.repo.CreatineRepository
import com.nicholasbergesen.gunsout.data.repo.ProteinRepository
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinDayRecord
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinHistory
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinHistoryRange
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinHistorySeries
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinTarget
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinTargetCalculator
import com.nicholasbergesen.gunsout.domain.nutrition.ProteinTargetRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

data class NutritionUiState(
    val today: LocalDate = LocalDate.now(),
    val todayEntries: List<ProteinEntry> = emptyList(),
    val totalProteinGrams: Long = 0,
    val proteinTarget: ProteinTarget? = null,
    val creatineSettings: CreatineSettings? = null,
    val creatineCheck: CreatineCheck? = null,
    val history: ProteinHistorySeries =
        ProteinHistorySeries(ProteinHistoryRange.WEEK, emptyList())
)

private data class ProteinHistoryData(
    val allEntries: List<ProteinEntry>,
    val series: ProteinHistorySeries
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val proteinRepository: ProteinRepository,
    private val creatineRepository: CreatineRepository,
    private val userPreferences: UserPreferences,
    private val currentUserIdProvider: CurrentUserIdProvider
) : ViewModel() {
    private val dayFlow = MutableStateFlow(LocalDate.now())
    private val historyRange = MutableStateFlow(ProteinHistoryRange.WEEK)

    init {
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
                dayFlow.value = LocalDate.now()
            }
        }
    }

    val state: StateFlow<NutritionUiState> = currentUserIdProvider.currentUserId
        .filterNotNull()
        .flatMapLatest { userId ->
            dayFlow.flatMapLatest { today ->
                val historyStart = YearMonth.from(today).minusMonths(11).atDay(1)
                val entriesFlow =
                    proteinRepository.observeEntriesRange(userId, historyStart, today)
                val snapshotsFlow =
                    proteinRepository.observeTargetSnapshots(userId, historyStart, today)
                val targetFlow = combine(
                    userPreferences.profile(userId),
                    userPreferences.targetOverrides(userId)
                ) { profile, overrides ->
                    ProteinTargetCalculator.effective(profile, overrides.proteinG)
                }
                    .distinctUntilChanged()
                    .onEach { target ->
                        proteinRepository.syncTodayTarget(
                            userId = userId,
                            date = today,
                            targetGrams = target?.grams
                        )
                    }
                val historyDataFlow = combine(
                    entriesFlow,
                    snapshotsFlow,
                    historyRange
                ) { entries, snapshots, selectedRange ->
                    ProteinHistoryData(
                        allEntries = entries,
                        series = ProteinHistory.aggregate(
                            range = selectedRange,
                            today = today,
                            records = entries.map { ProteinDayRecord(it.date, it.grams) },
                            targets = snapshots.map {
                                ProteinTargetRecord(it.date, it.targetGrams)
                            }
                        )
                    )
                }
                val creatineCheckFlow = creatineRepository.observeCheck(userId, today)

                combine(
                    historyDataFlow,
                    targetFlow,
                    creatineRepository.observeSettings(userId),
                    creatineCheckFlow
                ) { historyData, target, settings, creatineCheck ->
                    val todayEntries = historyData.allEntries
                        .asSequence()
                        .filter { it.date == today }
                        .sortedWith(
                            compareByDescending<ProteinEntry> { it.loggedAt }
                                .thenByDescending { it.id }
                        )
                        .toList()
                    NutritionUiState(
                        today = today,
                        todayEntries = todayEntries,
                        totalProteinGrams = todayEntries.sumOf { it.grams.toLong() },
                        proteinTarget = target,
                        creatineSettings = settings,
                        creatineCheck = creatineCheck,
                        history = historyData.series
                    )
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            NutritionUiState()
        )

    fun refreshDay() {
        dayFlow.value = LocalDate.now()
    }

    fun selectHistoryRange(range: ProteinHistoryRange) {
        historyRange.value = range
    }

    suspend fun addProtein(grams: Int, label: String?): Long {
        val userId = currentUserIdProvider.requireUserId()
        return proteinRepository.addEntry(userId, dayFlow.value, grams, label)
    }

    suspend fun updateEntry(entryId: Long, grams: Int, label: String?): Boolean {
        val userId = currentUserIdProvider.requireUserId()
        return proteinRepository.updateEntry(userId, entryId, grams, label)
    }

    suspend fun deleteEntry(entryId: Long): Boolean {
        val userId = currentUserIdProvider.requireUserId()
        return proteinRepository.deleteEntry(userId, entryId)
    }

    suspend fun restoreEntry(entry: ProteinEntry): Long {
        val userId = currentUserIdProvider.requireUserId()
        return proteinRepository.restoreEntry(userId, entry)
    }

    suspend fun setCreatineTaken(taken: Boolean) {
        val userId = currentUserIdProvider.requireUserId()
        creatineRepository.setTaken(userId, dayFlow.value, taken)
    }

    suspend fun updateCreatine(doseGrams: Int, reminderTime: LocalTime?) {
        val userId = currentUserIdProvider.requireUserId()
        creatineRepository.updateSettings(userId, doseGrams, reminderTime)
    }
}
