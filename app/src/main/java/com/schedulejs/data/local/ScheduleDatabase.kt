package com.schedulejs.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DayTemplateEntity::class,
        TemplateTaskEntity::class,
        StudyRotationEntity::class,
        WorkoutRotationEntity::class,
        WeeklyReviewLogEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScheduleDatabase : RoomDatabase() {
    abstract fun dayTemplateDao(): DayTemplateDao
    abstract fun templateTaskDao(): TemplateTaskDao
    abstract fun studyRotationDao(): StudyRotationDao
    abstract fun workoutRotationDao(): WorkoutRotationDao
    abstract fun weeklyReviewLogDao(): WeeklyReviewLogDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var instance: ScheduleDatabase? = null

        fun getInstance(context: Context): ScheduleDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScheduleDatabase::class.java,
                    "schedulejs.db"
                ).build().also { instance = it }
            }
        }
    }
}
