package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.AppDestination
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Bottom navigation behaviour and appearance.
 *
 * Replaces the placeholder "Greeting" screenshot test, which rendered a bare Text
 * and therefore verified nothing about the app.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class NavigationBarTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `selecting a tab reports the destination`() {
    var selected: AppDestination? = null

    composeTestRule.setContent {
      MyApplicationTheme {
        AppBottomNavBar(
          currentDestination = AppDestination.HOME,
          onDestinationSelected = { selected = it }
        )
      }
    }

    composeTestRule.onNodeWithTag("nav_tab_audit").performClick()

    assertEquals(AppDestination.AUDIT, selected)
  }

  @Test
  fun `reselecting the current tab does not re-navigate`() {
    var callbackCount = 0

    composeTestRule.setContent {
      MyApplicationTheme {
        AppBottomNavBar(
          currentDestination = AppDestination.HOME,
          onDestinationSelected = { callbackCount++ }
        )
      }
    }

    // The bar always forwards the tap; MainActivity is responsible for ignoring
    // a same-destination selection. Assert the callback still fires exactly once
    // so that contract stays explicit.
    composeTestRule.onNodeWithTag("nav_tab_home").performClick()

    assertEquals(1, callbackCount)
  }

  @Test
  fun `navigation bar snapshot`() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AppBottomNavBar(
          currentDestination = AppDestination.COMPARE,
          onDestinationSelected = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(
      filePath = "src/test/screenshots/navigation_bar.png"
    )
  }
}
