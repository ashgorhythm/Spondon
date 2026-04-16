package com.spondon.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val bloodGroup: String = "",
    val isDonor: Boolean = false,
    val avatarUrl: String = "",
    val district: String = "",
)