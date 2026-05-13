package com.sanidad.movil.presentation.screens.racks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.RackResponse
import com.sanidad.movil.data.repository.RackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RacksViewModel(
    private val rackRepository: RackRepository
) : ViewModel() {
    private val _racks = MutableStateFlow<List<RackResponse>>(emptyList())
    val racks: StateFlow<List<RackResponse>> = _racks
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarRacks() }

    fun cargarRacks() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = rackRepository.getRacks()
            result.fold(
                onSuccess = { _racks.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}