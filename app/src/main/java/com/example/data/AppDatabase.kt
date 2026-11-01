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
    version = 9,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keypro_keyboard_db"
                )
                    .addMigrations(MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default starter snippets
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.snippetDao()?.insertAll(DEFAULT_SNIPPETS)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val DEFAULT_SNIPPETS = listOf(
            SnippetEntity(
                title = "Email Address",
                content = "alex.rivera@example.com",
                isPinned = true,
                category = "Work",
                shortcut = "@email"
            ),
            SnippetEntity(
                title = "Email Signature",
                content = "Best regards,\nAlex Rivera",
                isPinned = true,
                category = "Work",
                shortcut = "@sig"
            ),
            SnippetEntity(
                title = "Quick Reply",
                content = "Sounds great! Looking forward to it.",
                isPinned = true,
                category = "Messages",
                shortcut = "@reply"
            ),
            SnippetEntity(
                title = "Terminal Bash",
                content = "git commit -m \"feat: update keyboard components\"",
                isPinned = true,
                category = "Code",
                shortcut = "@git"
            ),
            SnippetEntity(
                title = "Meeting Link",
                content = "https://meet.google.com/xyz-abcd-efg",
                isPinned = true,
                category = "Work",
                shortcut = "@meet"
            ),
            SnippetEntity(
                title = "Home Address",
                content = "742 Evergreen Terrace, Springfield, OR 97477",
                isPinned = true,
                category = "Personal",
                shortcut = "@address"
            )
        )
    }
}
