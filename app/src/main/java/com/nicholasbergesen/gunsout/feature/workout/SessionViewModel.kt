package com.nicholasbergesen.gunsout.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.CurrentUserIdProvider
import com.nicholasbergesen.gunsout.data.entity.Exercise
import com.nicholasbergesen.gunsout.data.entity.ProgramExercise
import com.nicholasbergesen.gunsout.data.entity.SetEntry
import com.nicholasbergesen.gunsout.data.entity.WorkoutSession
import com.nicholasbergesen.gunsout.data.prefs.UserPreferences
import com.nicholasbergesen.gunsout.data.repo.BodyRepository
import com.nicholasbergesen.gunsout.data.repo.WorkoutRepository
import com.nicholasbergesen.gunsout.domain.recommendation.ExerciseRecommendation
import com.nicholasbergesen.gunsout.domain.recommendation.ExerciseRecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Collections
import javax.inject.Inject

data class PlannedExerciseUi(
    val programExercise: ProgramExercise,
    val exercise: Exercise,
    val sets: List<SetEntry>,
    val previousBest: SetEntry?,
    val recommendation: ExerciseRecommendation?,
    val alternates: List<Exercise> = emptyList(),
    val prescription: ProgramExercise = programExercise
)

data class SessionUiState(
    val loading: Boolean = true,
    val session: WorkoutSession? = null,
    val dayLabel: String = "",
    val items: List<PlannedExerciseUi> = emptyList(),
    val baselineWeekActive: Boolean = true,
    val kneeFeel: Int? = null,
    val notes: String = "",
    val finished: Boolean = false
)

internal data class RestTimerRequest(val durationSec: Int, val exerciseName: String)

internal fun sessionSetEntryForLog(
    userId: String,
    sessionId: Long,
    programExercise: ProgramExercise,
    exercise: Exercise,
    setIndex: Int,
    weightKg: Double?,
    reps: Int?,
    rpe: Int?,
    isWarmup: Boolean,
    completedAt: LocalDateTime
) = SetEntry(
    userId = userId,
    sessionId = sessionId,
    programExerciseId = programExercise.id,
    exerciseIdSnapshot = exercise.id,
    exerciseNameSnapshot = exercise.name,
    setIndex = setIndex,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isWarmup = isWarmup,
    completedAt = completedAt
)

