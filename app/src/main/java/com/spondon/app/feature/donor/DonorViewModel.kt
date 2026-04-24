package com.spondon.app.feature.donor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.data.repository.DonorRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.Community
import com.spondon.app.core.domain.model.Donation
import com.spondon.app.core.domain.model.DonationStatus
import com.spondon.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════
// UI States
// ═══════════════════════════════════════════════════════════════

data class FindDonorState(
    val donors: List<User> = emptyList(),
    val communities: List<Community> = emptyList(),
    val searchQuery: String = "",
    val selectedBloodGroups: List<String> = emptyList(),
    val selectedCommunityId: String? = null,
    val selectedDistrict: String? = null,
    val availableOnly: Boolean = false,
    val sortBy: DonorSortOption = DonorSortOption.MOST_DONATIONS,
    val isLoading: Boolean = false,
    val isFilterSheetVisible: Boolean = false,
    val error: String? = null,
)

enum class DonorSortOption(val label: String) {
    MOST_DONATIONS("Most Donations"),
    RECENTLY_ACTIVE("Recently Active"),
    NAME("Name"),
}

data class DonorProfileState(
    val donor: User? = null,
    val currentUser: User? = null,
    val sharedCommunities: List<Community> = emptyList(),
    val donationHistory: List<Donation> = emptyList(),
    val isAvailable: Boolean = false,
    val cooldownDaysRemaining: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class DonationHistoryState(
    val donations: List<Donation> = emptyList(),
    val totalDonations: Int = 0,
    val nextEligibleDays: Int = 0,
    val isEligibleNow: Boolean = false,
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String, // emoji or icon key
    val criteria: Int, // number of donations required
    val earnedDate: Date? = null,
)

data class AchievementsState(
    val badges: List<Badge> = emptyList(),
    val totalDonations: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DonorViewModel @Inject constructor(
    private val donorRepository: DonorRepository,
    private val userRepository: UserRepository,
    private val communityRepository: CommunityRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    // ─── Find Donor State ────────────────────────────────────
    private val _findState = MutableStateFlow(FindDonorState())
    val findState: StateFlow<FindDonorState> = _findState.asStateFlow()

    // ─── Donor Profile State ─────────────────────────────────
    private val _profileState = MutableStateFlow(DonorProfileState())
    val profileState: StateFlow<DonorProfileState> = _profileState.asStateFlow()

    // ─── Donation History State ──────────────────────────────
    private val _historyState = MutableStateFlow(DonationHistoryState())
    val historyState: StateFlow<DonationHistoryState> = _historyState.asStateFlow()

    // ─── Achievements State ──────────────────────────────────
    private val _achievementsState = MutableStateFlow(AchievementsState())
    val achievementsState: StateFlow<AchievementsState> = _achievementsState.asStateFlow()

    // ═══════════════════════════════════════════════════════════
    // Find Donor
    // ═══════════════════════════════════════════════════════════

    fun loadFindDonor() {
        viewModelScope.launch {
            _findState.update { it.copy(isLoading = true, error = null) }

            // Load user's communities for the filter
            val commResult = communityRepository.getMyCommunities(currentUserId)
            val communities = (commResult as? Resource.Success)?.data ?: emptyList()

            _findState.update { it.copy(communities = communities) }

            // Initial search with no filters
            searchDonors()
        }
    }

    fun searchDonors() {
        viewModelScope.launch {
            _findState.update { it.copy(isLoading = true, error = null) }
            val state = _findState.value

            // Use first selected blood group for Firestore query
            val bloodGroup = state.selectedBloodGroups.firstOrNull()

            val result = donorRepository.searchDonors(
                bloodGroup = bloodGroup,
                communityId = state.selectedCommunityId,
                district = state.selectedDistrict,
                availableOnly = state.availableOnly,
            )

            when (result) {
                is Resource.Success -> {
                    var donors = result.data.filter { it.uid != currentUserId }

                    // Filter by multiple blood groups if more than one selected
                    if (state.selectedBloodGroups.size > 1) {
                        donors = donors.filter { it.bloodGroup in state.selectedBloodGroups }
                    }

                    // Apply search query
                    if (state.searchQuery.isNotBlank()) {
                        val q = state.searchQuery.lowercase()
                        donors = donors.filter {
                            it.name.lowercase().contains(q) ||
                                    it.bloodGroup.lowercase().contains(q) ||
                                    it.district.lowercase().contains(q)
                        }
                    }

                    // Sort
                    donors = when (state.sortBy) {
                        DonorSortOption.MOST_DONATIONS -> donors.sortedByDescending { it.totalDonations }
                        DonorSortOption.RECENTLY_ACTIVE -> donors.sortedByDescending { it.lastDonationDate?.time ?: 0L }
                        DonorSortOption.NAME -> donors.sortedBy { it.name }
                    }

                    _findState.update { it.copy(donors = donors, isLoading = false) }
                }
                is Resource.Error -> {
                    _findState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _findState.update { it.copy(searchQuery = query) }
        searchDonors()
    }

    fun toggleBloodGroupFilter(bg: String) {
        _findState.update { state ->
            val current = state.selectedBloodGroups.toMutableList()
            if (current.contains(bg)) current.remove(bg) else current.add(bg)
            state.copy(selectedBloodGroups = current)
        }
        searchDonors()
    }

    fun setCommunityFilter(communityId: String?) {
        _findState.update { it.copy(selectedCommunityId = communityId) }
        searchDonors()
    }

    fun setDistrictFilter(district: String?) {
        _findState.update { it.copy(selectedDistrict = district) }
        searchDonors()
    }

    fun toggleAvailableOnly() {
        _findState.update { it.copy(availableOnly = !it.availableOnly) }
        searchDonors()
    }

    fun setSortOption(sort: DonorSortOption) {
        _findState.update { it.copy(sortBy = sort) }
        searchDonors()
    }

    fun toggleFilterSheet() {
        _findState.update { it.copy(isFilterSheetVisible = !it.isFilterSheetVisible) }
    }

    // ═══════════════════════════════════════════════════════════
    // Donor Public Profile
    // ═══════════════════════════════════════════════════════════

    fun loadDonorProfile(userId: String) {
        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }

            val donorResult = donorRepository.getDonorProfile(userId)
            val currentUserResult = userRepository.getUser(currentUserId)

            val donor = (donorResult as? Resource.Success)?.data
            val currentUser = (currentUserResult as? Resource.Success)?.data

            if (donor == null) {
                _profileState.update { it.copy(isLoading = false, error = "Donor not found") }
                return@launch
            }

            // Find shared communities
            val sharedIds = donor.communityIds.filter { it in (currentUser?.communityIds ?: emptyList()) }
            val sharedCommunities = mutableListOf<Community>()
            for (id in sharedIds.take(10)) {
                val comm = communityRepository.getCommunity(id)
                if (comm is Resource.Success) sharedCommunities.add(comm.data)
            }

            // Calculate availability
            val (isAvailable, cooldownDays) = checkAvailability(donor)

            // Load public donation history
            val historyResult = donorRepository.getDonationHistory(userId)
            val history = (historyResult as? Resource.Success)?.data
                ?.filter { it.status == DonationStatus.CONFIRMED }
                ?: emptyList()

            _profileState.update {
                it.copy(
                    donor = donor,
                    currentUser = currentUser,
                    sharedCommunities = sharedCommunities,
                    donationHistory = history.take(5), // Only public summary
                    isAvailable = isAvailable,
                    cooldownDaysRemaining = cooldownDays,
                    isLoading = false,
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // My Donation History
    // ═══════════════════════════════════════════════════════════

    fun loadDonationHistory() {
        viewModelScope.launch {
            _historyState.update { it.copy(isLoading = true, error = null) }

            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            val donationsResult = donorRepository.getDonationHistory(currentUserId)
            val donations = (donationsResult as? Resource.Success)?.data ?: emptyList()

            val (isEligible, cooldownDays) = checkAvailability(user)

            _historyState.update {
                it.copy(
                    donations = donations,
                    totalDonations = user?.totalDonations ?: donations.size,
                    nextEligibleDays = cooldownDays,
                    isEligibleNow = isEligible,
                    user = user,
                    isLoading = false,
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Achievements & Badges
    // ═══════════════════════════════════════════════════════════

    fun loadAchievements() {
        viewModelScope.launch {
            _achievementsState.update { it.copy(isLoading = true, error = null) }

            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data
            val totalDonations = user?.totalDonations ?: 0
            val earnedBadges = user?.badges ?: emptyList()

            val allBadges = listOf(
                Badge("first_drop", "First Drop", "Complete your first blood donation", "🩸", 1),
                Badge("life_saver", "Life Saver", "Complete 5 blood donations", "💉", 5),
                Badge("hero_donor", "Hero Donor", "Complete 10 blood donations", "🦸", 10),
                Badge("legend", "Donation Legend", "Complete 25 blood donations", "🏆", 25),
                Badge("champion", "Community Champion", "Complete 50 blood donations", "👑", 50),
                Badge("century", "Century Donor", "Complete 100 blood donations", "💯", 100),
            ).map { badge ->
                if (totalDonations >= badge.criteria && earnedBadges.contains(badge.id)) {
                    badge.copy(earnedDate = user?.createdAt) // Use approximate date
                } else if (totalDonations >= badge.criteria) {
                    badge.copy(earnedDate = Date()) // Newly earned
                } else {
                    badge
                }
            }

            // Auto-award any new badges
            val newBadgeIds = allBadges
                .filter { it.earnedDate != null }
                .map { it.id }
            if (newBadgeIds.toSet() != earnedBadges.toSet() && user != null) {
                viewModelScope.launch {
                    userRepository.updateUser(user.copy(badges = newBadgeIds))
                }
            }

            _achievementsState.update {
                it.copy(
                    badges = allBadges,
                    totalDonations = totalDonations,
                    isLoading = false,
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private fun checkAvailability(user: User?): Pair<Boolean, Int> {
        if (user == null) return false to 0
        if (!user.isDonor) return false to 0

        val lastDonation = user.lastDonationDate ?: return true to 0

        val daysSince = TimeUnit.MILLISECONDS.toDays(
            Date().time - lastDonation.time,
        ).toInt()

        val requiredDays = if (user.availabilityOverride) {
            Constants.MIN_OVERRIDE_DAYS
        } else {
            user.donationInterval
        }

        return if (daysSince >= requiredDays) {
            true to 0
        } else {
            false to (requiredDays - daysSince)
        }
    }

    companion object {
        val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    }
}