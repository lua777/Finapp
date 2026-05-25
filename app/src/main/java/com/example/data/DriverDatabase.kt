package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DriverRecord::class], version = 2, exportSchema = false)
abstract class DriverDatabase : RoomDatabase() {
    abstract val dao: DriverRecordDao

    companion object {
        @Volatile
        private var INSTANCE: DriverDatabase? = null

        fun getDatabase(context: Context): DriverDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DriverDatabase::class.java,
                    "driver_finance_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
