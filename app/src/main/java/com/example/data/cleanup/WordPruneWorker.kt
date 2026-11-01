package com.example.data.cleanup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import java.util.concurrent.TimeUnit

/**
 * Weekly periodic worker that deletes learned words and bigrams with low counts
 * that have not been used for 6+ months (recency decay prune).
 */
class WordPruneWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            // 6 months in milliseconds (~180 days)
            val sixMonthsAgo = System.currentTimeMillis() - (180L * 24L * 60L * 60L * 1000L)
            
            // Delete words with frequency < 3 unused for 6+ months
            db.wordFrequencyDao().deleteStaleWords(cutoffTimestamp = sixMonthsAgo, minFrequency = 3)
            
            // Delete bigrams with count < 3 unused for 6+ months
            db.bigramFrequencyDao().deleteStaleBigrams(cutoffTimestamp = sixMonthsAgo, minCount = 3)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "keypro_word_prune_periodic_work"

        fun scheduleWeeklyPrune(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<WordPruneWorker>(7, TimeUnit.DAYS)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } catch (_: Exception) {}
        }
    }
}
