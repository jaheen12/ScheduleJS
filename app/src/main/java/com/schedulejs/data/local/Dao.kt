package com.schedulejs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DayTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: DayTemplateEntity): Long

    @Query("SELECT * FROM day_templates WHERE dayType = :dayType LIMIT 1")
    suspend fun getByDayType(dayType: String): DayTemplateEntity?

    @Query("SELECT COUNT(*) FROM day_templates")
    suspend fun count(): Int

    @Query("SELECT * FROM day_templates ORDER BY id")
    suspend fun getAll(): List<DayTemplateEntity>

    @Update
    suspend fun update(template: DayTemplateEntity)
}

@Dao
interface TemplateTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TemplateTaskEntity>)

    @Query("SELECT * FROM template_tasks WHERE templateId = :templateId ORDER BY sortOrder")
    suspend fun getForTemplate(templateId: Long): List<TemplateTaskEntity>

    @Query("DELETE FROM template_tasks WHERE templateId = :templateId")
    suspend fun deleteForTemplate(templateId: Long)
}

@Dao
interface StudyRotationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rotations: List<StudyRotationEntity>)

    @Query("SELECT * FROM study_rotations WHERE dayOfWeek = :dayOfWeek ORDER BY blockType")
    suspend fun getForDay(dayOfWeek: Int): List<StudyRotationEntity>
}

@Dao
interface WorkoutRotationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rotations: List<WorkoutRotationEntity>)

    @Query("SELECT * FROM workout_rotations WHERE dayOfWeek = :dayOfWeek ORDER BY sortOrder")
    suspend fun getForDay(dayOfWeek: Int): List<WorkoutRotationEntity>
}

@Dao
interface WeeklyReviewLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WeeklyReviewLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeeklyReviewLogEntity)

    @Query("SELECT * FROM weekly_review_logs ORDER BY reviewDate DESC")
    suspend fun getAll(): List<WeeklyReviewLogEntity>
}

@Dao
interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>
}

@Dao
interface DailyProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: DailyProgressEntity)

    @Query("SELECT * FROM daily_progress WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): DailyProgressEntity?
}

@Dao
interface FocusTimerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: FocusTimerStateEntity)

    @Query("SELECT * FROM focus_timer_state WHERE id = 1")
    suspend fun get(): FocusTimerStateEntity?

    @Query("DELETE FROM focus_timer_state WHERE id = 1")
    suspend fun clear()
}

@Dao
interface BellyRoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: BellyRoutineStateEntity)

    @Query("SELECT * FROM belly_routine_state WHERE id = 1")
    suspend fun get(): BellyRoutineStateEntity?

    @Query("DELETE FROM belly_routine_state WHERE id = 1")
    suspend fun clear()
}

@Dao
interface NightlyChecklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checklist: NightlyChecklistEntity)

    @Query("SELECT * FROM nightly_checklists WHERE date = :date LIMIT 1")
    suspend fun getForDate(date: String): NightlyChecklistEntity?
}
