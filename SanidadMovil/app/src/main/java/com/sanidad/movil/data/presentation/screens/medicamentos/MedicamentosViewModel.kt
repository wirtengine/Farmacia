package com.sanidad.movil.presentation.screens.medicamentos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.dto.MedicamentoResponse
import com.sanidad.movil.data.repository.MedicamentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MedicamentosViewModel(
    private val medicamentoRepository: MedicamentoRepository
) : ViewModel() {

    private val _medicamentos = MutableStateFlow<List<MedicamentoResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoResponse>> = _medicamentos

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { cargarMedicamentos() }

    fun cargarMedicamentos() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = medicamentoRepository.getMedicamentos()
            result.fold(
                onSuccess = { _medicamentos.value = it },
                onFailure = { /* manejar error */ }
            )
            _isLoading.value = false
        }
    }
}