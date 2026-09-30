package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggestions")
data class SuggestionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val actionCommand: String,
    val type: String,
    val isDismissed: Boolean,
    val createdAt: Long
)
