package com.spondon.app.feature.superadmin.forceupdate

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
import javax.inject.Inject

// ─── Data ────────────────────────────────────────────────────

data class ForceUpdateConfig(
    val minimumVersionCode: Int = 0,
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val playStoreUrl: String = "",
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false,
)

// ─── State ───────────────────────────────────────────────────

data class SAForceUpdateState(
    val config: ForceUpdateConfig = ForceUpdateConfig(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Editor fields
    val editMinVersionCode: String = "",
    val editLatestVersionCode: String = "",
    val editLatestVersionName: String = "",
    val editPlayStoreUrl: String = "",
    val editReleaseNotes: String = "",
    val editIsForceUpdate: Boolean = false,

    // Save
    val isSaving: Boolean = false,
    val saveSuccess: String? = null,
)

// ─── ViewModel ───────────────────────────────────────────────

@HiltViewModel
class SAForceUpdateViewModel @Inject constructor(
    private val saRepository: SARepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SAForceUpdateState())
    val state: StateFlow<SAForceUpdateState> = _state.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = saRepository.getForceUpdateConfig()) {
                is Resource.Success -> {
                    val c = result.data
                    _state.update {
                        it.copy(
                            config = c,
                            editMinVersionCode = "${c.minimumVersionCode}",
                            editLatestVersionCode = "${c.latestVersionCode}",
                            editLatestVersionName = c.latestVersionName,
                            editPlayStoreUrl = c.playStoreUrl,
                            editReleaseNotes = c.releaseNotes,
                            editIsForceUpdate = c.isForceUpdate,
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

    fun updateMinVersionCode(v: String) = _state.update { it.copy(editMinVersionCode = v.filter { c -> c.isDigit() }) }
    fun updateLatestVersionCode(v: String) = _state.update { it.copy(editLatestVersionCode = v.filter { c -> c.isDigit() }) }
    fun updateLatestVersionName(v: String) = _state.update { it.copy(editLatestVersionName = v) }
    fun updatePlayStoreUrl(v: String) = _state.update { it.copy(editPlayStoreUrl = v) }
    fun updateReleaseNotes(v: String) = _state.update { it.copy(editReleaseNotes = v) }
    fun toggleForceUpdate() = _state.update { it.copy(editIsForceUpdate = !it.editIsForceUpdate) }

    fun saveConfig() {
        val s = _state.value
        val minCode = s.editMinVersionCode.toIntOrNull() ?: 0
        val latestCode = s.editLatestVersionCode.toIntOrNull() ?: 0

        if (latestCode < minCode) {
            _state.update { it.copy(error = "Latest version code must be >= minimum") }
            return
        }
        if (s.editLatestVersionName.isBlank()) {
            _state.update { it.copy(error = "Version name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (saRepository.setForceUpdateConfig(
                minimumVersionCode = minCode,
                latestVersionCode = latestCode,
                latestVersionName = s.editLatestVersionName,
                playStoreUrl = s.editPlayStoreUrl,
                releaseNotes = s.editReleaseNotes,
                isForceUpdate = s.editIsForceUpdate,
            )) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = "Force update config saved",
                            config = ForceUpdateConfig(
                                minimumVersionCode = minCode,
                                latestVersionCode = latestCode,
                                latestVersionName = s.editLatestVersionName,
                                playStoreUrl = s.editPlayStoreUrl,
                                releaseNotes = s.editReleaseNotes,
                                isForceUpdate = s.editIsForceUpdate,
                            ),
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isSaving = false, error = "Failed to save config") }
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun clearSaveSuccess() = _state.update { it.copy(saveSuccess = null) }
    fun clearError() = _state.update { it.copy(error = null) }
}
