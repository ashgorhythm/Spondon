package com.spondon.app.feature.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    /** True while a check is in progress */
    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    /**
     * Tri-state result for manual "Check for Update":
     * - null  → hasn't checked yet
     * - true  → checked and up-to-date
     * - false → checked and update available (info is in [updateInfo])
     */
    private val _isUpToDate = MutableStateFlow<Boolean?>(null)
    val isUpToDate: StateFlow<Boolean?> = _isUpToDate.asStateFlow()

    fun checkForUpdates(currentVersion: String) {
        if (_isChecking.value) return
        viewModelScope.launch {
            _isChecking.value = true
            _isUpToDate.value = null
            val info = repository.checkGitHubForUpdate(currentVersion)
            _updateInfo.value = info
            _isUpToDate.value = (info == null)
            _isChecking.value = false
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    fun clearUpToDateFlag() {
        _isUpToDate.value = null
    }
}
