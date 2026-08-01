package com.nicholasbergesen.gunsout.di

import com.nicholasbergesen.gunsout.data.repo.CreatineReminderUpdater
import com.nicholasbergesen.gunsout.feature.creatine.CreatineReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CreatineReminderModule {
    @Binds
    @Singleton
    abstract fun bindCreatineReminderUpdater(
        scheduler: CreatineReminderScheduler
    ): CreatineReminderUpdater
}
