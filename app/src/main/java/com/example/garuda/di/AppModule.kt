package com.example.garuda.di

import android.content.Context
import com.example.garuda.GarudaApp
import com.example.garuda.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplication(@ApplicationContext app: Context): GarudaApp {
        return app as GarudaApp
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "garuda_db"
        ).build()
    }

    @Provides
    fun provideContactDao(db: AppDatabase): com.example.garuda.data.local.dao.ContactDao {
        return db.contactDao()
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(app: GarudaApp): com.google.android.gms.location.FusedLocationProviderClient {
        return com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(app)
    }

    @Provides
    @Singleton
    fun provideLocationClient(
        app: GarudaApp,
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): com.example.garuda.domain.location.LocationClient {
        return com.example.garuda.data.location.DefaultLocationClient(app, client)
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): com.google.firebase.database.FirebaseDatabase {
        // Use the correct regional database URL
        return com.google.firebase.database.FirebaseDatabase.getInstance(
            "https://garuda-1d2ad-default-rtdb.asia-southeast1.firebasedatabase.app"
        )
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): com.google.firebase.auth.FirebaseAuth {
        return com.google.firebase.auth.FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage {
        return com.google.firebase.storage.FirebaseStorage.getInstance()
    }
}
