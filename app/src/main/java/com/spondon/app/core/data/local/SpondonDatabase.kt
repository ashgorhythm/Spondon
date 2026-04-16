package com.spondon.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spondon.app.core.data.local.dao.UserDao
import com.spondon.app.core.data.local.entity.UserEntity

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class SpondonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}