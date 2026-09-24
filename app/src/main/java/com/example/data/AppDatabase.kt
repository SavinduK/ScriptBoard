package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [SnippetEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun snippetDao(): SnippetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keypro_keyboard_db"
                )
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
