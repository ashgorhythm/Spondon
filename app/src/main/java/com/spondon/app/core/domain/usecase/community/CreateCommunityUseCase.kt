package com.spondon.app.core.domain.usecase.community

import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.domain.model.Community
import javax.inject.Inject

/** Use case for creating a new community. The creator is auto-assigned as admin. */
class CreateCommunityUseCase @Inject constructor(
    private val repository: CommunityRepository,
) {
    suspend operator fun invoke(community: Community): Resource<String> {
        return repository.createCommunity(community)
    }
}