package com.example.cad.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DrawingEntity::class, ProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CadDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: CadDatabase? = null

        fun getInstance(context: Context): CadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CadDatabase::class.java,
                    "cad_mobile_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
