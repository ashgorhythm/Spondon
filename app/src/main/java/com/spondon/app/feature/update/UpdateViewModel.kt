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

    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            val info = repository.checkGitHubForUpdate(currentVersion)
            _updateInfo.value = info
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }
}
