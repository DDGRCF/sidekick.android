package dev.ddgrcf.sidekick.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ddgrcf.sidekick.core.relay.RelayApi
import dev.ddgrcf.sidekick.core.relay.RelaySocketManager
import dev.ddgrcf.sidekick.core.relay.TokenManager
import dev.ddgrcf.sidekick.core.store.SecretStore
import dev.ddgrcf.sidekick.core.store.SidekickDatabase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SidekickDatabase =
        SidekickDatabase.create(context)

    @Provides
    @Singleton
    fun provideSecretStore(@ApplicationContext context: Context): SecretStore =
        SecretStore.create(context)

    @Provides
    @Singleton
    fun provideRelayApi(client: OkHttpClient): RelayApi = RelayApi(client)

    @Provides
    @Singleton
    fun provideTokenManager(api: RelayApi, secretStore: SecretStore): TokenManager =
        TokenManager(api, secretStore)

    @Provides
    @Singleton
    fun provideRelaySocketManager(
        client: OkHttpClient,
        tokenManager: TokenManager,
        secretStore: SecretStore,
    ): RelaySocketManager = RelaySocketManager(client, tokenManager, secretStore)
}
