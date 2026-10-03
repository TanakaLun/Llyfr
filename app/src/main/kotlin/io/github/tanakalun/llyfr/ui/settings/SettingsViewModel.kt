package io.github.tanakalun.llyfr.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tanakalun.llyfr.R
import io.github.tanakalun.llyfr.core.ErrorReporter
import io.github.tanakalun.llyfr.data.backup.BackupFormatKind
import io.github.tanakalun.llyfr.data.backup.BackupService
import io.github.tanakalun.llyfr.data.crypto.WrongBackupPasswordException
import io.github.tanakalun.llyfr.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface BackupDialogState {
    data object None : BackupDialogState
    data object ExportPassword : BackupDialogState
    data class ImportPassword(val bytes: ByteArray) : BackupDialogState
    data class ImportConfirm(
        val bytes: ByteArray,
        val count: Int,
        val password: CharArray?,
    ) : BackupDialogState
}

enum class BusyOp { Export, Import }

data class SettingsUiState(
    val busyOp: BusyOp? = null,
    val backupDialog: BackupDialogState = BackupDialogState.None,
)

class SettingsViewModel(
    private val backupService: BackupService,
    private val errorReporter: ErrorReporter,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    /** 导出路径还未选：先存口令，等 SAF 返回 uri 后再执行导出。 */
    private var pendingExportPassword: CharArray? = null

    fun onAskExportPassword() {
        _uiState.update { it.copy(backupDialog = BackupDialogState.ExportPassword) }
    }

    fun onExportPasswordFilled(password: CharArray) {
        pendingExportPassword = password
        _uiState.update { it.copy(backupDialog = BackupDialogState.None) }
    }

    /** SAF 选择器被取消：清空暂存口令。 */
    fun onExportCancelled() {
        pendingExportPassword?.fill('\u0000')
        pendingExportPassword = null
    }

    fun onExportUri(uri: Uri) {
        val password = pendingExportPassword ?: run {
            _uiState.update { it.copy(backupDialog = BackupDialogState.ExportPassword) }
            return
        }
        pendingExportPassword = null
        _uiState.update { it.copy(busyOp = BusyOp.Export, backupDialog = BackupDialogState.None) }
        viewModelScope.launch {
            try {
                val result = backupService.export(uri, password)
                _messages.emit(appContext.getString(R.string.export_success, result.bytes))
            } catch (e: Exception) {
                val report = errorReporter.report("LlyfrBackup", e, uri)
                _messages.emit(
                    appContext.getString(R.string.export_failed, report.message, report.logPath ?: ""),
                )
            } finally {
                password.fill('\u0000')
                _uiState.update { it.copy(busyOp = null) }
            }
        }
    }

    fun onImportUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = backupService.readBytes(uri)
                when (backupService.detectFormat(bytes)) {
                    BackupFormatKind.V3 -> {
                        _uiState.update { it.copy(backupDialog = BackupDialogState.ImportPassword(bytes)) }
                    }
                    BackupFormatKind.LEGACY -> {
                        val count = backupService.previewCount(bytes, null)
                        _uiState.update {
                            it.copy(backupDialog = BackupDialogState.ImportConfirm(bytes, count, null))
                        }
                    }
                }
            } catch (e: Exception) {
                val report = errorReporter.report("LlyfrBackup", e, uri)
                _messages.emit(appContext.getString(R.string.export_failed, report.message, report.logPath ?: ""))
            }
        }
    }

    fun onImportPasswordConfirmed(password: CharArray) {
        val bytes = (_uiState.value.backupDialog as? BackupDialogState.ImportPassword)?.bytes ?: return
        _uiState.update { it.copy(busyOp = BusyOp.Import, backupDialog = BackupDialogState.None) }
        viewModelScope.launch {
            try {
                val count = backupService.previewCount(bytes, password)
                _uiState.update {
                    it.copy(
                        busyOp = null,
                        backupDialog = BackupDialogState.ImportConfirm(bytes, count, password),
                    )
                }
            } catch (e: WrongBackupPasswordException) {
                _messages.emit(appContext.getString(R.string.backup_wrong_password))
                _uiState.update { it.copy(busyOp = null) }
            } catch (e: Exception) {
                _messages.emit(appContext.getString(R.string.backup_wrong_password))
                _uiState.update { it.copy(busyOp = null) }
            }
        }
    }

    fun onImportConfirm(overwriteSettings: Boolean) {
        val dialog = _uiState.value.backupDialog as? BackupDialogState.ImportConfirm ?: return
        val password = dialog.password
        _uiState.update { it.copy(busyOp = BusyOp.Export, backupDialog = BackupDialogState.None) }
        viewModelScope.launch {
            try {
                val result = backupService.import(dialog.bytes, password, overwriteSettings)
                _messages.emit(appContext.getString(R.string.imported_notes, result.imported))
            } catch (e: Exception) {
                val report = errorReporter.report("LlyfrBackup", e, null)
                _messages.emit(appContext.getString(R.string.export_failed, report.message, report.logPath ?: ""))
            } finally {
                password?.fill('\u0000')
                _uiState.update { it.copy(busyOp = null) }
            }
        }
    }

    fun dismissBackupDialog() {
        val dialog = _uiState.value.backupDialog
        if (dialog is BackupDialogState.ImportConfirm) dialog.password?.fill('\u0000')
        _uiState.update { it.copy(backupDialog = BackupDialogState.None) }
    }

    fun updateColorMode(value: Int) = SettingsStore.updateColorMode(value)
    fun updateKeyColorIndex(value: Int) = SettingsStore.updateKeyColorIndex(value)
    fun updatePaletteStyle(value: Int) = SettingsStore.updatePaletteStyle(value)
    fun updateColorSpec(value: Int) = SettingsStore.updateColorSpec(value)
    fun updateEnableBlur(value: Boolean) = SettingsStore.updateEnableBlur(value)
    fun updateBlurStyle(value: Int) = SettingsStore.updateBlurStyle(value)
    fun updateUseFloatingNavbar(value: Boolean) = SettingsStore.updateUseFloatingNavbar(value)
    fun updateFloatingNavbarStyle(value: Int) = SettingsStore.updateFloatingNavbarStyle(value)
    fun updateFloatingNavbarPosition(value: Int) = SettingsStore.updateFloatingNavbarPosition(value)
    fun updateShowSearchBar(value: Boolean) = SettingsStore.updateShowSearchBar(value)
    fun updateEnterCreatesItem(value: Boolean) = SettingsStore.updateEnterCreatesItem(value)
    fun updateCodeBlockWrap(value: Boolean) = SettingsStore.updateCodeBlockWrap(value)
    fun updateEncryptNotes(value: Boolean) = SettingsStore.updateEncryptNotes(value)
    fun updateSaveOnBack(value: Boolean) = SettingsStore.updateSaveOnBack(value)
    fun updateSwipeBackEnabled(value: Boolean) = SettingsStore.updateSwipeBackEnabled(value)
    fun updateLanguage(value: Int) = SettingsStore.updateLanguage(appContext, value)
}