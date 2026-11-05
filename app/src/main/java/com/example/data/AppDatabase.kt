package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import com.example.data.auth.AuthAuditLog
import com.example.data.auth.AuthAuditLogDao
import com.example.data.auth.UserDao
import com.example.data.auth.UserEntity

@Database(
    entities = [
        Cabin::class,
        CabinMessage::class,
        CabinTask::class,
        AudioParticipant::class,
        OutboxEntry::class,
        UserEntity::class,
        AuthAuditLog::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cabinDao(): CabinDao
    abstract fun messageDao(): MessageDao
    abstract fun taskDao(): TaskDao
    abstract fun outboxDao(): OutboxDao
    abstract fun userDao(): UserDao
    abstract fun authAuditLogDao(): AuthAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "private_cabins_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
