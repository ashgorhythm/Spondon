package com.spondon.app.core.domain.usecase.donor

import com.spondon.app.core.data.repository.DonorRepository
import javax.inject.Inject

/** Use case for checking donation eligibility. */
class CheckEligibilityUseCase @Inject constructor(
    private val repository: DonorRepository,
) {
    // TODO: Implement in corresponding phase
}