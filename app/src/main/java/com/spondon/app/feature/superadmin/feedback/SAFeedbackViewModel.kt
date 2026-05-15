package com.spondon.app.feature.superadmin.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spondon.app.core.common.Resource
import com.spondon.app.feature.superadmin.data.SARepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

// ─── Data Model ──────────────────────────────────────────────

data class SAFeedbackItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val type: String = "OTHER",
    val body: String = "",
    val screenshotUrl: String? = null,
    val appVersion: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val status: String = "UNREAD",
    val createdAt: Date? = null,
)

// ─── State ───────────────────────────────────────────────────

data class SAFeedbackState(
    val feedbackItems: List<SAFeedbackItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Filters
    val typeFilter: String? = null,       // null = all
    val statusFilter: String? = null,     // null = all

    // Detail
    val selectedFeedback: SAFeedbackItem? = null,
    val showDetail: Boolean = false,

    // Reply
    val replyText: String = "",
    val isSendingReply: Boolean = false,
    val replySent: Boolean = false,

    // Action
    val actionSuccess: String? = null,
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SAFeedbackViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SAFeedbackState())
    val state: StateFlow<SAFeedbackState> = _state.asStateFlow()

    init {
        loadFeedback()
    }

    fun loadFeedback() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getAllFeedback()) {
                is Resource.Success -> {
                    _state.update { it.copy(feedbackItems = result.data, isLoading = false) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Filters ──────────────────────────────────────────

    fun setTypeFilter(type: String?) = _state.update { it.copy(typeFilter = type) }
    fun setStatusFilter(status: String?) = _state.update { it.copy(statusFilter = status) }

    fun filteredItems(): List<SAFeedbackItem> {
        val s = _state.value
        return s.feedbackItems.filter { item ->
            (s.typeFilter == null || item.type == s.typeFilter) &&
                    (s.statusFilter == null || item.status == s.statusFilter)
        }
    }

    // ─── Detail ───────────────────────────────────────────

    fun selectFeedback(item: SAFeedbackItem) {
        _state.update { it.copy(selectedFeedback = item, showDetail = true, replyText = "", replySent = false) }
        // Mark as read if unread
        if (item.status == "UNREAD") {
            viewModelScope.launch {
                saRepository.updateFeedbackStatus(item.id, "READ")
                // Update local state
                _state.update { state ->
                    val updated = state.feedbackItems.map {
                        if (it.id == item.id) it.copy(status = "READ") else it
                    }
                    state.copy(
                        feedbackItems = updated,
                        selectedFeedback = state.selectedFeedback?.copy(status = "READ"),
                    )
                }
            }
        }
    }

    fun dismissDetail() = _state.update { it.copy(showDetail = false, selectedFeedback = null) }

    // ─── Reply ────────────────────────────────────────────

    fun updateReply(text: String) = _state.update { it.copy(replyText = text) }

    fun sendReply() {
        val s = _state.value
        val feedback = s.selectedFeedback ?: return
        if (s.replyText.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSendingReply = true) }
            when (saRepository.sendNotificationToUser(
                uid = feedback.userId,
                title = "Feedback Reply — ${feedback.type}",
                body = s.replyText,
                type = "INFO",
            )) {
                is Resource.Success -> {
                    _state.update { it.copy(isSendingReply = false, replySent = true, replyText = "") }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSendingReply = false) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    // ─── Status Actions ───────────────────────────────────

    fun markResolved(feedbackId: String) {
        viewModelScope.launch {
            saRepository.updateFeedbackStatus(feedbackId, "RESOLVED")
            _state.update { state ->
                val updated = state.feedbackItems.map {
                    if (it.id == feedbackId) it.copy(status = "RESOLVED") else it
                }
                state.copy(
                    feedbackItems = updated,
                    selectedFeedback = state.selectedFeedback?.let {
                        if (it.id == feedbackId) it.copy(status = "RESOLVED") else it
                    },
                    actionSuccess = "Marked as resolved",
                )
            }
        }
    }

    fun markSpam(feedbackId: String) {
        viewModelScope.launch {
            saRepository.updateFeedbackStatus(feedbackId, "SPAM")
            _state.update { state ->
                val updated = state.feedbackItems.map {
                    if (it.id == feedbackId) it.copy(status = "SPAM") else it
                }
                state.copy(
                    feedbackItems = updated,
                    actionSuccess = "Marked as spam",
                )
            }
        }
    }

    fun clearActionSuccess() = _state.update { it.copy(actionSuccess = null) }
}
