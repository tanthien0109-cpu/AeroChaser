package com.aerochaser

import android.app.Application
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.aerochaser.di.appModule

class AeroChaserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase services to ensure no PII is collected automatically
        val analytics = FirebaseAnalytics.getInstance(this)
        // Enable basic tracking without ad-id for privacy
        analytics.setAnalyticsCollectionEnabled(true)
        
        // Performance & Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        
        startKoin {
            androidLogger()
            androidContext(this@AeroChaserApp)
            modules(appModule)
        }
    }
}
