package com.gunsout.data.backup

import androidx.room.withTransaction
import com.gunsout.data.db.GunsoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val db: GunsoutDatabase
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val programs = db.programDao().observeAll().first().map { it.toBackup() }
        val days = programs.flatMap { db.programDayDao().getForProgram(it.id) }.map { it.toBackup() }
        val exercises = db.exerciseDao().observeAll().first().map { it.toBackup() }

        // ExerciseAlternates have no direct list-all DAO. We synthesize entries by walking exercises
        // and querying alternates per exercise. Reason is lost in this minimal export path; if you
        // need fidelity here, add an exposeAll() DAO query. PREFERENCE is the safest default.
        val alternates = exercises.flatMap { ex ->
            db.exerciseAlternateDao().getAlternates(ex.id)
                .map { ExerciseAlternateBackup(exerciseId = ex.id, alternateExerciseId = it.id, reason = "PREFERENCE") }
        }

        val programExercises = days.flatMap { day -> db.programExerciseDao().getForDay(day.id).map { it.toBackup() } }

        val sessions = db.workoutSessionDao().observeAll().first().map { it.toBackup() }
        val setEntries = sessions.flatMap { db.setEntryDao().getForSession(it.id).map { it.toBackup() } }

        val mealPlans = db.mealPlanDao().observeAll().first().map { it.toBackup() }
        val templates = mealPlans.flatMap { plan ->
            db.mealTemplateDao().observeForPlan(plan.id).first()
        }.distinctBy { it.id }.map { it.toBackup() }

        val ingredients = db.ingredientDao().observeAll().first().map { it.toBackup() }
        val mealTemplateIngredients = templates.flatMap { t -> db.mealTemplateIngredientDao().getForTemplate(t.id) }.map { it.toBackup() }

        val foodEntries = db.foodEntryDao().observeRange(
            java.time.LocalDate.of(1970, 1, 1), java.time.LocalDate.of(9999, 12, 31)
        ).first().map { it.toBackup() }

        val supplements = db.supplementDao().observeAll().first().map { it.toBackup() }
        val supplementLogs = supplements.flatMap { sup ->
            db.supplementLogDao().recentForSupplement(sup.id, java.time.LocalDate.of(1970, 1, 1))
        }.map { it.toBackup() }

        val bodyMetrics = db.bodyMetricsLogDao().observeAll().first().map { it.toBackup() }

        val backup = GunsoutBackup(
            exportedAtIso = LocalDateTime.now().toString(),
            programs = programs,
            programDays = days,
            exercises = exercises,
            exerciseAlternates = alternates,
            programExercises = programExercises,
            sessions = sessions,
            setEntries = setEntries,
            mealPlans = mealPlans,
            mealTemplates = templates,
            ingredients = ingredients,
            mealTemplateIngredients = mealTemplateIngredients,
            foodEntries = foodEntries,
            supplements = supplements,
            supplementLogs = supplementLogs,
            bodyMetricsLogs = bodyMetrics
        )
        json.encodeToString(GunsoutBackup.serializer(), backup)
    }

    /**
     * Replace all user data with the contents of [jsonText]. Wrapped in a transaction so a partial
     * import never leaves the DB half-populated. The current implementation REPLACES rather than
     * merges; callers should warn the user.
     */
    suspend fun importFromJson(jsonText: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = runCatching { json.decodeFromString(GunsoutBackup.serializer(), jsonText) }
            .getOrElse { return@withContext ImportResult.Error(it.message ?: "Parse failed") }

        if (parsed.schemaVersion != 1) {
            return@withContext ImportResult.Error("Unsupported backup schema v${parsed.schemaVersion}")
        }

        db.withTransaction {
            val helper = db.openHelper.writableDatabase
            // Clear in child-first order to respect foreign keys.
            for (sql in listOf(
                "DELETE FROM set_entry",
                "DELETE FROM workout_session",
                "DELETE FROM supplement_log",
                "DELETE FROM supplement",
                "DELETE FROM body_metrics_log",
                "DELETE FROM food_entry",
                "DELETE FROM meal_template_ingredient",
                "DELETE FROM meal_template",
                "DELETE FROM meal_plan",
                "DELETE FROM ingredient",
                "DELETE FROM program_exercise",
                "DELETE FROM exercise_alternate",
                "DELETE FROM exercise",
                "DELETE FROM program_day",
                "DELETE FROM program"
            )) helper.execSQL(sql)

            parsed.programs.forEach { db.programDao().insert(it.toEntity()) }
            parsed.programDays.forEach { db.programDayDao().insert(it.toEntity()) }
            parsed.exercises.forEach { db.exerciseDao().insert(it.toEntity()) }
            parsed.exerciseAlternates.forEach { db.exerciseAlternateDao().insert(it.toEntity()) }
            parsed.programExercises.forEach { db.programExerciseDao().insert(it.toEntity()) }
            parsed.sessions.forEach { db.workoutSessionDao().insert(it.toEntity()) }
            parsed.setEntries.forEach { db.setEntryDao().insert(it.toEntity()) }
            parsed.mealPlans.forEach { db.mealPlanDao().insert(it.toEntity()) }
            parsed.mealTemplates.forEach { db.mealTemplateDao().insert(it.toEntity()) }
            parsed.ingredients.forEach { db.ingredientDao().insert(it.toEntity()) }
            parsed.mealTemplateIngredients.forEach { db.mealTemplateIngredientDao().insert(it.toEntity()) }
            parsed.foodEntries.forEach { db.foodEntryDao().insert(it.toEntity()) }
            parsed.supplements.forEach { db.supplementDao().insert(it.toEntity()) }
            parsed.supplementLogs.forEach { db.supplementLogDao().insert(it.toEntity()) }
            parsed.bodyMetricsLogs.forEach { db.bodyMetricsLogDao().insert(it.toEntity()) }
        }
        ImportResult.Success(
            totalRows = parsed.programs.size + parsed.programDays.size + parsed.exercises.size +
                parsed.programExercises.size + parsed.sessions.size + parsed.setEntries.size +
                parsed.mealPlans.size + parsed.mealTemplates.size + parsed.ingredients.size +
                parsed.foodEntries.size + parsed.supplements.size + parsed.supplementLogs.size +
                parsed.bodyMetricsLogs.size
        )
    }
}

sealed class ImportResult {
    data class Success(val totalRows: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}
