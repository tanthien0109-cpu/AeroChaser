package com.aerochaser.data.cloud

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GooglePhotosSourceTest {

    private class FakeGooglePhotosSource : GooglePhotosSource() {
        var getResponse: String = ""
        var postResponse: String = ""
        var lastGetUrl: String? = null
        var lastPostUrl: String? = null
        var lastPostBody: String? = null

        override fun makeGetRequest(urlStr: String, token: String): String {
            lastGetUrl = urlStr
            return getResponse
        }

        override fun makePostRequest(urlStr: String, token: String, body: String): String {
            lastPostUrl = urlStr
            lastPostBody = body
            return postResponse
        }
    }

    private lateinit var photosSource: FakeGooglePhotosSource

    @Before
    fun setUp() {
        photosSource = FakeGooglePhotosSource()
    }

    @Test
    fun testDefaultState() = runTest {
        assertEquals("Google Photos", photosSource.sourceName)
        assertFalse(photosSource.isAuthenticated.first())
    }

    @Test
    fun testUpdateAuthWithNull() = runTest {
        photosSource.updateAuth(null)
        assertFalse(photosSource.isAuthenticated.first())
    }

    @Test
    fun testUpdateAuthWithValidToken() = runTest {
        photosSource.updateAuth("mock_access_token")
        assertTrue(photosSource.isAuthenticated.first())
    }

    @Test
    fun testListAlbumsWithoutAuthThrowsException() = runTest {
        try {
            photosSource.listAlbums()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Not authenticated with Google Photos", e.message)
        }
    }

    @Test
    fun testListAlbumsSuccess() = runTest {
        photosSource.updateAuth("mock_token")
        photosSource.getResponse = """
            {
              "albums": [
                {
                  "id": "album_1",
                  "title": "Plane Spotting 2026",
                  "mediaItemsCount": "42",
                  "coverPhotoBaseUrl": "https://lh3.googleusercontent.com/cover1"
                }
              ]
            }
        """.trimIndent()

        val albums = photosSource.listAlbums()
        assertEquals(1, albums.size)
        assertEquals("album_1", albums[0].id)
        assertEquals("Plane Spotting 2026", albums[0].title)
        assertEquals(42L, albums[0].itemCount)
        assertEquals("https://lh3.googleusercontent.com/cover1", albums[0].coverPhotoUrl)
        assertTrue(photosSource.lastGetUrl!!.contains("pageSize=50"))
    }

    @Test
    fun testListPhotosInAlbumSuccess() = runTest {
        photosSource.updateAuth("mock_token")
        photosSource.postResponse = """
            {
              "mediaItems": [
                {
                  "id": "photo_1",
                  "filename": "A350_landing.jpg",
                  "baseUrl": "https://lh3.googleusercontent.com/photo1",
                  "mediaMetadata": {
                    "creationTime": "2026-05-25T12:00:00Z",
                    "photo": {
                      "cameraMake": "Sony",
                      "cameraModel": "A7R V",
                      "apertureFNumber": 2.8,
                      "isoEquivalent": 200,
                      "focalLength": 200.0
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val photos = photosSource.listPhotosInAlbum("album_1")
        assertEquals(1, photos.size)
        assertEquals("photo_1", photos[0].id)
        assertEquals("A350_landing.jpg", photos[0].fileName)
        assertEquals("Sony A7R V", photos[0].cameraModel)
        assertEquals("2.8", photos[0].aperture)
        assertEquals(200, photos[0].iso)
        assertEquals("200.0", photos[0].focalLength)
        assertEquals("https://lh3.googleusercontent.com/photo1=w256-h256-c", photos[0].thumbnailUrl)
        assertTrue(photosSource.lastPostUrl!!.contains("mediaItems:search"))
        assertTrue(photosSource.lastPostBody!!.contains("album_1"))
    }

    @Test
    fun testFetchPhotosSuccess() = runTest {
        photosSource.updateAuth("mock_token")
        photosSource.getResponse = """
            {
              "mediaItems": [
                {
                  "id": "photo_2",
                  "filename": "B787_takeoff.jpg",
                  "baseUrl": "https://lh3.googleusercontent.com/photo2",
                  "mediaMetadata": {
                    "creationTime": "2026-05-25T13:00:00Z",
                    "photo": {
                      "cameraMake": "Canon",
                      "cameraModel": "R5",
                      "apertureFNumber": 4.0,
                      "isoEquivalent": 100,
                      "focalLength": 400.0
                    }
                  }
                }
              ]
            }
        """.trimIndent()

        val photos = photosSource.fetchPhotos()
        assertEquals(1, photos.size)
        assertEquals("photo_2", photos[0].id)
        assertEquals("B787_takeoff.jpg", photos[0].fileName)
        assertEquals("Canon R5", photos[0].cameraModel)
        assertEquals("4.0", photos[0].aperture)
        assertEquals(100, photos[0].iso)
        assertEquals("400.0", photos[0].focalLength)
        assertEquals("https://lh3.googleusercontent.com/photo2=w256-h256-c", photos[0].thumbnailUrl)
        assertTrue(photosSource.lastGetUrl!!.contains("mediaItems?pageSize=100"))
    }
}
