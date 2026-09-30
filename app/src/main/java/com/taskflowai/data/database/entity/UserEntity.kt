package com.taskflowai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val isGoogleConnected: Boolean,
    val isCalendarConnected: Boolean,
    val lastLoginAt: Long
)
