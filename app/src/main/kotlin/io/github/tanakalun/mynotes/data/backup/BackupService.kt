package io.github.tanakalun.mynotes.data.backup

import android.content.Context
import android.net.Uri
import io.github.tanakalun.mynotes.data.crypto.BackupCrypto
import io.github.tanakalun.mynotes.data.crypto.LegacyBackupCrypto
import io.github.tanakalun.mynotes.data.crypto.WrongBackupPasswordException
import io.github.tanakalun.mynotes.data.media.ImageUtils
import io.github.tanakalun.mynotes.data.model.CheckItem
import io.github.tanakalun.mynotes.data.model.IMAGE_PATH_REGEX
import io.github.tanakalun.mynotes.data.model.Note
import io.github.tanakalun.mynotes.data.model.NoteType
import io.github.tanakalun.mynotes.data.repository.NoteRepository
import io.github.tanakalun.mynotes.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

enum class BackupFormatKind { V3, LEGACY }

data class ExportResult(
    val exportedNotes: Int,
    val bytes: Int,
)

/**
 * 备份服务：组合笔记仓库 + 设置 + 文件 IO。
 * 全部挂起函数，内部 IO 调度到后台线程；不反向依赖任何仓库以外的单例。
 */
class BackupService(
    private val appContext: Context,
    private val noteRepository: NoteRepository,
    private val settingsStore: SettingsStore,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun readBytes(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read file")
    }

    suspend fun detectFormat(bytes: ByteArray): BackupFormatKind = withContext(Dispatchers.Default) {
        if (BackupCrypto.hasV3Header(bytes)) BackupFormatKind.V3 else BackupFormatKind.LEGACY
    }

    suspend fun previewCount(bytes: ByteArray, password: CharArray?): Int = withContext(Dispatchers.Default) {
        val zipBytes = decryptBackup(bytes, password)
        BackupFormat.fromZip(zipBytes).keys.count { it.startsWith(NOTES_DIR) && it.endsWith(".json") }
    }

    suspend fun export(uri: Uri, password: CharArray): ExportResult = withContext(Dispatchers.IO) {
        val notes = noteRepository.getAllOnce()
        val exportable = notes.filter { !it.unreadable }
        val skipped = notes.size - exportable.size

        val files = linkedMapOf<String, ByteArray>()
        val imageMap = mutableMapOf<String, String>()

        exportable.forEachIndexed { index, note ->
            val noteImages = note.images + IMAGE_PATH_REGEX.findAll(note.content)
                .map { it.groupValues[1] }.toList()
            noteImages.forEachIndexed { imgIdx, path ->
                if (path in imageMap) return@forEachIndexed
                val file = File(path)
                if (file.exists() && file.isFile) {
                    val ext = file.extension.ifEmpty { "jpg" }
                    val name = "img_${index}_$imgIdx.$ext"
                    imageMap[path] = IMAGES_DIR + name
                    files[IMAGES_DIR + name] = file.readBytes()
                }
            }

            val backupNote = BackupNote(
                id = note.id,
                type = note.type.name,
                title = note.title,
                content = note.content,
                checklist = note.checklist.map { BackupCheckItem(it.id, it.text, it.checked) },
                colorIndex = note.colorIndex,
                images = note.images.map { imageMap[it] ?: it },
                pinned = note.pinned,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
            )
            files[NOTES_DIR + note.id + ".json"] = json.encodeToString(backupNote).toByteArray(Charsets.UTF_8)
        }

        files[SETTINGS_FILE] = json.encodeToString(
            BackupSettingsWrapper(exportSettings()),
        ).toByteArray(Charsets.UTF_8)
        files[META_FILE] = json.encodeToString(
            BackupMetadata(createdAt = System.currentTimeMillis()),
        ).toByteArray(Charsets.UTF_8)

        val encrypted = BackupCrypto.encrypt(BackupFormat.toZip(files), password)
        appContext.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(encrypted)
            os.flush()
        } ?: throw IllegalStateException("Cannot write backup file")

        ExportResult(exportedNotes = exportable.size - skipped, bytes = encrypted.size)
    }

    suspend fun import(
        bytes: ByteArray,
        password: CharArray?,
        overwriteSettings: Boolean,
    ): ImportResult = withContext(Dispatchers.IO) {
        val zipBytes = decryptBackup(bytes, password)
        val files = BackupFormat.fromZip(zipBytes)

        val notesToImport = mutableListOf<Note>()
        files.forEach { (name, data) ->
            if (name.startsWith(NOTES_DIR) && name.endsWith(".json")) {
                val backupNote = runCatching {
                    json.decodeFromString<BackupNote>(data.decodeToString())
                }.getOrNull() ?: return@forEach

                val restoredImages = backupNote.images.map { zipPath ->
                    val imgData = files[zipPath]
                    if (imgData != null) {
                        val dir = ImageUtils.getImagesDir(appContext)
                        val ext = File(zipPath).extension.ifEmpty { "jpg" }
                        val target = File(dir, "import_${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext")
                        target.writeBytes(imgData)
                        target.absolutePath
                    } else {
                        zipPath
                    }
                }

                notesToImport.add(
                    Note(
                        id = backupNote.id,
                        type = runCatching { NoteType.valueOf(backupNote.type) }
                            .getOrDefault(NoteType.Note),
                        title = backupNote.title,
                        content = backupNote.content,
                        checklist = backupNote.checklist.map { CheckItem(it.id, it.text, it.checked) },
                        colorIndex = backupNote.colorIndex,
                        images = restoredImages,
                        pinned = backupNote.pinned,
                        createdAt = backupNote.createdAt,
                        updatedAt = backupNote.updatedAt,
                    ),
                )
            }
        }

        if (notesToImport.isNotEmpty()) {
            noteRepository.saveAll(notesToImport)
        }

        if (overwriteSettings) {
            files[SETTINGS_FILE]?.let { data ->
                runCatching {
                    applySettings(json.decodeFromString<BackupSettingsWrapper>(data.decodeToString()).settings)
                }
            }
        }

        ImportResult(imported = notesToImport.size)
    }

    private fun decryptBackup(bytes: ByteArray, password: CharArray?): ByteArray {
        return if (BackupCrypto.hasV3Header(bytes)) {
            if (password == null) throw WrongBackupPasswordException("Password required")
            BackupCrypto.decrypt(bytes, password)
        } else {
            runCatching { LegacyBackupCrypto.decrypt(bytes) }
                .getOrElse { throw WrongBackupPasswordException("Corrupted or unsupported backup") }
        }
    }

    private fun exportSettings(): BackupSettings = BackupSettings(
        colorMode = settingsStore.colorMode,
        keyColorIndex = settingsStore.keyColorIndex,
        paletteStyle = settingsStore.paletteStyle,
        colorSpec = settingsStore.colorSpec,
        enableBlur = settingsStore.enableBlur,
        blurStyle = settingsStore.blurStyle,
        useFloatingNavbar = settingsStore.useFloatingNavbar,
        floatingNavbarStyle = settingsStore.floatingNavbarStyle,
        floatingNavbarPosition = settingsStore.floatingNavbarPosition,
        showSearchBar = settingsStore.showSearchBar,
        enterCreatesItem = settingsStore.enterCreatesItem,
        codeBlockWrap = settingsStore.codeBlockWrap,
        encryptNotes = settingsStore.encryptNotes,
    )

    private fun applySettings(s: BackupSettings) {
        settingsStore.updateColorMode(s.colorMode)
        settingsStore.updateKeyColorIndex(s.keyColorIndex)
        settingsStore.updatePaletteStyle(s.paletteStyle)
        settingsStore.updateColorSpec(s.colorSpec)
        settingsStore.updateEnableBlur(s.enableBlur)
        settingsStore.updateBlurStyle(s.blurStyle)
        settingsStore.updateUseFloatingNavbar(s.useFloatingNavbar)
        settingsStore.updateFloatingNavbarStyle(s.floatingNavbarStyle)
        settingsStore.updateFloatingNavbarPosition(s.floatingNavbarPosition)
        settingsStore.updateShowSearchBar(s.showSearchBar)
        settingsStore.updateEnterCreatesItem(s.enterCreatesItem)
        settingsStore.updateCodeBlockWrap(s.codeBlockWrap)
        settingsStore.updateEncryptNotes(s.encryptNotes)
    }

    private companion object {
        const val META_FILE = "metadata.json"
        const val NOTES_DIR = "notes/"
        const val IMAGES_DIR = "images/"
        const val SETTINGS_FILE = "settings.json"
    }
}
