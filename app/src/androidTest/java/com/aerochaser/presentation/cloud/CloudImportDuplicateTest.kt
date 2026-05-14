package com.aerochaser.presentation.cloud

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CloudImportDuplicateTest {

    private lateinit var device: UiDevice
    private val launcherPackage = "com.google.android.apps.nexuslauncher"
    private val appPackage = "com.aerochaser"

    @Before
    fun startMainActivityFromHomeScreen() {
        // Initialize UiDevice instance
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Start from the home screen
        device.pressHome()

        // Wait for launcher
        val launcherPackage: String = device.launcherPackageName
        device.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), 5000)

        // Launch the app
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = context.packageManager.getLaunchIntentForPackage(appPackage)?.apply {
            // Clear out any previous instances
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Wait for the app to appear
        device.wait(Until.hasObject(By.pkg(appPackage).depth(0)), 5000)
    }

    @Test
    fun testCloudImportAvoidsDuplicates() {
        // 1. Wait for "Connect Google Account" button if signed out, or wait for Signed in state
        val connectButton = device.wait(Until.findObject(By.text("Connect Google Account")), 5000)
        if (connectButton != null) {
            connectButton.click()
            
            // Wait for account picker and click the first account (Google Play Services)
            val accountItem = device.wait(Until.findObject(By.res("com.google.android.gms:id/account_name")), 10000)
            accountItem?.click()
            
            // It might prompt for permissions (Drive, Photos). Click "Allow" or "Continue"
            var continueButton = device.wait(Until.findObject(By.text("Continue")), 5000)
            while (continueButton != null) {
                continueButton.click()
                Thread.sleep(2000)
                continueButton = device.wait(Until.findObject(By.text("Continue")), 2000)
            }
            var allowButton = device.wait(Until.findObject(By.text("Allow")), 5000)
            while (allowButton != null) {
                allowButton.click()
                Thread.sleep(2000)
                allowButton = device.wait(Until.findObject(By.text("Allow")), 2000)
            }
        }

        // Wait for tabs to be visible
        device.wait(Until.hasObject(By.text("Drive")), 10000)

        // 2. Drive Import
        val driveTab = device.findObject(By.text("Drive"))
        driveTab.click()
        
        val browseDriveButton = device.wait(Until.findObject(By.text("Browse Drive Folders")), 5000)
        browseDriveButton?.click()
        
        // Click the first folder
        val folderIcon = device.wait(Until.findObject(By.desc("View photos")), 10000)
        folderIcon?.click()

        // Wait for photos to load
        val importAllDrive = device.wait(Until.findObject(By.text("Import All")), 10000)
        importAllDrive?.click()
        
        // Wait for complete
        device.wait(Until.hasObject(By.text("Import Complete")), 15000)
        
        // 3. Photos Import
        val photosTab = device.findObject(By.text("Photos"))
        photosTab.click()
        
        val browsePhotosButton = device.wait(Until.findObject(By.text("Browse Photo Albums")), 5000)
        browsePhotosButton?.click()
        
        // Click the first album
        val albumIcon = device.wait(Until.findObject(By.descContains("")), 10000) // AlbumCard is clickable
        // Let's just find the first text that is "items" which is inside AlbumCard
        val albumItem = device.wait(Until.findObject(By.textContains("items")), 10000)
        albumItem?.click()
        
        // Wait for photos to load
        val importAllPhotos = device.wait(Until.findObject(By.text("Import All")), 10000)
        importAllPhotos?.click()
        
        // Wait for complete
        device.wait(Until.hasObject(By.text("Import Complete")), 15000)
        
        // Optionally assert text contains "skipped" if we want to ensure duplicates were skipped
        val skippedText = device.findObject(By.textContains("skipped"))
        assert(skippedText != null)
    }
}
