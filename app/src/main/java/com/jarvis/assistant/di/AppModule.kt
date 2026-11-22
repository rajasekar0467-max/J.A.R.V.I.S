package com.jarvis.assistant.di

import android.content.Context
import com.jarvis.assistant.core.AppDispatchers
import com.jarvis.assistant.security.AppDataResetManager
import com.jarvis.assistant.security.AuditLog
import com.jarvis.assistant.security.KeystoreManager
import com.jarvis.assistant.security.SecretStore
import com.jarvis.assistant.settings.SettingsRepository
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
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers()

    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager = KeystoreManager()

    @Provides
    @Singleton
    fun provideSecretStore(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager,
        auditLog: AuditLog
    ): SecretStore = SecretStore(context, keystoreManager, auditLog)

    @Provides
    @Singleton
    fun provideAuditLog(@ApplicationContext context: Context): AuditLog = AuditLog(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    @Singleton
    fun provideAppDataResetManager(
        @ApplicationContext context: Context,
        secretStore: SecretStore,
        settingsRepository: SettingsRepository,
        auditLog: AuditLog
    ): AppDataResetManager = AppDataResetManager(context, secretStore, settingsRepository, auditLog)
}
