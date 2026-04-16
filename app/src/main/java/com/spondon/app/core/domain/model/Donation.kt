package com.spondon.app.core.domain.model

import java.util.Date

data class Donation(
    val id: String = "",
    val requestId: String = "",
    val donorId: String = "",
    val hospital: String = "",
    val bloodGroup: String = "",
    val date: Date? = null,
    val status: DonationStatus = DonationStatus.PENDING,
    val confirmedBy: String? = null,
)