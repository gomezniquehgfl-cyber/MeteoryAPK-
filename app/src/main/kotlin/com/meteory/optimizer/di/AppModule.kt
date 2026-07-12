package com.meteory.optimizer.di

import android.content.Context
import androidx.room.Room
import com.meteory.optimizer.data.*
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
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "meteory.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideGameProfileDao(db: AppDatabase): GameProfileDao = db.gameProfileDao()
    @Provides fun provideSystemHealthLogDao(db: AppDatabase): SystemHealthLogDao = db.systemHealthLogDao()
    @Provides fun provideCleaningHistoryDao(db: AppDatabase): CleaningHistoryDao = db.cleaningHistoryDao()
    @Provides fun provideBatteryHistoryDao(db: AppDatabase): BatteryHistoryDao = db.batteryHistoryDao()
}
