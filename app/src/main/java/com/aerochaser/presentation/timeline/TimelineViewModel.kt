package com.aerochaser.presentation.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.usecase.GetPhotosUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimelineViewModel(private val getPhotosUseCase: GetPhotosUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow<TimelineUiState>(TimelineUiState.Loading)
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            try {
                val photos = getPhotosUseCase()
                _uiState.value = TimelineUiState.Success(photos)
            } catch (e: Exception) {
                _uiState.value = TimelineUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface TimelineUiState {
    object Loading : TimelineUiState
    data class Success(val photos: List<PhotoMetadata>) : TimelineUiState
    data class Error(val message: String) : TimelineUiState
}
