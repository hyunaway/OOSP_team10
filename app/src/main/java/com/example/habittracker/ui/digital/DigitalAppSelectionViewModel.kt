package com.example.habittracker.ui.digital

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.habittracker.data.local.UserPreferenceManager
import com.example.habittracker.data.usage.InstalledAppInfo
import com.example.habittracker.data.usage.InstalledAppProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DigitalAppSelectionUiState(
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val loading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class DigitalAppSelectionViewModel @Inject constructor(
    private val userPreferenceManager: UserPreferenceManager,
    private val installedAppProvider: InstalledAppProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DigitalAppSelectionUiState())
    val uiState: StateFlow<DigitalAppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
        viewModelScope.launch {
            userPreferenceManager.selectedDigitalPackagesFlow
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { packages ->
                    _uiState.update { it.copy(selectedPackages = packages) }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun togglePackage(packageName: String) {
        val updated = _uiState.value.selectedPackages.toMutableSet().apply {
            if (!add(packageName)) remove(packageName)
        }
        _uiState.update { it.copy(selectedPackages = updated) }
        viewModelScope.launch {
            userPreferenceManager.updateSelectedDigitalPackages(updated)
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val apps = installedAppProvider.getLaunchableApps()
                _uiState.update { it.copy(installedApps = apps, loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, errorMessage = e.message) }
            }
        }
    }
}
