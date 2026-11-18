package com.vaultaire.android.sync

import android.content.Context
import com.vaultaire.android.backup.ZeroKnowledgeBackupEngine
import com.vaultaire.android.sync.engine.GarbageCollector
import com.vaultaire.android.sync.engine.MergeEngine
import com.vaultaire.android.sync.engine.SyncEncryptionEngine
import com.vaultaire.android.sync.engine.SyncFormat
import com.vaultaire.android.sync.engine.VectorClock
import com.vaultaire.android.sync.model.SyncMetadata
import com.vaultaire.android.sync.transport.ChangeWatcher
import com.vaultaire.android.sync.transport.DriveAuthManager
import com.vaultaire.android.sync.transport.DriveSyncTransport
import com.vaultaire.android.sync.transport.ResumableUploader
import com.vaultaire.android.sync.worker.NetworkMonitor
import com.vaultaire.android.sync.worker.SyncScheduler
import com.vaultaire.android.sync.worker.SyncWorker
import com.vaultaire.android.storage.VaultStorageEngine
import com.vaultaire.android.storage.VaultStorageEngineImpl
import com.vaultaire.android.vault.VaultManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for sync feature dependencies.
 * Provides all sync engine, transport, worker, and UI components.
 */
@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncJson(): Json {
        return Json {
            ignoreUnknownKeys = true
        }
    }

    @Provides
    @Singleton
    fun provideVectorClock(): VectorClock = VectorClock

    @Provides
    @Singleton
    fun provideMergeEngine(): MergeEngine = MergeEngine()

    @Provides
    @Singleton
    fun provideGarbageCollector(): GarbageCollector = GarbageCollector

    @Provides
    @Singleton
    fun provideSyncFormat(): SyncFormat = SyncFormat

    @Provides
    @Singleton
    fun provideResumableUploader(): ResumableUploader = ResumableUploader()

    @Provides
    @Singleton
    fun provideVaultStorageEngine(context: Context): VaultStorageEngine = VaultStorageEngineImpl(context)

    @Provides
    @Singleton
    fun provideVaultManager(context: Context): VaultManager = VaultManager(context)

    @Provides
    @Singleton
    fun provideDriveAuthManager(
        context: Context
    ): DriveAuthManager = DriveAuthManager(context)

    @Provides
    @Singleton
    fun provideZeroKnowledgeBackupEngine(): ZeroKnowledgeBackupEngine = ZeroKnowledgeBackupEngine()

    @Provides
    @Singleton
    fun provideDriveSyncTransport(
        authManager: DriveAuthManager,
        backupEngine: ZeroKnowledgeBackupEngine
    ): DriveSyncTransport = DriveSyncTransport(authManager, backupEngine)

    @Provides
    @Singleton
    fun provideSyncScheduler(context: Context): SyncScheduler = SyncScheduler(context)

    @Provides
    @Singleton
    fun provideChangeWatcher(
        context: Context,
        authManager: DriveAuthManager
    ): ChangeWatcher = ChangeWatcher(context, authManager)

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        context: Context,
        syncScheduler: SyncScheduler,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ): NetworkMonitor = NetworkMonitor(context, syncScheduler, CoroutineScope(ioDispatcher + SupervisorJob()))
}