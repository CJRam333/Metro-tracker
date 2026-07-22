package com.example.hmrcompanion

import android.app.Application
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration

class HmrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            load(this@HmrApplication, PreferenceManager.getDefaultSharedPreferences(this@HmrApplication))
            userAgentValue = packageName
            // Cache up to 100MB of tiles for offline use
            tileFileSystemCacheMaxBytes = 100L * 1024 * 1024
            expirationOverrideDuration = 1000L * 60 * 60 * 24 * 30
        }
    }
}
