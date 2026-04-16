package com.spondon.app.core.data.repository

import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): Resource<User>
    suspend fun updateUser(user: User): Resource<Unit>
    suspend fun updateFcmToken(token: String): Resource<Unit>
    suspend fun deleteAccount(): Resource<Unit>
    fun observeUser(userId: String): Flow<User>
}