package com.spondon.app.core.data.repository

import com.spondon.app.core.common.Resource
import com.spondon.app.core.domain.model.Donation
import com.spondon.app.core.domain.model.User

interface DonorRepository {
    suspend fun searchDonors(bloodGroup: String?, communityId: String?, district: String?, availableOnly: Boolean): Resource<List<User>>
    suspend fun getDonorProfile(userId: String): Resource<User>
    suspend fun getDonationHistory(userId: String): Resource<List<Donation>>
    suspend fun recordDonation(donation: Donation): Resource<Unit>
    suspend fun overrideAvailability(userId: String, adminId: String): Resource<Unit>
}