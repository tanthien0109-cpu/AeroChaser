package com.aerochaser.di

import android.location.Geocoder
import com.aerochaser.BuildConfig
import com.aerochaser.data.ai.GeminiAiSummaryRepository
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.Locale
import androidx.room.Room
import com.aerochaser.data.local.AppDatabase
import com.aerochaser.data.local.exif.AndroidExifParser
import com.aerochaser.data.local.io.AndroidFileIO
import com.aerochaser.data.repository.PhotoRepositoryImpl
import com.aerochaser.domain.exif.ExifParser
import com.aerochaser.domain.io.FileIO
import com.aerochaser.domain.repository.AiSummaryRepository
import com.aerochaser.domain.repository.GearInsightRepository
import com.aerochaser.domain.repository.PhotoRepository
import com.aerochaser.domain.usecase.GetPhotosUseCase
import com.aerochaser.domain.usecase.ScanDirectoryUseCase
import com.aerochaser.presentation.detail.PhotoDetailViewModel
import com.aerochaser.presentation.timeline.TimelineViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "aerochaser-db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    single { get<AppDatabase>().photoDao() }
    single { get<AppDatabase>().aiSummaryDao() }

    // Google Services
    single { FirebaseAnalytics.getInstance(androidContext()) }
    single { Geocoder(androidContext(), Locale.getDefault()) }

    // Platform-specific implementations
    // PLATFORM-SPECIFIC: Android FileIO via SAF
    single<FileIO> { AndroidFileIO(androidContext()) }
    // PLATFORM-SPECIFIC: Android ExifInterface
    single<ExifParser> { AndroidExifParser(androidContext()) }

    // Repository
    single<PhotoRepository> { PhotoRepositoryImpl(get()) }

    // Use Cases
    factory { GetPhotosUseCase(get()) }
    factory { ScanDirectoryUseCase(get(), get(), get()) }

    single<GearInsightRepository> { com.aerochaser.data.repository.GearInsightRepositoryStub() }

    // AI Summary
    single<AiSummaryRepository> {
        GeminiAiSummaryRepository(
            aiSummaryDao = get(),
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // Cloud
    single { com.aerochaser.data.cloud.GoogleDrivePhotoSource(androidContext()) }
    single { com.aerochaser.data.cloud.GooglePhotosSource() }

    // ViewModels
    viewModel { TimelineViewModel(get()) }
    viewModel { PhotoDetailViewModel(get(), get(), get(), get()) }
    viewModel { com.aerochaser.presentation.cloud.CloudImportViewModel(androidContext(), get(), get(), get()) }
}
