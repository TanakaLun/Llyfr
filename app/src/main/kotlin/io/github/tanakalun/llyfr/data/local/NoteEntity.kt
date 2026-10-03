package io.github.tanakalun.llyfr.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "type", defaultValue = "0")
    val type: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "checklist", defaultValue = "'[]'")
    val checklist: String,

    @ColumnInfo(name = "color_index", defaultValue = "0")
    val colorIndex: Int,

    @ColumnInfo(name = "images", defaultValue = "'[]'")
    val images: String,

    @ColumnInfo(name = "pinned", defaultValue = "0")
    val pinned: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "unreadable", defaultValue = "0")
    val unreadable: Int,
)