package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_logs",
    indices = [Index("taskId")]
)
data class ActionLogEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val timestamp: Long,
    val actionType: String,
    val status: String,
    val description: String,
    val metadataJson: String = "{}"
)
