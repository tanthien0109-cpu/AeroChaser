package com.aerochaser.data.local.io

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import android.net.Uri
import com.aerochaser.domain.io.FileIO

// PLATFORM-SPECIFIC: Android File API using SAF (Storage Access Framework)
class AndroidFileIO(private val context: Context) : FileIO {
    override suspend fun getFilesFromDirectory(directoryUri: String): List<String> {
        val uri = Uri.parse(directoryUri)
        val documentFile = DocumentFile.fromTreeUri(context, uri) ?: return emptyList()
        
        return documentFile.listFiles()
            .filter { it.isFile && (it.type?.startsWith("image/") == true) }
            .map { it.uri.toString() }
    }

    override suspend fun fileExists(uri: String): Boolean {
        return try {
            val documentFile = DocumentFile.fromSingleUri(context, Uri.parse(uri))
            documentFile?.exists() == true
        } catch (e: Exception) {
            false
        }
    }
}
