package com.nicholasbergesen.gunsout.data.repo

import com.nicholasbergesen.gunsout.data.dao.FoodEntryDao
import com.nicholasbergesen.gunsout.data.dao.MealTemplateDao
import com.nicholasbergesen.gunsout.data.entity.FoodEntry
import com.nicholasbergesen.gunsout.data.entity.MealTemplate
import com.nicholasbergesen.gunsout.data.entity.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DietRepositoryTest {

    @Test fun `template logging scales macros and clamps negative multipliers`() = runTest {
        val foodDao = FakeFoodEntryDao()
        val repository = DietRepository(FakeMealTemplateDao(), foodDao)
        val template = MealTemplate(
            id = 7,
            userId = "user",
            name = "Protein oats",
            mealType = MealType.BREAKFAST,
            kcal = 500,
            proteinG = 40.0,
            carbsG = 60.0,
            fatG = 10.0
        )
        val date = LocalDate.of(2026, 6, 14)

        val scaledId = repository.logFromTemplate("user", template, date, multiplier = 1.5)
        val clampedId = repository.logFromTemplate("user", template, date, multiplier = -2.0)

        val scaled = foodDao.entry(scaledId)
        assertEquals("Protein oats (1.5x)", scaled.name)
        assertEquals(750, scaled.kcal)
        assertEquals(60.0, scaled.proteinG, 0.001)
        assertEquals(90.0, scaled.carbsG, 0.001)
        assertEquals(15.0, scaled.fatG, 0.001)
        assertEquals(template.id, scaled.sourceTemplateId)

        val clamped = foodDao.entry(clampedId)
        assertEquals("Protein oats (0x)", clamped.name)
        assertEquals(0, clamped.kcal)
        assertEquals(0.0, clamped.proteinG, 0.001)
        assertEquals(0.0, clamped.carbsG, 0.001)
        assertEquals(0.0, clamped.fatG, 0.001)
    }

    @Test fun `custom food entries can be observed updated and deleted`() = runTest {
        val foodDao = FakeFoodEntryDao()
        val repository = DietRepository(FakeMealTemplateDao(), foodDao)
        val date = LocalDate.of(2026, 6, 14)

        val id = repository.logCustomFood(
            userId = "user",
            date = date,
            mealType = MealType.DINNER,
            name = "Chicken rice bowl",
            kcal = 640,
            proteinG = 52.0,
            carbsG = 76.0,
            fatG = 12.0
        )

        assertEquals(1, repository.observeEntriesForDate("user", date).first().size)
        assertEquals(1, repository.observeEntriesRange("user", date.minusDays(1), date.plusDays(1)).first().size)

        repository.updateEntry(foodDao.entry(id).copy(name = "Chicken rice bowl with salsa"))
        assertEquals("Chicken rice bowl with salsa", foodDao.entry(id).name)

        repository.deleteEntry(id)
        assertNull(foodDao.entryOrNull(id))
    }

    @Test fun `templates can be saved and observed by user`() = runTest {
        val templateDao = FakeMealTemplateDao()
        val repository = DietRepository(templateDao, FakeFoodEntryDao())
        val template = MealTemplate(
            userId = "user",
            name = "Greek yogurt",
            mealType = MealType.SNACK,
            kcal = 180,
            proteinG = 20.0,
            carbsG = 12.0,
            fatG = 4.0,
            seedKey = "yogurt"
        )

        val id = repository.saveTemplate(template)

        assertEquals("Greek yogurt", repository.observeTemplates("user").first().single().name)
        assertEquals(id, templateDao.getBySeedKey("user", "yogurt")!!.id)
    }

    private class FakeMealTemplateDao : MealTemplateDao {
        private val rows = linkedMapOf<Long, MealTemplate>()
        private var nextId = 1L

        override fun observeAll(userId: String): Flow<List<MealTemplate>> =
            flowOf(rows.values.filter { it.userId == userId }.sortedWith(compareBy<MealTemplate> { it.mealType.name }.thenBy { it.name }))

        override suspend fun getAll(userId: String): List<MealTemplate> =
            rows.values.filter { it.userId == userId }.sortedBy { it.id }

        override suspend fun getById(id: Long): MealTemplate? = rows[id]

        override suspend fun getBySeedKey(userId: String, seedKey: String): MealTemplate? =
            rows.values.firstOrNull { it.userId == userId && it.seedKey == seedKey }

        override suspend fun insert(template: MealTemplate): Long {
            val id = template.id.takeIf { it != 0L } ?: nextId++
            rows[id] = template.copy(id = id)
            return id
        }

        override suspend fun update(template: MealTemplate) {
            rows[template.id] = template
        }

        override suspend fun delete(id: Long) {
            rows.remove(id)
        }
    }

    private class FakeFoodEntryDao : FoodEntryDao {
        private val rows = linkedMapOf<Long, FoodEntry>()
        private var nextId = 1L

        override fun observeForDate(userId: String, date: LocalDate): Flow<List<FoodEntry>> =
            flowOf(rows.values.filter { it.userId == userId && it.date == date }.sortedBy { it.createdAt })

        override fun observeRange(userId: String, start: LocalDate, end: LocalDate): Flow<List<FoodEntry>> =
            flowOf(rows.values.filter { it.userId == userId && !it.date.isBefore(start) && !it.date.isAfter(end) }
                .sortedWith(compareBy<FoodEntry> { it.date }.thenBy { it.createdAt }))

        override suspend fun getAll(userId: String): List<FoodEntry> =
            rows.values.filter { it.userId == userId }.sortedBy { it.id }

        override suspend fun insert(entry: FoodEntry): Long {
            val id = entry.id.takeIf { it != 0L } ?: nextId++
            rows[id] = entry.copy(id = id)
            return id
        }

        override suspend fun update(entry: FoodEntry) {
            rows[entry.id] = entry
        }

        override suspend fun delete(id: Long) {
            rows.remove(id)
        }

        fun entry(id: Long): FoodEntry = rows.getValue(id)
        fun entryOrNull(id: Long): FoodEntry? = rows[id]
    }
}
