package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.data.AppDatabase
import com.example.data.StickerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KeyProApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Auto-seed high quality animated .webp stickers if no stickers exist yet
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(this@KeyProApplication)
                val repo = StickerRepository(db.stickerDao())
                repo.ensureSampleStickersSeeded(this@KeyProApplication)

                // Start background clipboard sync manager
                val syncManager = com.example.sync.ClipboardSyncManager.getInstance(this@KeyProApplication)

                // Check and auto-purge expired history (7, 14, 28 days) if enabled
                val prefs = com.example.ui.keyboard.util.KeyboardPreferences.getInstance(this@KeyProApplication)
                if (prefs.autoDeleteClipboardEnabled.value) {
                    val snippetRepo = com.example.data.SnippetRepository(db.snippetDao())
                    snippetRepo.purgeExpiredHistory(prefs.clipboardRetentionDays.value)
                }
            } catch (_: Exception) {}
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .respectCacheHeaders(false)
            .build()
    }
}
