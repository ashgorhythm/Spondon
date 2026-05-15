package com.spondon.app.feature.superadmin.maintenance

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

// ─── Data ────────────────────────────────────────────────────

data class MaintenanceConfig(
    val isEnabled: Boolean = false,
    val title: String = "",
    val message: String = "",
    val estimatedEnd: Date? = null,
    val enabledAt: Date? = null,
    val enabledBy: String = "",
)

// ─── State ───────────────────────────────────────────────────

data class SAMaintenanceState(
    val config: MaintenanceConfig = MaintenanceConfig(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Editor fields
    val editTitle: String = "",
    val editMessage: String = "",
    val editEstimatedMinutes: String = "",

    // Confirmation
    val showConfirmDialog: Boolean = false,
    val confirmPassphrase: String = "",
    val isToggling: Boolean = false,
    val toggleSuccess: String? = null,
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SAMaintenanceViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SAMaintenanceState())
    val state: StateFlow<SAMaintenanceState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getMaintenanceConfig()) {
                is Resource.Success -> {
                    val config = result.data
                    _state.update {
                        it.copy(
                            config = config,
                            editTitle = config.title,
                            editMessage = config.message,
                            isLoading = false,
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateTitle(v: String) = _state.update { it.copy(editTitle = v) }
    fun updateMessage(v: String) = _state.update { it.copy(editMessage = v) }
    fun updateEstimatedMinutes(v: String) = _state.update { it.copy(editEstimatedMinutes = v.filter { c -> c.isDigit() }) }

    fun showConfirm() = _state.update { it.copy(showConfirmDialog = true, confirmPassphrase = "") }
    fun hideConfirm() = _state.update { it.copy(showConfirmDialog = false, confirmPassphrase = "") }
    fun updateConfirmPassphrase(v: String) = _state.update { it.copy(confirmPassphrase = v) }

    fun toggleMaintenance() {
        val s = _state.value
        if (s.confirmPassphrase.length < 6) {
            _state.update { it.copy(error = "Passphrase too short") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isToggling = true, error = null) }

            // Verify passphrase first
            when (saRepository.verifyPassphrase(s.confirmPassphrase)) {
                is Resource.Error -> {
                    _state.update { it.copy(isToggling = false, error = "Invalid passphrase") }
                    return@launch
                }
                else -> {}
            }

            val newEnabled = !s.config.isEnabled
            val estimatedMinutes = s.editEstimatedMinutes.toIntOrNull()

            when (saRepository.setMaintenanceMode(
                enabled = newEnabled,
                title = s.editTitle.ifBlank { if (newEnabled) "আমরা আপগ্রেড করছি" else "" },
                message = s.editMessage.ifBlank { if (newEnabled) "অনুগ্রহ করে কিছুক্ষণ অপেক্ষা করুন" else "" },
                estimatedMinutes = estimatedMinutes,
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isToggling = false,
                            showConfirmDialog = false,
                            toggleSuccess = if (newEnabled) "Maintenance mode ENABLED" else "Maintenance mode DISABLED",
                            config = it.config.copy(
                                isEnabled = newEnabled,
                                title = it.editTitle,
                                message = it.editMessage,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isToggling = false, error = "Failed to toggle maintenance") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearToggleSuccess() = _state.update { it.copy(toggleSuccess = null) }
    fun clearError() = _state.update { it.copy(error = null) }
}
