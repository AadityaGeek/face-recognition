package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEntity::class, VerificationLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        fun getDatabase(context: Context): AppDatabase {
            TODO("Implementation hidden")
        }
    }
}
