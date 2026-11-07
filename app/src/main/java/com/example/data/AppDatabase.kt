package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SnippetEntity::class,
        WordFrequencyEntity::class,
        BigramFrequencyEntity::class,
        StickerEntity::class,
        ConnectedDeviceEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao
    abstract fun wordFrequencyDao(): WordFrequencyDao
    abstract fun bigramFrequencyDao(): BigramFrequencyDao
    abstract fun stickerDao(): StickerDao
    abstract fun connectedDeviceDao(): ConnectedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add originalCase column to word_frequency
                db.execSQL("ALTER TABLE word_frequency ADD COLUMN originalCase TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE word_frequency SET originalCase = word WHERE originalCase = ''")
                // Create bigram_frequencies table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS bigram_frequencies (
                        prevWord TEXT NOT NULL,
                        word TEXT NOT NULL,
                        count INTEGER NOT NULL DEFAULT 1,
                        lastUsed INTEGER NOT NULL,
                        PRIMARY KEY (prevWord, word)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stickers ADD COLUMN gifPath TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE stickers ADD COLUMN staticPngPath TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keypro_keyboard_db"
                )
                    .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_SNIPPETS: List<SnippetEntity> = emptyList()
    }
}
