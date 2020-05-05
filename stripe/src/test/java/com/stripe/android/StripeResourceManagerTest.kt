package com.stripe.android

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.stripe.android.model.StripeJsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.apache.tools.ant.filters.StringInputStream
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class StripeResourceManagerTest {
    private val testDispatcher = TestCoroutineDispatcher()
    private val scope: TestCoroutineScope = TestCoroutineScope(testDispatcher)

    private val requestExecutor: ResourceRequestExecutor = mock()
    private val assetManager: AssetManager = mock()

    private val localAsset = mapOf("hello" to "world")
    private val cdnResource = mapOf("cdn" to true)

    private lateinit var resourceManager: StripeResourceManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        whenever(assetManager.open("test_asset.json"))
            .thenReturn(StringInputStream(JSONObject(localAsset).toString()))
        whenever(requestExecutor.execute(argWhere { it.resourceName == "cdn_resource" }))
            .thenReturn(StripeResponse(200, JSONObject(cdnResource).toString()))

        resourceManager = StripeResourceManager(
            RuntimeEnvironment.application.applicationContext,
            scope = scope,
            logger = Logger.noop(),
            requestExecutor = requestExecutor,
            assetManager = assetManager
        )
    }

    @Test
    fun fetchJson_loadsFromAssets() {
        val json = resourceManager.fetchJson("test_asset")
        assertThat(json).isNotNull()
        val map = StripeJsonUtils.jsonObjectToMap(json)

        assertThat(map).isEqualTo(localAsset)
    }

    @Test
    fun fetchJson_handlesUnknownAsset() {
        val json = resourceManager.fetchJson("unknown_asset")
        assertThat(json).isNull()
    }

    @Test
    fun fetchJson_loadFromCdn_andCallsCallback() {
        var result: JSONObject? = null
        resourceManager.fetchJson("cdn_resource") {
            assertThat(it.isSuccess).isTrue()
            result = it.getOrThrow()
        }

        assertThat(result).isNotNull()

        val map = StripeJsonUtils.jsonObjectToMap(result)
        assertThat(map).isEqualTo(cdnResource)
    }

    @Test
    fun fetchJson_storesCdnResultInCache() {
        // nothing in the cache first time
        assertThat(resourceManager.fetchJson("cdn_resource")).isNull()

        val result = resourceManager.fetchJson("cdn_resource")
        // but on the second call, it is already cached
        assertThat(result).isNotNull()

        val map = StripeJsonUtils.jsonObjectToMap(result)
        assertThat(map).isEqualTo(cdnResource)
    }

    @Test
    fun fetchJson_storesCdnResultOnDisk_andCachesResultFromDisk() {
        // kick of the cdn request
        assertThat(resourceManager.fetchJson("cdn_resource")).isNull()

        val emptyRequestExecutor: ResourceRequestExecutor = mock()
        // in practice we never have two instances of the resource manager but this approximates
        // what happens when the app is killed and relaunched (i.e. cache is cleared)
        val otherManager = StripeResourceManager(
            RuntimeEnvironment.application.applicationContext,
            scope = scope,
            logger = Logger.noop(),
            requestExecutor = emptyRequestExecutor,
            assetManager = mock()
        )

        var resultFromDisk: JSONObject? = null
        otherManager.fetchJson("cdn_resource") {
            assertThat(it.isSuccess).isTrue()
            resultFromDisk = it.getOrThrow()
        }
        assertThat(resultFromDisk).isNotNull()

        val map = StripeJsonUtils.jsonObjectToMap(resultFromDisk)
        assertThat(map).isEqualTo(cdnResource)

        // first request was stored on disk so we never hit the cdn
        verify(emptyRequestExecutor, never()).execute(any())

        // and result loaded from disk is stored in cache so we don't hit disk again
        assertThat(StripeJsonUtils.jsonObjectToMap(otherManager.fetchJson("cdn_resource")))
            .isEqualTo(cdnResource)
    }

    @Test
    fun fetchJson_handlesConcurrentRequests() {
        testDispatcher.pauseDispatcher()

        var firstCallbackResult: JSONObject? = null
        var firstCallbackCounter = 0
        val firstCachedResult = resourceManager.fetchJson("cdn_resource") {
            firstCallbackCounter++
            assertThat(it.isSuccess)
            firstCallbackResult = it.getOrThrow()
        }

        var secondCallbackResult: JSONObject? = null
        var secondCallbackCounter = 0
        val secondCachedResult = resourceManager.fetchJson("cdn_resource") {
            secondCallbackCounter++
            assertThat(it.isSuccess)
            secondCallbackResult = it.getOrThrow()
        }

        assertThat(firstCachedResult).isNull()
        assertThat(firstCallbackResult).isNull()
        assertThat(firstCallbackCounter).isEqualTo(0)
        assertThat(secondCachedResult).isNull()
        assertThat(secondCallbackResult).isNull()
        assertThat(secondCallbackCounter).isEqualTo(0)

        testDispatcher.resumeDispatcher()

        assertThat(firstCallbackResult).isNotNull()
        assertThat(firstCallbackCounter).isEqualTo(1)
        assertThat(secondCallbackResult).isNotNull()
        assertThat(secondCallbackCounter).isEqualTo(1)

        verify(requestExecutor, times(1)).execute(any())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
