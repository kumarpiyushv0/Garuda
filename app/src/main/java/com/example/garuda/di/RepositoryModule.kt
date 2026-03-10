package com.example.garuda.di

import com.example.garuda.data.repository.AudioRepositoryImpl
import com.example.garuda.data.repository.ContactRepositoryImpl
import com.example.garuda.data.repository.LocationRepositoryImpl
import com.example.garuda.domain.repository.AudioRepository
import com.example.garuda.domain.repository.ContactRepository
import com.example.garuda.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.example.garuda.data.manager.SystemEmergencyManagerImpl
import com.example.garuda.domain.manager.SystemEmergencyManager
import com.example.garuda.domain.classifier.EmergencyIntentAnalyzer
import com.example.garuda.domain.classifier.GeminiAnalyzer
import com.example.garuda.domain.classifier.OnDeviceAnalyzer
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSystemEmergencyManager(
        systemEmergencyManagerImpl: SystemEmergencyManagerImpl
    ): SystemEmergencyManager

    @Binds
    @Singleton
    @Named("on_device")
    abstract fun bindOnDeviceAnalyzer(
        onDeviceAnalyzer: OnDeviceAnalyzer
    ): EmergencyIntentAnalyzer

    @Binds
    @Singleton
    @Named("cloud")
    abstract fun bindGeminiAnalyzer(
        geminiAnalyzer: GeminiAnalyzer
    ): EmergencyIntentAnalyzer

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        audioRepositoryImpl: AudioRepositoryImpl
    ): AudioRepository

    @Binds
    @Singleton
    abstract fun bindContactRepository(
        contactRepositoryImpl: ContactRepositoryImpl
    ): ContactRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        appPreferencesRepositoryImpl: com.example.garuda.data.repository.AppPreferencesRepositoryImpl
    ): com.example.garuda.domain.repository.AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: com.example.garuda.data.repository.AuthRepositoryImpl
    ): com.example.garuda.domain.repository.AuthRepository
}
