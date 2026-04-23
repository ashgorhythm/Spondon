package com.spondon.app.feature.request

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Constants
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.CommunityRepository
import com.spondon.app.core.data.repository.RequestRepository
import com.spondon.app.core.data.repository.UserRepository
import com.spondon.app.core.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ─── UI State ────────────────────────────────────────────────
data class HomeState(
    val userName: String = "",
    val user: User? = null,
    val communities: List<Community> = emptyList(),
    val selectedCommunityFilter: String? = null, // null = "All"
    val requests: List<BloodRequest> = emptyList(),
    val urgentRequests: List<BloodRequest> = emptyList(),
    val totalDonors: Int = 0,
    val fulfilledRequests: Int = 0,
    val pendingRequests: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class CreateRequestState(
    val bloodGroup: String = "",
    val urgency: Urgency = Urgency.NORMAL,
    val unitsNeeded: Int = 1,
    val patientName: String = "",
    val hospital: String = "",
    val donationDate: Date? = null,
    val contactNumber: String = "",
    val selectedCommunityIds: List<String> = emptyList(),
    val availableCommunities: List<Community> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
)

data class RequestDetailState(
    val request: BloodRequest? = null,
    val requesterName: String = "",
    val requesterPhone: String = "",
    val isCurrentUserRequester: Boolean = false,
    val canDonate: Boolean = false,
    val cooldownDaysRemaining: Int = 0,
    val hasResponded: Boolean = false,
    val isLoading: Boolean = true,
    val isResponding: Boolean = false,
    val error: String? = null,
)

data class FeedState(
    val selectedTab: Int = 0, // 0 = Feed, 1 = My Requests
    val feedRequests: List<BloodRequest> = emptyList(),
    val myRequests: List<BloodRequest> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class RequestViewModel @Inject constructor(
    private val requestRepository: RequestRepository,
    private val communityRepository: CommunityRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    // ─── Home State ──────────────────────────────────────────
    private val _homeState = MutableStateFlow(HomeState())
    val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    // ─── Create Request State ────────────────────────────────
    private val _createState = MutableStateFlow(CreateRequestState())
    val createState: StateFlow<CreateRequestState> = _createState.asStateFlow()

    // ─── Request Detail State ────────────────────────────────
    private val _detailState = MutableStateFlow(RequestDetailState())
    val detailState: StateFlow<RequestDetailState> = _detailState.asStateFlow()

    // ─── Feed State ──────────────────────────────────────────
    private val _feedState = MutableStateFlow(FeedState())
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    init {
        loadHome()
    }

    // ═══════════════════════════════════════════════════════════
    // Home Dashboard
    // ═══════════════════════════════════════════════════════════

    fun loadHome() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, error = null) }

            // Load user
            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            // Load communities
            val communityIds = user?.communityIds ?: emptyList()
            val commResult = if (communityIds.isNotEmpty()) {
                communityRepository.getMyCommunities(currentUserId)
            } else {
                Resource.Success(emptyList())
            }
            val communities = (commResult as? Resource.Success)?.data ?: emptyList()

            // Load requests for joined communities
            val reqResult = if (communityIds.isNotEmpty()) {
                requestRepository.getRequestsForCommunities(communityIds)
            } else {
                Resource.Success(emptyList())
            }
            val allRequests = (reqResult as? Resource.Success)?.data ?: emptyList()
            val activeRequests = allRequests.filter { it.status == RequestStatus.ACTIVE }
            val urgent = activeRequests
                .filter { it.urgency == Urgency.CRITICAL }
                .sortedByDescending { it.createdAt }

            _homeState.update {
                it.copy(
                    userName = user?.name?.split(" ")?.firstOrNull() ?: "User",
                    user = user,
                    communities = communities,
                    requests = activeRequests,
                    urgentRequests = urgent,
                    totalDonors = communities.sumOf { c -> c.memberCount },
                    fulfilledRequests = allRequests.count { r -> r.status == RequestStatus.FULFILLED },
                    pendingRequests = activeRequests.size,
                    isLoading = false,
                )
            }
        }
    }

    fun filterByCommunity(communityId: String?) {
        _homeState.update { state ->
            val filtered = if (communityId == null) {
                state.requests
            } else {
                state.requests.filter { it.communityIds.contains(communityId) }
            }
            state.copy(
                selectedCommunityFilter = communityId,
                urgentRequests = filtered.filter { it.urgency == Urgency.CRITICAL },
            )
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Create Request
    // ═══════════════════════════════════════════════════════════

    fun loadCreateForm() {
        viewModelScope.launch {
            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data

            val commResult = communityRepository.getMyCommunities(currentUserId)
            val communities = (commResult as? Resource.Success)?.data ?: emptyList()

            _createState.update {
                it.copy(
                    contactNumber = user?.phone ?: "",
                    availableCommunities = communities,
                    selectedCommunityIds = communities.map { c -> c.id },
                )
            }
        }
    }

    fun updateBloodGroup(bg: String) = _createState.update { it.copy(bloodGroup = bg) }
    fun updateUrgency(u: Urgency) = _createState.update { it.copy(urgency = u) }
    fun updateUnits(n: Int) = _createState.update { it.copy(unitsNeeded = n.coerceIn(1, 20)) }
    fun updatePatientName(n: String) = _createState.update { it.copy(patientName = n) }
    fun updateHospital(h: String) = _createState.update { it.copy(hospital = h) }
    fun updateDonationDate(d: Date?) = _createState.update { it.copy(donationDate = d) }
    fun updateContactNumber(n: String) = _createState.update { it.copy(contactNumber = n) }

    fun toggleCommunity(id: String) {
        _createState.update { state ->
            val current = state.selectedCommunityIds.toMutableList()
            if (current.contains(id)) current.remove(id) else current.add(id)
            state.copy(selectedCommunityIds = current)
        }
    }

    fun submitRequest() {
        val state = _createState.value
        if (state.bloodGroup.isBlank() || state.hospital.isBlank() || state.selectedCommunityIds.isEmpty()) {
            _createState.update { it.copy(error = "Please fill all required fields") }
            return
        }

        viewModelScope.launch {
            _createState.update { it.copy(isSubmitting = true, error = null) }

            // Calculate expiry: donation date + 24 hours, or 7 days from now
            val expiry = state.donationDate?.let {
                val cal = Calendar.getInstance()
                cal.time = it
                cal.add(Calendar.HOUR, 24)
                cal.time
            } ?: run {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.time
            }

            val request = BloodRequest(
                communityIds = state.selectedCommunityIds,
                requesterId = currentUserId,
                bloodGroup = state.bloodGroup,
                urgency = state.urgency,
                unitsNeeded = state.unitsNeeded,
                patientName = state.patientName.ifBlank { null },
                hospital = state.hospital,
                donationDateTime = state.donationDate,
                contactNumber = state.contactNumber,
                status = RequestStatus.ACTIVE,
                expiresAt = expiry,
            )

            when (val result = requestRepository.createRequest(request)) {
                is Resource.Success -> {
                    _createState.update { it.copy(isSubmitting = false, isSuccess = true) }
                }
                is Resource.Error -> {
                    _createState.update { it.copy(isSubmitting = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetCreateForm() {
        _createState.value = CreateRequestState()
        loadCreateForm()
    }

    // ═══════════════════════════════════════════════════════════
    // Request Detail
    // ═══════════════════════════════════════════════════════════

    fun loadRequestDetail(requestId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, error = null) }

            when (val result = requestRepository.getRequest(requestId)) {
                is Resource.Success -> {
                    val request = result.data

                    // Load requester info
                    val requesterResult = userRepository.getUser(request.requesterId)
                    val requester = (requesterResult as? Resource.Success)?.data

                    // Check current user eligibility
                    val currentUserResult = userRepository.getUser(currentUserId)
                    val currentUser = (currentUserResult as? Resource.Success)?.data

                    val (canDonate, cooldownDays) = checkEligibility(currentUser)

                    _detailState.update {
                        it.copy(
                            request = request,
                            requesterName = requester?.name ?: "Unknown",
                            requesterPhone = requester?.phone ?: "",
                            isCurrentUserRequester = request.requesterId == currentUserId,
                            canDonate = canDonate,
                            cooldownDaysRemaining = cooldownDays,
                            hasResponded = request.respondents.contains(currentUserId),
                            isLoading = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun respondToRequest() {
        val requestId = _detailState.value.request?.id ?: return
        viewModelScope.launch {
            _detailState.update { it.copy(isResponding = true) }
            when (val result = requestRepository.respondToRequest(requestId, currentUserId)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(
                            isResponding = false,
                            hasResponded = true,
                            request = it.request?.copy(
                                respondents = it.request.respondents + currentUserId,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    _detailState.update { it.copy(isResponding = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateStatus(status: RequestStatus) {
        val requestId = _detailState.value.request?.id ?: return
        viewModelScope.launch {
            when (requestRepository.updateRequestStatus(requestId, status)) {
                is Resource.Success -> {
                    _detailState.update {
                        it.copy(request = it.request?.copy(status = status))
                    }
                }
                else -> {}
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Feed
    // ═══════════════════════════════════════════════════════════

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.update { it.copy(isLoading = true, error = null) }

            val userResult = userRepository.getUser(currentUserId)
            val user = (userResult as? Resource.Success)?.data
            val communityIds = user?.communityIds ?: emptyList()

            val feedResult = requestRepository.getRequestsForCommunities(communityIds)
            val feedRequests = (feedResult as? Resource.Success)?.data ?: emptyList()

            val myResult = requestRepository.getMyRequests(currentUserId)
            val myRequests = (myResult as? Resource.Success)?.data ?: emptyList()

            _feedState.update {
                it.copy(
                    feedRequests = feedRequests.sortedWith(
                        compareByDescending<BloodRequest> { r -> r.urgency == Urgency.CRITICAL }
                            .thenByDescending { r -> r.createdAt },
                    ),
                    myRequests = myRequests,
                    isLoading = false,
                )
            }
        }
    }

    fun setFeedTab(tab: Int) = _feedState.update { it.copy(selectedTab = tab) }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════

    private fun checkEligibility(user: User?): Pair<Boolean, Int> {
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

    /** Utility: relative time display */
    companion object {
        fun getRelativeTime(date: Date?): String {
            if (date == null) return ""
            val diff = Date().time - date.time
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> "${days / 7}w ago"
            }
        }
    }
}