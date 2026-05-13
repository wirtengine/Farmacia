package com.sanidad.movil.presentation.screens.lotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.LoteResponse
import com.sanidad.movil.data.repository.LoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LotesViewModel(
    private val loteRepository: LoteRepository
) : ViewModel() {
    private val _lotes = MutableStateFlow<List<LoteResponse>>(emptyList())
    val lotes: StateFlow<List<LoteResponse>> = _lotes
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarLotes() }

    fun cargarLotes() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = loteRepository.getLotes()
            result.fold(
                onSuccess = { _lotes.value = it },
                onFailure = { }
            )
            _isLoading.value = false
        }
    }
}