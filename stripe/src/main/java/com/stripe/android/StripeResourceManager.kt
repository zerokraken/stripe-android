package com.stripe.android

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.Scanner

class StripeResourceManager private constructor(
    context: Context
) {
    private val context = context.applicationContext
    private val requestExecutor = ResourceRequestExecutor.Default()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val jsonCache =
        Collections.synchronizedMap(mutableMapOf<String, JSONObject>())
    private val activeRequests = Collections.synchronizedSet(mutableSetOf<String>())

    fun fetchJson(name: String, callback: ((Result<JSONObject>) -> Any)?): JSONObject? {
        jsonCache[name]?.let {
            Log.d("StripeResourceManager", "found $name in memory")
            return it
        }

        Log.d("StripeResourceManager", "launching coroutine")

        val withExt = "$name.json"

        scope.launch {
            val fromDisk = readJson(context.cacheDir, withExt)
            if (fromDisk != null) {
                callback?.onMain(Result.success(fromDisk))
            } else {
                if (activeRequests.add(name)) {
                    Log.d("StripeResourceManager", "fetching $name from cdn")
                    try {
                        val result = requestExecutor.execute(JsonResourceRequest(name)).responseJson
                        jsonCache[name] = result

                        callback?.onMain(Result.success(result))

                        Log.d("StripeResourceManager", "writing $name to disk")

                        val cacheFile = File(context.cacheDir, withExt)
                        cacheFile.createNewFile()
                        cacheFile.writeText(result.toString())
                    } catch (e: Throwable) {
                        Log.e("StripeResourceManager", e.toString())
                        callback?.onMain(Result.failure(e))
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

    internal companion object :
        SingletonHolder<Context, StripeResourceManager>(::StripeResourceManager) {

        private fun readJson(scanner: Scanner): JSONObject {
            return JSONObject(scanner.useDelimiter("\\A").next())
        }

        private fun readJson(directory: File, fileName: String): JSONObject? {
            Log.d("StripeResourceManager", "looking for $fileName in $directory")
            return File(directory, fileName)
                .takeIf { it.exists() }
                ?.let {
                    Log.d("StripeResourceManager", "found $fileName on disk")
                    readJson(Scanner(it))
                }
        }
    }
}

