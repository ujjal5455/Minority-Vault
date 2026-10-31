package com.example.minorityvault.di

import android.content.Context
import com.example.minorityvault.hotseat.SeedSource
import com.example.minorityvault.lobby.TransportFactory
import com.example.minorityvault.net.NearbyTransport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    fun transportFactory(@ApplicationContext context: Context): TransportFactory =
        TransportFactory { NearbyTransport(context) }
}
