package com.stripe.android

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.Scanner

class StripeResourceManager(
    context: Context
) {
    private val context = context.applicationContext
    private val requestExecutor = ResourceRequestExecutor.Default()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val jsonCache =
        Collections.synchronizedMap(mutableMapOf<String, JSONObject>())
    private val activeRequests = Collections.synchronizedSet(mutableSetOf<String>())

    fun fetchJson(name: String, callback: JsonResourceCallback? = null): JSONObject? {
        jsonCache[name]?.let {
            Log.d("StripeResourceManager", "found $name in memory")
            return it
        }

        Log.d("StripeResourceManager", "launching coroutine")

        val withExt = "$name.json"

        scope.launch {
            val fromDisk = fetchJsonFromDisk(withExt)
            if (fromDisk != null) {
                callback?.let { it::onSuccess.onMain(fromDisk) }
            } else {
                if (activeRequests.add(name)) {
                    Log.d("StripeResourceManager", "fetching $name from cdn")
                    try {
                        val result = requestExecutor.execute(JsonResourceRequest(name)).responseJson
                        jsonCache[name] = result

                        callback?.let { it::onSuccess.onMain(result) }

                        Log.d("StripeResourceManager", "writing $name to disk")

                        val cacheFile = File(context.cacheDir, withExt)
                        cacheFile.createNewFile()
                        cacheFile.writeText(result.toString())
                    } catch (e: Throwable) {
                        Log.e("StripeResourceManager", e.toString())
                        callback?.let { it::onError.onMain(e) }
                    } finally {
                        activeRequests.remove(name)
                    }
                } else {
                    Log.d("StripeResourceManager", "request for $name already exists")
                }
            }
        }

        return if (context.assets.list("")?.contains(withExt) == true) {
            readJson(Scanner(context.assets.open(withExt)))
        } else {
            null
        }
    }

    private fun fetchJsonFromDisk(fileName: String): JSONObject? {
        Log.d("StripeResourceManager", "looking for $fileName in ${context.cacheDir}")
        return File(context.cacheDir, fileName)
            .takeIf { it.exists() }
            ?.let {
                Log.d("StripeResourceManager", "found $fileName on disk")
                readJson(Scanner(it))
            }
    }

    companion object {
        private suspend inline fun <T> ((T) -> Unit).onMain(input: T) {
            val f = this
            withContext(Main) {
                f(input)
            }
        }

        private fun readJson(scanner: Scanner): JSONObject {
            return JSONObject(scanner.useDelimiter("\\A").next())
        }

        interface JsonResourceCallback {
            fun onSuccess(json: JSONObject)
            fun onError(error: Throwable)
        }

        internal data class JsonResourceRequest(
            internal val resourceName: String
        ) : StripeRequest() {
            override val method: Method = Method.GET
            override val baseUrl: String = "$HOST/$resourceName.json"
            override val mimeType: MimeType = MimeType.Json
            override val params: Map<String, Any> = emptyMap()
            override val headersFactory: RequestHeadersFactory = RequestHeadersFactory.Default()

            internal companion object {
                internal const val HOST = "https://dyqiu3dgtk0l0.cloudfront.net"
            }
        }

        internal interface ResourceRequestExecutor {
            fun execute(request: JsonResourceRequest): StripeResponse

            class Default internal constructor(
                private val logger: Logger = Logger.noop()
            ) : ResourceRequestExecutor {
                private val connectionFactory = ConnectionFactory.Default()

                override fun execute(request: JsonResourceRequest): StripeResponse {
                    return executeInternal(request)
                }

                private fun executeInternal(request: JsonResourceRequest): StripeResponse {
                    logger.info(request.toString())

                    connectionFactory.create(request).use {
                        try {
                            val stripeResponse = it.response
                            logger.info(stripeResponse.toString())
                            return stripeResponse
                        } catch (e: IOException) {
                            logger.error("Exception while making Stripe resource request", e)
                            throw e
                        }
                    }
                }
            }
        }
    }
}