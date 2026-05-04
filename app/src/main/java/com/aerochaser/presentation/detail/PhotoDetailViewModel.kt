package com.aerochaser.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.GearInsightRepository
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoDetailViewModel(
    private val photoRepository: PhotoRepository,
    private val gearInsightRepository: GearInsightRepository
) : ViewModel() {

    private val _photo = MutableStateFlow<PhotoMetadata?>(null)
    val photo: StateFlow<PhotoMetadata?> = _photo.asStateFlow()

    private val _gearProfile = MutableStateFlow<GearProfile?>(null)
    val gearProfile: StateFlow<GearProfile?> = _gearProfile.asStateFlow()

    private val _isGearLoading = MutableStateFlow(false)
    val isGearLoading: StateFlow<Boolean> = _isGearLoading.asStateFlow()

    fun loadPhoto(photoId: String) {
        viewModelScope.launch {
            val metadata = photoRepository.getPhotoById(photoId)
            _photo.value = metadata
            
            if (metadata != null) {
                fetchGearInsights(metadata)
            }
        }
    }

    private fun fetchGearInsights(metadata: PhotoMetadata) {
        viewModelScope.launch {
            _isGearLoading.value = true
            val result = gearInsightRepository.fetchGearProfile(
                cameraModel = metadata.cameraModel,
                lensModel = metadata.lensModel,
                systemType = metadata.systemType
            )
            
            // On failure, we just keep it null. The UI should gracefully handle null GearProfiles.
            _gearProfile.value = result.getOrNull()
            _isGearLoading.value = false
        }
    }
}
