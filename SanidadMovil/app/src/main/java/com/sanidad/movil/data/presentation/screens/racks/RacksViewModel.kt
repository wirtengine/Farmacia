package com.sanidad.movil.presentation.screens.racks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.RackResponse
import com.sanidad.movil.data.repository.RackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RacksUiState(
    val racks: List<RackResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RacksViewModel(
    private val rackRepository: RackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RacksUiState(isLoading = true))
    val uiState: StateFlow<RacksUiState> = _uiState

    init { cargarRacks() }

    fun cargarRacks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = rackRepository.getRacks()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        racks = result.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}