internal fun restTimerRequestForLoggedSet(
    programExercise: ProgramExercise,
    exercise: Exercise,
    setIndex: Int,
    itemIndex: Int,
    itemCount: Int,
    isWarmup: Boolean
): RestTimerRequest? {
    val isLastSet = setIndex >= programExercise.sets
    val isLastExercise = itemIndex == itemCount - 1
    return if (!isWarmup && !(isLastSet && isLastExercise)) {
        RestTimerRequest(programExercise.restSec, exercise.name)
    } else {
        null
    }
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workouts: WorkoutRepository,
    private val bodyRepository: BodyRepository,
    private val userPrefs: UserPreferences,
    private val currentUserIdProvider: CurrentUserIdProvider,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    private val engine = ExerciseRecommendationEngine()
    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    private val sessionWriteMutex = Mutex()
    private val pendingSetSaves = Collections.synchronizedSet(mutableSetOf<Job>())
    private var finishRequested = false

    /**
     * In-session swap overrides: maps programExerciseId -> exerciseId to use just for this
     * session. Persisted swaps (save to program) are written directly to the ProgramExercise row,
     * so they do not appear here.
     */
    private val sessionOverrides: MutableMap<Long, Long> = mutableMapOf()

    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init { load() }

    private fun load() = viewModelScope.launch {
        val userId = currentUserIdProvider.requireUserId()
        val session = workouts.getSessionById(sessionId) ?: return@launch
        val pdId = session.programDayId ?: return@launch
        val profile = userPrefs.profile(userId).first()
        val latestBodyLog = bodyRepository.getLatest(userId)
        val recentBodyLogs = bodyRepository.observeSince(userId, LocalDate.now().minusDays(28)).first()
        val activeProgram = workouts.getActiveProgram(userId)
        val baseline = com.nicholasbergesen.gunsout.domain.baseline.BaselineWeekResolver.isActive(
            programCreatedAt = activeProgram?.createdAt,
            forcedFlag = profile.baselineWeekActive
        )
        val pds = workouts.getProgramExercisesForSession(pdId, sessionId)
        val currentSetsByProgramExercise = workouts.getSetsForSession(sessionId)
            .groupBy { it.programExerciseId }
        val items = pds.map { pe ->
            val currentSets = currentSetsByProgramExercise[pe.id].orEmpty()
            val effective = resolvedSessionExerciseIdentity(
                programExercise = pe,
                currentSets = currentSets,
                overrideExerciseId = sessionOverrides[pe.id]
            )
            val ex = workouts.getExercise(effective.exerciseId)
                ?: Exercise(
                    id = effective.exerciseId,
                    userId = userId,
                    name = effective.fallbackName ?: "(unknown)",
                    primaryMuscleGroup = com.nicholasbergesen.gunsout.data.entity.MuscleGroup.OTHER,
                    equipment = com.nicholasbergesen.gunsout.data.entity.Equipment.OTHER
                )
            val rowExerciseSeedKey = if (pe.exerciseId == ex.id) {
                ex.seedKey
            } else {
                workouts.getExercise(pe.exerciseId)?.seedKey
            }
            val prescription = resolvedSessionExercisePrescription(
                programExercise = pe,
                identity = effective,
                rowExerciseSeedKey = rowExerciseSeedKey,
                snapshotExerciseSeedKey = ex.seedKey
            )
            val recent = workouts.getPreviousSetsForExercise(userId, ex.id)
            // Group recent sets into prior-session batches and order them by the session date so
            // backup-restored IDs (which may be out of chronological order) still surface the
            // genuinely most recent session.
            val priorSessions = recent
                .filter { it.sessionId != sessionId }
                .groupBy { it.sessionId }
                .mapNotNull { (sid, sets) ->
                    val date = workouts.getSessionById(sid)?.date ?: return@mapNotNull null
                    Triple(date, sid, sets)
                }
                .sortedByDescending { it.first }
            val priorBest = priorSessions.firstOrNull()?.third
                ?.maxByOrNull { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val priorSets = priorSessions.firstOrNull()?.third.orEmpty()
            val recommendation = engine.recommend(
                prescription = prescription,
                exercise = ex,
                previousWorkingSets = priorSets,
                baselineWeekActive = baseline,
                profile = profile,
                latestBodyLog = latestBodyLog,
                recentBodyLogs = recentBodyLogs
            )
            val alternates = workouts.getAlternates(ex.id)
            PlannedExerciseUi(
                programExercise = pe,
                exercise = ex,
                sets = currentSets,
                previousBest = priorBest,
                recommendation = recommendation,
                alternates = alternates,
                prescription = prescription
            )
        }
        _state.update {
            it.copy(
                loading = false,
                session = session,
                dayLabel = session.programDayLabelSnapshot,
                items = items,
                baselineWeekActive = baseline
            )
        }
    }

    /**
     * Swap a planned exercise to an alternate. If [saveToProgram] is true, persists the swap on
     * the ProgramExercise row so future sessions inherit it. Otherwise the swap is recorded in
     * this VM's override map and the slot's logged sets are retargeted to the new exercise
     * snapshot. Either way, [load] picks up the change on the next refresh.
     */
    fun swapExercise(programExercise: ProgramExercise, newExerciseId: Long, saveToProgram: Boolean) {
        viewModelScope.launch {
            if (saveToProgram) {
                workouts.persistExerciseSwap(programExercise, newExerciseId)
                sessionOverrides.remove(programExercise.id)
            } else {
                sessionOverrides[programExercise.id] = newExerciseId
            }
            workouts.retargetSetsForSlot(sessionId, programExercise.id, newExerciseId)
            load()
        }
    }

    fun logSet(
        programExercise: ProgramExercise,
        exercise: Exercise,
        setIndex: Int,
        weightKg: Double?,
        reps: Int?,
        rpe: Int?,
        isWarmup: Boolean = false
    ) {
        if (finishRequested) return
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            sessionWriteMutex.withLock {
                val userId = currentUserIdProvider.requireUserId()
                workouts.logSet(
                    sessionSetEntryForLog(
                        userId = userId,
                        sessionId = sessionId,
                        programExercise = programExercise,
                        exercise = exercise,
                        setIndex = setIndex,
                        weightKg = weightKg,
                        reps = reps,
                        rpe = rpe,
                        isWarmup = isWarmup,
                        completedAt = LocalDateTime.now()
                    )
                )
                // Skip rest timer on warmup sets and on the very last set of the last exercise.
                val items = _state.value.items
                val itemIndex = items.indexOfFirst { it.programExercise.id == programExercise.id }
                val timer = restTimerRequestForLoggedSet(
                    programExercise = programExercise,
                    exercise = exercise,
                    setIndex = setIndex,
                    itemIndex = itemIndex,
                    itemCount = items.size,
                    isWarmup = isWarmup
                )
                if (timer != null) {
                    RestTimerService.start(appContext, timer.durationSec, timer.exerciseName)
                }
                load()
            }
        }
        pendingSetSaves.add(job)
        job.invokeOnCompletion { pendingSetSaves.remove(job) }
        job.start()
    }

    fun setKneeFeel(value: Int?) = _state.update { it.copy(kneeFeel = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun finish() = viewModelScope.launch {
        finishRequested = true
        synchronized(pendingSetSaves) { pendingSetSaves.toList() }.joinAll()
        sessionWriteMutex.withLock {
            workouts.completeSession(sessionId, _state.value.kneeFeel, _state.value.notes.ifBlank { null })
            RestTimerService.stop(appContext)
            _state.update { it.copy(finished = true) }
        }
    }
}
