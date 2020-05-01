package com.stripe.android

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.Scanner

private typealias Callback = (Result<JSONObject>) -> Unit
class StripeResourceManager private constructor(
    context: Context
) {

    private val context = context.applicationContext
    private val requestExecutor = ResourceRequestExecutor.Default()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val jsonCache = mutableMapOf<String, JSONObject>()
    private val callbacks =
        mutableMapOf<String, MutableList<Callback>>()

    fun fetchJson(name: String, callback: Callback? = null): JSONObject? {
        jsonCache[name]?.let {
            Log.d("StripeResourceManager", "found $name in memory")
            return it
        }

        Log.d("StripeResourceManager", "launching coroutine")

        val withExt = "$name.json"

        scope.launch {
            synchronized(callbacks) {
                if (name in callbacks) {
                    callback?.let { callbacks[name]!!.add(it) }
                    return@launch
                } else {
                    callbacks[name] = mutableListOf()
                    callback?.let { callbacks[name]!!.add(it) }
                }
            }

            val fromDisk = readJson(context.cacheDir, withExt)
            if (fromDisk != null) {
                onResult(name, Result.success(fromDisk))
            } else {
                Log.d("StripeResourceManager", "fetching $name from cdn")
                try {
                    val result = requestExecutor.execute(JsonResourceRequest(name)).responseJson

                    onResult(name, Result.success(result))

                    Log.d("StripeResourceManager", "writing $name to disk")

                    val cacheFile = File(context.cacheDir, withExt)
                    cacheFile.createNewFile()
                    cacheFile.writeText(result.toString())
                } catch (e: Throwable) {
                    Log.e("StripeResourceManager", e.toString())
                    onResult(name, Result.failure(e))
                }
            }
        }

        return if (context.assets.list("")?.contains(withExt) == true) {
            readJson(Scanner(context.assets.open(withExt)))
        } else {
            null
        }
    }

    fun fetchJson(name: String, liveData: MutableLiveData<JSONObject>) {
        liveData.postValue(
            fetchJson(name) {
                it.getOrNull()?.let(liveData::postValue)
            }
        )
    }

    private suspend fun onResult(name: String, result: Result<JSONObject>) {
        var toUpdate: List<Callback>?
        synchronized(callbacks) {
            toUpdate = callbacks.remove(name)
        }

        result.getOrNull()?.let {
            jsonCache[name] = it
        }

        toUpdate?.forEach {
            it.onMain(result)
        }
    }

    internal companion object :
        SingletonHolder<Context, StripeResourceManager>(::StripeResourceManager) {

        private fun readJson(scanner: Scanner): JSONObject {
            scanner.use {
                return JSONObject(it.useDelimiter("\\A").next())
            }
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

