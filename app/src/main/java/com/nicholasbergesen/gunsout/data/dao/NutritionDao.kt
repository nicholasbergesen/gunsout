package com.nicholasbergesen.gunsout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nicholasbergesen.gunsout.data.entity.CreatineCheck
import com.nicholasbergesen.gunsout.data.entity.CreatineSettings
import com.nicholasbergesen.gunsout.data.entity.ProteinEntry
import com.nicholasbergesen.gunsout.data.entity.ProteinTargetSnapshot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ProteinEntryDao {
    @Query(
        """
        SELECT * FROM protein_entry
        WHERE userId = :userId AND date = :date
        ORDER BY loggedAt DESC, id DESC
        """
    )
    fun observeForDate(userId: String, date: LocalDate): Flow<List<ProteinEntry>>

    @Query(
        """
        SELECT * FROM protein_entry
        WHERE userId = :userId AND date BETWEEN :start AND :end
        ORDER BY date ASC, loggedAt ASC, id ASC
        """
    )
    fun observeRange(userId: String, start: LocalDate, end: LocalDate): Flow<List<ProteinEntry>>

    @Query("SELECT * FROM protein_entry WHERE userId = :userId ORDER BY date ASC, loggedAt ASC, id ASC")
    suspend fun getAll(userId: String): List<ProteinEntry>

    @Insert
    suspend fun insert(entry: ProteinEntry): Long

    @Query(
        """
        UPDATE protein_entry
        SET grams = :grams, label = :label
        WHERE id = :id AND userId = :userId
        """
    )
    suspend fun update(userId: String, id: Long, grams: Int, label: String?): Int

    @Query("DELETE FROM protein_entry WHERE id = :id AND userId = :userId")
    suspend fun delete(userId: String, id: Long): Int
}

@Dao
interface ProteinTargetSnapshotDao {
    @Query(
        """
        SELECT * FROM protein_target_snapshot
        WHERE userId = :userId AND date BETWEEN :start AND :end
        ORDER BY date ASC
        """
    )
    fun observeRange(
        userId: String,
        start: LocalDate,
        end: LocalDate
    ): Flow<List<ProteinTargetSnapshot>>

    @Query("SELECT * FROM protein_target_snapshot WHERE userId = :userId ORDER BY date ASC")
    suspend fun getAll(userId: String): List<ProteinTargetSnapshot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: ProteinTargetSnapshot)

    @Query("DELETE FROM protein_target_snapshot WHERE userId = :userId AND date = :date")
    suspend fun delete(userId: String, date: LocalDate): Int
}

@Dao
interface CreatineDao {
    @Query("SELECT * FROM creatine_settings WHERE userId = :userId")
    fun observeSettings(userId: String): Flow<CreatineSettings?>

    @Query("SELECT * FROM creatine_settings WHERE userId = :userId")
    suspend fun getSettings(userId: String): CreatineSettings?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSettings(settings: CreatineSettings): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: CreatineSettings)

    @Query("SELECT * FROM creatine_check WHERE userId = :userId AND date = :date")
    fun observeCheck(userId: String, date: LocalDate): Flow<CreatineCheck?>

    @Query("SELECT * FROM creatine_check WHERE userId = :userId AND date = :date")
    suspend fun getCheck(userId: String, date: LocalDate): CreatineCheck?

    @Query("SELECT * FROM creatine_check WHERE userId = :userId ORDER BY date ASC")
    suspend fun getAllChecks(userId: String): List<CreatineCheck>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCheck(check: CreatineCheck): Long

    @Query("DELETE FROM creatine_check WHERE userId = :userId AND date = :date")
    suspend fun deleteCheck(userId: String, date: LocalDate): Int
}
