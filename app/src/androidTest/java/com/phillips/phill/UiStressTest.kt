package com.phillips.phill

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class UiStressTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_SMS
    )

    private val random = Random(42)

    @Test
    fun runUiStressMonkeyTest() {
        val tabs = listOf("Dashboard", "Schedule", "Jobs", "Comms", "More")
        
        // Execute 60 random UI interactions
        for (i in 0 until 60) {
            val action = random.nextInt(0, 4)
            
            when (action) {
                0 -> {
                    // Switch bottom bar tabs randomly
                    val randomTab = tabs[random.nextInt(tabs.size)]
                    try {
                        composeTestRule.onNodeWithContentDescription(randomTab).performClick()
                        composeTestRule.waitForIdle()
                    } catch (_: Exception) {
                        // Skip if node not found / not currently layout-visible
                    }
                }
                1 -> {
                    // Click sub-elements on Schedule screen if visible
                    try {
                        val viewModes = listOf("Day", "Week", "Month")
                        val randomMode = viewModes[random.nextInt(viewModes.size)]
                        composeTestRule.onNodeWithText(randomMode).performClick()
                        composeTestRule.waitForIdle()
                    } catch (_: Exception) {
                        // Skip if not on Schedule tab or nodes not present
                    }
                }
                2 -> {
                    // Click sub-items in More menu if visible
                    try {
                        val moreOptions = listOf("Customers", "Billing", "Analytics", "Settings", "Profit & Loss", "Tax Summary")
                        val randomOption = moreOptions[random.nextInt(moreOptions.size)]
                        composeTestRule.onNodeWithText(randomOption).performClick()
                        composeTestRule.waitForIdle()
                    } catch (_: Exception) {
                        // Skip if not on More screen
                    }
                }
                3 -> {
                    // Try to trigger back-navigation randomly
                    try {
                        androidx.test.espresso.Espresso.pressBack()
                        composeTestRule.waitForIdle()
                    } catch (_: Exception) {
                        // Skip if backstack is empty (which would close app)
                    }
                }
            }
            
            // Introduce a short wait to simulate speed-torture clicks
            Thread.sleep(random.nextLong(100, 300))
        }
    }
}
