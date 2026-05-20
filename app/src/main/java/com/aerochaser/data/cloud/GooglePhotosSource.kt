package com.aerochaser.data.cloud

import android.util.Log
import com.aerochaser.domain.models.PhotoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Photos Library API integration for AeroChaser.
 *
 * Uses the REST API directly since the official Java client library
 * (google-photos-library-client) is designed for server-side use.
 *
 * Auth lifecycle:
 * - UI calls GoogleSignIn with photoslibrary.readonly scope
 * - On success, the access token is passed via [updateAuth]
 * - All API calls use Bearer token authentication
 */
open class GooglePhotosSource {

    companion object {
        private const val TAG = "GooglePhotosSource"
        private const val BASE_URL = "https://photoslibrary.googleapis.com/v1"
    }

    @Volatile
    private var accessToken: String? = null

    /**
     * Called by the ViewModel after sign-in succeeds.
     * @param token The OAuth2 access token with photoslibrary.readonly scope.
     */
    open fun updateAuth(token: String?) {
        accessToken = token
        if (token != null) {
            Log.d(TAG, "Photos source authenticated")
        } else {
            Log.d(TAG, "Photos source cleared")
        }
    }

    /**
     * Lists all albums in the user's Google Photos library.
     */
    suspend fun listAlbums(): List<PhotosAlbum> = withContext(Dispatchers.IO) {
        val token = accessToken ?: throw IllegalStateException("Not authenticated with Google Photos")
        val albums = mutableListOf<PhotosAlbum>()

        try {
            var pageToken: String? = null
            do {
                val urlStr = buildString {
                    append("$BASE_URL/albums?pageSize=50")
                    if (pageToken != null) append("&pageToken=$pageToken")
                }

                val response = makeGetRequest(urlStr, token)
                val json = JSONObject(response)

                json.optJSONArray("albums")?.let { albumsArray ->
                    for (i in 0 until albumsArray.length()) {
                        val album = albumsArray.getJSONObject(i)
                        albums.add(
                            PhotosAlbum(
                                id = album.getString("id"),
                                title = album.optString("title", "Untitled Album"),
                                itemCount = album.optLong("mediaItemsCount", 0),
                                coverPhotoUrl = album.optString("coverPhotoBaseUrl", ""),
                                coverPhotoMediaItemId = album.optString("coverPhotoMediaItemId", "")
                            )
                        )
                    }
                }

                pageToken = json.optString("nextPageToken", "").ifEmpty { null }
            } while (pageToken != null)
        } catch (e: PhotosApiException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list albums", e)
            throw e
        }

        albums
    }

    /**
     * Lists photos in a specific album.
     */
    suspend fun listPhotosInAlbum(albumId: String): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val token = accessToken ?: throw IllegalStateException("Not authenticated with Google Photos")
        val photos = mutableListOf<PhotoMetadata>()

        try {
            var pageToken: String? = null
            do {
                val requestBody = JSONObject().apply {
                    put("albumId", albumId)
                    put("pageSize", 100)
                    if (pageToken != null) put("pageToken", pageToken)
                }.toString()

                val response = makePostRequest("$BASE_URL/mediaItems:search", token, requestBody)
                val json = JSONObject(response)

                json.optJSONArray("mediaItems")?.let { items ->
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val mediaMetadata = item.optJSONObject("mediaMetadata")
                        val photoMeta = mediaMetadata?.optJSONObject("photo")

                        // Only include photos (skip videos)
                        if (mediaMetadata?.has("photo") == true) {
                            val creationTime = mediaMetadata.optString("creationTime", "")
                            val captureMs = try {
                                java.time.Instant.parse(creationTime).toEpochMilli()
                            } catch (_: Exception) {
                                System.currentTimeMillis()
                            }

                            photos.add(
                                PhotoMetadata(
                                    id = item.getString("id"),
                                    localUri = "photos://${item.getString("id")}",
                                    captureDateMs = captureMs,
                                    cameraModel = run {
                                        val make = photoMeta?.optString("cameraMake", "") ?: ""
                                        val model = photoMeta?.optString("cameraModel", "") ?: ""
                                        "$make $model".trim().ifEmpty { null }
                                    },
                                    lensModel = null,
                                    aperture = photoMeta?.optDouble("apertureFNumber")?.takeIf { !it.isNaN() }?.toString(),
                                    shutterSpeed = null,
                                    iso = photoMeta?.optInt("isoEquivalent", 0)?.takeIf { it > 0 },
                                    focalLength = photoMeta?.optDouble("focalLength")?.takeIf { !it.isNaN() }?.toString(),
                                    gpsLat = null,
                                    gpsLng = null,
                                    fileName = item.optString("filename", "Unknown"),
                                    thumbnailUrl = item.optString("baseUrl", "") + "=w256-h256-c",
                                    fileSizeBytes = null,
                                    modifiedDateMs = captureMs
                                )
                            )
                        }
                    }
                }

                pageToken = json.optString("nextPageToken", "").ifEmpty { null }
            } while (pageToken != null)
        } catch (e: PhotosApiException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list photos in album: $albumId", e)
            throw e
        }

        photos
    }

    private fun makeGetRequest(urlStr: String, token: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            return handleResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun makePostRequest(urlStr: String, token: String, body: String): String {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000
            conn.doOutput = true

            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            return handleResponse(conn)
        } finally {
            conn.disconnect()
        }
    }

    private fun handleResponse(conn: HttpURLConnection): String {
        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errorBody = try {
                conn.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                } ?: "No error body"
            } catch (_: Exception) {
                "Could not read error body"
            }
            Log.e(TAG, "Photos API error $responseCode: $errorBody")

            if (responseCode == 401 || responseCode == 403) {
                throw PhotosApiException("Google Photos access denied (HTTP $responseCode). Please ensure you checked and granted the Google Photos permission on the sign-in consent screen.", responseCode)
            }
            throw PhotosApiException("Google Photos API error (HTTP $responseCode)", responseCode)
        }

        return conn.inputStream.let { stream ->
            BufferedReader(InputStreamReader(stream)).use { it.readText() }
        }
    }
}

/**
 * Represents a Google Photos album for the browse UI.
 */
data class PhotosAlbum(
    val id: String,
    val title: String,
    val itemCount: Long,
    val coverPhotoUrl: String,
    val coverPhotoMediaItemId: String
)

/**
 * Thrown when the Google Photos Library API returns an error.
 */
class PhotosApiException(message: String, val httpCode: Int, cause: Throwable? = null) : Exception(message, cause)
