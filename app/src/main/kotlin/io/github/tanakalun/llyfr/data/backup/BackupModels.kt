package io.github.tanakalun.llyfr.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupMetadata(
    val version: Int = 3,
    val appVersion: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class BackupCheckItem(
    val id: String,
    val text: String,
    val checked: Boolean,
)

@Serializable
data class BackupNote(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val checklist: List<BackupCheckItem>,
    val colorIndex: Int,
    val images: List<String>,
    val pinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSettings(
    val colorMode: Int = 0,
    val keyColorIndex: Int = 0,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 0,
    val enableBlur: Boolean = true,
    val blurStyle: Int = 0,
    val useFloatingNavbar: Boolean = false,
    val floatingNavbarStyle: Int = 0,
    val floatingNavbarPosition: Int = 0,
    val showSearchBar: Boolean = true,
    val enterCreatesItem: Boolean = false,
    val codeBlockWrap: Boolean = true,
    val encryptNotes: Boolean = true,
)

@Serializable
data class BackupSettingsWrapper(
    val settings: BackupSettings,
)

@Serializable
data class ImportResult(
    val imported: Int,
    val skippedUnreadable: Int = 0,
)