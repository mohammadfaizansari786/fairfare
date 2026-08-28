package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test that the app under test is installed and resolvable.
 *
 * The previous version asserted the package name equalled "com.example", which is
 * the namespace rather than the applicationId, so it could never pass.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun appContextResolves() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertTrue(appContext.packageName.isNotBlank())
    assertTrue(appContext.packageName.contains("fairfare", ignoreCase = true))
  }
}
