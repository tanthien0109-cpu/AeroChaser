package com.aerochaser.domain.usecase

import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository

class GetPhotosUseCase(private val repository: PhotoRepository) {
    suspend operator fun invoke(): List<PhotoMetadata> {
        return repository.getLocalPhotos().sortedByDescending { it.captureDateMs }
    }
}
