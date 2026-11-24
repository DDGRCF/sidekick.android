package dev.ddgrcf.sidekick.core.store

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase

@Database(
    entities = [SessionEntity::class, MessageEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SidekickDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun historyDao(): HistoryDao

    companion object {
        fun create(context: Context): SidekickDatabase =
            Room.databaseBuilder(context.applicationContext, SidekickDatabase::class.java, "sidekick.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
