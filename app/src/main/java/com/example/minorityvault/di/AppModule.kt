package com.example.minorityvault.di

import com.example.minorityvault.hotseat.SeedSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun seedSource(): SeedSource {
        val random = SecureRandom()
        return SeedSource { random.nextLong() }
    }
}
