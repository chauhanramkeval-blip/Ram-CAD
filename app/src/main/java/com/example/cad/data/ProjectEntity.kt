package com.example.cad.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "cad_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val folderPath: String,
    val createdTimestamp: Long,
    val lastModifiedTimestamp: Long,
    val drawingsCount: Int = 0,
    val isFavorite: Boolean = false
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModifiedTimestamp))
        }
}
