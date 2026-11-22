package com.jarvis.assistant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigatesAcrossAllFiveTabs() {
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Chat").performClick()
        composeRule.onNodeWithText("JARVIS Chat").assertIsDisplayed()

        composeRule.onNodeWithText("Voice").performClick()
        composeRule.onNodeWithText("JARVIS Voice").assertIsDisplayed()

        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("Export All").assertIsDisplayed()

        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("AI Provider & Credentials").assertIsDisplayed()
    }
}
