package com.aerochaser.domain.io

/**
 * Explicit "seed" for File I/O operations.
 * This ensures the domain layer doesn't know about Android Context or java.io.File directly,
 * allowing iOS and Windows to plug in their own implementations.
 */
interface FileIO {
    suspend fun getFilesFromDirectory(directoryUri: String): List<String>
    suspend fun fileExists(uri: String): Boolean
}
