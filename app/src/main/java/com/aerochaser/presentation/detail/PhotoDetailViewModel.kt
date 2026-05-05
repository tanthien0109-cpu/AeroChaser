package com.aerochaser.presentation.detail

import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.GearInsightRepository
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoDetailViewModel(
    private val photoRepository: PhotoRepository,
    private val gearInsightRepository: GearInsightRepository,
    private val geocoder: Geocoder
) : ViewModel() {

    private val _photo = MutableStateFlow<PhotoMetadata?>(null)
    val photo: StateFlow<PhotoMetadata?> = _photo.asStateFlow()

    private val _gearProfile = MutableStateFlow<GearProfile?>(null)
    val gearProfile: StateFlow<GearProfile?> = _gearProfile.asStateFlow()

    private val _isGearLoading = MutableStateFlow(false)
    val isGearLoading: StateFlow<Boolean> = _isGearLoading.asStateFlow()

    private val _locationName = MutableStateFlow<String?>(null)
    val locationName: StateFlow<String?> = _locationName.asStateFlow()

    fun loadPhoto(photoId: String) {
        viewModelScope.launch {
            val metadata = photoRepository.getPhotoById(photoId)
            _photo.value = metadata
            
            if (metadata != null) {
                fetchGearInsights(metadata)
                if (metadata.gpsLat != null && metadata.gpsLng != null) {
                    fetchLocationName(metadata.gpsLat, metadata.gpsLng)
                }
            }
        }
    }

    private fun fetchLocationName(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                    _locationName.value = name
                }
            } catch (e: Exception) {
                // Ignore, map will still show lat/lng
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
