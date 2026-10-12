package com.maya.callguardian.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallHistoryEntity::class], version = 1, exportSchema = false)
abstract class MayaDatabase : RoomDatabase() {
    abstract fun callHistoryDao(): CallHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MayaDatabase? = null

        fun getInstance(context: Context): MayaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MayaDatabase::class.java,
                    "maya_security.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
