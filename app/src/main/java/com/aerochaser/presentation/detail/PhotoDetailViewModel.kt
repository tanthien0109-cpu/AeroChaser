package com.aerochaser.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoDetailViewModel(
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _photo = MutableStateFlow<PhotoMetadata?>(null)
    val photo: StateFlow<PhotoMetadata?> = _photo.asStateFlow()

    fun loadPhoto(photoId: String) {
        viewModelScope.launch {
            _photo.value = photoRepository.getPhotoById(photoId)
        }
    }
}
