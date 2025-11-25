package it.fast4x.riplay.extensions.databasebackup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.fast4x.riplay.extensions.audiotag.AudioTagViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

sealed class BackupUiState {
    object Idle : BackupUiState()
    object BackingUp : BackupUiState()
    object Restoring : BackupUiState()
    data class Success(val message: String) : BackupUiState()
    data class Error(val message: String) : BackupUiState()
}

class BackupViewModel(
    private val backupManager: DatabaseBackupManager,
    private val context: Context
) : ViewModel(), ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BackupViewModel(backupManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun performBackup(backupUri: Uri?) {
        if (backupUri == null) {
            _uiState.value = BackupUiState.Error("Please select a right file.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BackupUiState.BackingUp
            try {
                backupManager.backupDatabase(backupUri)
                _uiState.value = BackupUiState.Success("Backup completed!")
            } catch (e: IOException) {
                _uiState.value = BackupUiState.Error("Backup error: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Unknown backup error: ${e.message}")
            }
        }
    }

    fun performRestore(restoreUri: Uri?) {
        if (restoreUri == null) {
            _uiState.value = BackupUiState.Error("Please select a right file.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BackupUiState.Restoring
            try {
                backupManager.restoreDatabase(restoreUri)
                _uiState.value = BackupUiState.Success("Database restored, wait app will restart...")
                delay(5000)
                val packageManager = context.packageManager
                val launchIntent = packageManager.getLaunchIntentForPackage(context.packageName)
                val restartIntent = Intent.makeRestartActivityTask(launchIntent?.component)
                context.startActivity(restartIntent)
                Runtime.getRuntime().exit(0)
            } catch (e: IOException) {
                _uiState.value = BackupUiState.Error("Restore error: ${e.message}")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Unknown restore error: ${e.message}")
            }
        }
    }

    fun clearState() {
        _uiState.value = BackupUiState.Idle
    }
}