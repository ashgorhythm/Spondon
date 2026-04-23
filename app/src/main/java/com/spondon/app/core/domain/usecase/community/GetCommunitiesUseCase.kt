package com.spondon.app.core.domain.usecase.community

import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.domain.model.Community
import javax.inject.Inject

/** Use case for fetching communities — all or user-scoped. */
class GetCommunitiesUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    /** Fetches all public / discoverable communities. */
    suspend fun getAllCommunities(): Resource<List<Community>> {
        return repository.getCommunities()
    }

    /** Fetches only the communities the given user has joined. */
    suspend fun getMyCommunities(userId: String): Resource<List<Community>> {
        return repository.getMyCommunities(userId)
    }

    /** Fetches a single community by ID. */
    suspend fun getCommunity(communityId: String): Resource<Community> {
        return repository.getCommunity(communityId)
    }
}