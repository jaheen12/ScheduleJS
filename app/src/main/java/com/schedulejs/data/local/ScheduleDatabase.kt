package com.schedulejs.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DayTemplateEntity::class,
        TemplateTaskEntity::class,
        StudyRotationEntity::class,
        WorkoutRotationEntity::class,
        WeeklyReviewLogEntity::class,
        AppSettingsEntity::class,
        DailyProgressEntity::class,
        FocusTimerStateEntity::class,
        BellyRoutineStateEntity::class,
        NightlyChecklistEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun dayTemplateDao(): DayTemplateDao
    abstract fun templateTaskDao(): TemplateTaskDao
    abstract fun studyRotationDao(): StudyRotationDao
    abstract fun workoutRotationDao(): WorkoutRotationDao
    abstract fun weeklyReviewLogDao(): WeeklyReviewLogDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun dailyProgressDao(): DailyProgressDao
    abstract fun focusTimerDao(): FocusTimerDao
    abstract fun bellyRoutineDao(): BellyRoutineDao
    abstract fun nightlyChecklistDao(): NightlyChecklistDao

    companion object {
        @Volatile
        private var instance: ScheduleDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_progress` (
                        `date` TEXT NOT NULL,
                        `isWorkoutComplete` INTEGER NOT NULL,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_timer_state` (
                        `id` INTEGER NOT NULL,
                        `totalDurationSeconds` INTEGER NOT NULL,
                        `remainingSeconds` INTEGER NOT NULL,
                        `isRunning` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `startedAtEpochMillis` INTEGER,
                        `enableDnd` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `belly_routine_state` (
                        `id` INTEGER NOT NULL,
                        `accumulatedElapsedSeconds` INTEGER NOT NULL,
                        `isRunning` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `startedAtEpochMillis` INTEGER,
                        `lastCueStepIndex` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `nightly_checklists` (
                        `date` TEXT NOT NULL,
                        `clothesLaidOut` INTEGER NOT NULL,
                        `lunchPrepped` INTEGER NOT NULL,
                        `bagPacked` INTEGER NOT NULL,
                        `completedAtEpochMillis` INTEGER,
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE `app_settings`
                    ADD COLUMN `notificationsEducationDismissed` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    ALTER TABLE `app_settings`
                    ADD COLUMN `exactAlarmEducationDismissed` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    ALTER TABLE `app_settings`
                    ADD COLUMN `dndEducationDismissed` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    ALTER TABLE `app_settings`
                    ADD COLUMN `batteryOptimizationEducationDismissed` INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): ScheduleDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScheduleDatabase::class.java,
                    "schedulejs.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
