package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.model.StripeJsonUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.apache.tools.ant.filters.StringInputStream
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StripeResourceManagerTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val scope: CoroutineScope = TestCoroutineScope(testDispatcher)

    private val requestExecutor: ResourceRequestExecutor = mock()
    private val assetManager: AssetManager = mock()

    private val testJson = """{ "hello": "world" } """

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun resoureManager_loadsFromAssets() {
        val resourceManager = StripeResourceManager(
            RuntimeEnvironment.application.applicationContext,
            scope = scope,
            logger = Logger.noop(),
            requestExecutor = requestExecutor,
            assetManager = assetManager
        )
        whenever(assetManager.open("test_asset.json")).thenReturn(StringInputStream(testJson))

        val json = resourceManager.fetchJson("test_asset")
        assertThat(json).isNotNull()
        val map = StripeJsonUtils.jsonObjectToMap(json)

        assertThat(map).isEqualTo(
            mapOf("hello" to "world")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
