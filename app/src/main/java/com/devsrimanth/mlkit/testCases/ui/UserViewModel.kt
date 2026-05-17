package com.devsrimanth.mlkit.testCases.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devsrimanth.mlkit.testCases.model.User
import com.devsrimanth.mlkit.testCases.repository.StoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repository: StoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<User>>> = _uiState.asStateFlow()

    init { getUsers() }

    fun getUsers() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getUsers()
            _uiState.value = when {
                result.isSuccess -> UiState.Success(result.getOrDefault(emptyList()))
                else -> UiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
}


sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}