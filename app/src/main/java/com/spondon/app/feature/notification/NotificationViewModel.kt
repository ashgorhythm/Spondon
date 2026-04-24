package com.spondon.app.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.spondon.app.core.common.Resource
import com.spondon.app.core.data.repository.NotificationRepository
import com.spondon.app.core.domain.model.AppNotification
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val currentUserId get() = auth.currentUser?.uid ?: ""

    private val _state = MutableStateFlow(NotificationState())
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    init {
        observeUnread()
    }

    private fun observeUnread() {
        viewModelScope.launch {
            notificationRepository.observeUnreadCount(currentUserId)
                .collect { count ->
                    _state.update { it.copy(unreadCount = count) }
                }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            when (val result = notificationRepository.getNotifications(currentUserId)) {
                is Resource.Success -> {
                    _state.update { it.copy(notifications = result.data, isLoading = false) }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
            _state.update { s ->
                s.copy(notifications = s.notifications.map { n ->
                    if (n.id == notificationId) n.copy(isRead = true) else n
                })
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead(currentUserId)
            _state.update { s ->
                s.copy(notifications = s.notifications.map { it.copy(isRead = true) })
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
            _state.update { s ->
                s.copy(notifications = s.notifications.filter { it.id != notificationId })
            }
        }
    }
}