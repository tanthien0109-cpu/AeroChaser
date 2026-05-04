package com.aerochaser.data.repository

import com.aerochaser.domain.models.HardwareSystemType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GearInsightRepositoryStubTest {

    private val repository = GearInsightRepositoryStub()

    @Test
    fun `Profile A - Xiaomi 13 Ultra - returns precise profile`() = runTest {
        val result = repository.fetchGearProfile(
            cameraModel = "Xiaomi 13 Ultra",
            lensModel = "Xiaomi 13 Ultra Rear Camera",
            systemType = HardwareSystemType.INTEGRATED
        )
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(HardwareSystemType.INTEGRATED, profile?.type)
        assertTrue(profile?.summary?.contains("flagship mobile photography") == true)
        assertTrue(profile?.bodyDetails?.contains("IMX989") == true)
        assertTrue(profile?.lensDetails?.contains("Leica Vario-Summicron") == true)
    }

    @Test
    fun `Profile B - Nikon D500 - returns precise profile`() = runTest {
        val result = repository.fetchGearProfile(
            cameraModel = "NIKON D500",
            lensModel = "AF-S NIKKOR 80-400mm f/4.5-5.6G ED VR",
            systemType = HardwareSystemType.INTERCHANGEABLE
        )
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(HardwareSystemType.INTERCHANGEABLE, profile?.type)
        assertTrue(profile?.summary?.contains("pro-tier APS-C DSLR") == true)
        assertTrue(profile?.bodyDetails?.contains("153-point AF") == true)
        assertTrue(profile?.lensDetails?.contains("120-600mm equivalent") == true)
    }

    @Test
    fun `Edge Case - Null strings - graceful fallback failure`() = runTest {
        val result = repository.fetchGearProfile(
            cameraModel = null,
            lensModel = null,
            systemType = HardwareSystemType.UNKNOWN
        )
        // Expected failure for absolutely zero data
        assertTrue(result.isFailure)
        assertEquals("No hardware metadata available for insight lookup.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `Edge Case - Blank strings - graceful fallback failure`() = runTest {
        val result = repository.fetchGearProfile(
            cameraModel = "   ",
            lensModel = "   ",
            systemType = HardwareSystemType.UNKNOWN
        )
        assertTrue(result.isFailure)
        assertEquals("No hardware metadata available for insight lookup.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `Edge Case - Unknown generic Interchangeable - graceful fallback`() = runTest {
        val result = repository.fetchGearProfile(
            cameraModel = "Generic DSLR",
            lensModel = "Generic 50mm",
            systemType = HardwareSystemType.INTERCHANGEABLE
        )
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(HardwareSystemType.INTERCHANGEABLE, profile?.type)
        assertEquals("Generic DSLR", profile?.bodyDetails)
        assertEquals("Generic 50mm", profile?.lensDetails)
    }
    
    @Test
    fun `Edge Case - Unknown generic Integrated - graceful fallback`() = runTest {
        val result = repository.fetchGearProfile(
            cameraModel = "Generic Phone",
            lensModel = null,
            systemType = HardwareSystemType.INTEGRATED
        )
        assertTrue(result.isSuccess)
        val profile = result.getOrNull()
        assertNotNull(profile)
        assertEquals(HardwareSystemType.INTEGRATED, profile?.type)
        assertTrue(profile?.bodyDetails?.contains("Generic Phone") == true)
        assertEquals("Uses a built-in optical assembly.", profile?.lensDetails)
    }
}
