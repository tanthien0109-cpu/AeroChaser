package com.aerochaser.di

import androidx.room.Room
import com.aerochaser.data.local.AppDatabase
import com.aerochaser.data.local.exif.AndroidExifParser
import com.aerochaser.data.local.io.AndroidFileIO
import com.aerochaser.data.repository.PhotoRepositoryImpl
import com.aerochaser.domain.exif.ExifParser
import com.aerochaser.domain.io.FileIO
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
        ).build()
    }
    single { get<AppDatabase>().photoDao() }

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

    // ViewModels
    viewModel { TimelineViewModel(get()) }
    viewModel { PhotoDetailViewModel(get()) }
}
