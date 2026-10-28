package dev.jotalac.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import dev.jotalac.core.data.DATA_STORE_FILE_NAME
import dev.jotalac.core.database.AppDatabase
import dev.jotalac.core.database.getDatabaseBuilder
import dev.jotalac.feature.notebooks_management.domain.IosNotebookPathProvider
import dev.jotalac.feature.notebooks_management.domain.NotebookPathProvider
import kotlinx.cinterop.ExperimentalForeignApi
import dev.jotalac.core.utils.NetworkMonitor
import dev.jotalac.core.utils.DeviceNetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import dev.jotalac.core.data.createDataStore
import dev.jotalac.feature.git_sync.data.LibGit2SyncRepositoryImpl
import dev.jotalac.feature.git_sync.data.LibGit2MailboxRepositoryImpl
import dev.jotalac.feature.git_sync.data.LibGit2MirrorRepositoryImpl
import dev.jotalac.feature.git_sync.domain.MailboxRepository
import dev.jotalac.feature.git_sync.domain.MirrorRepository
import dev.jotalac.feature.git_sync.domain.GitSyncRepository


@OptIn(ExperimentalForeignApi::class)
actual val platformModule: Module = module {
    single<NetworkMonitor> { DeviceNetworkMonitor() }

    single<NotebookPathProvider> { IosNotebookPathProvider() }

    single<RoomDatabase.Builder<AppDatabase>> {
        getDatabaseBuilder()
    }

    single<DataStore<Preferences>> {
        createDataStore {
            val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )

            val dirPath = requireNotNull(documentDirectory?.path) {
                "Failed to get iOS Document Directory"
            }

            "$dirPath/$DATA_STORE_FILE_NAME"
        }
    }

    single<MirrorRepository> { LibGit2MirrorRepositoryImpl() }
    single<MailboxRepository> { LibGit2MailboxRepositoryImpl() }

    single<GitSyncRepository> {
        LibGit2SyncRepositoryImpl()
    }
}