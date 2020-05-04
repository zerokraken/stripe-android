package com.stripe.android

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.util.Scanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private typealias Callback = (Result<JSONObject>) -> Unit
class StripeResourceManager
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal constructor(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val logger: Logger = Logger.real(),
    private val requestExecutor: ResourceRequestExecutor = ResourceRequestExecutor.Default(),
    private val assetManager: AssetManager = AssetManager.Default(context.applicationContext)
) {

    private val context = context.applicationContext
    private val jsonCache = mutableMapOf<String, JSONObject>()
    private val callbacks =
        mutableMapOf<String, MutableList<Callback>>()

    fun fetchJson(name: String, callback: Callback? = null): JSONObject? {
        // Return immediately if the resource is in cache
        jsonCache[name]?.let {
            logger.debug("Found resource $name in memory")
            return it
        }

        // Otherwise, launch the background task
        scope.launch {
            background(name, callback)
        }

        // And return the packaged asset if available
        return assetManager.open(name.json())?.use {
            logger.debug("Loading bundled asset $name in the mean time")
            readJson(Scanner(it))
        }
    }

    fun fetchJson(name: String, liveData: MutableLiveData<JSONObject>) {
        liveData.postValue(
            fetchJson(name) {
                it.getOrNull()?.let(liveData::postValue)
            }
        )
    }

    private suspend fun background(name: String, callback: Callback?) {
        synchronized(callbacks) {
            if (name in callbacks) {
                logger.debug("Existing request for resource $name exists")
                callback?.let { callbacks[name]!!.add(it) }
                return
            } else {
                callbacks[name] = mutableListOf()
                callback?.let { callbacks[name]!!.add(it) }
            }
        }

        val fromDisk = readJson(context.cacheDir, name)
        if (fromDisk != null) {
            logger.debug("Found resource $name on disk")
            onResult(name, Result.success(fromDisk))
        } else {
            logger.debug("Fetching resource $name from cdn")
            try {
                val result = requestExecutor.execute(JsonResourceRequest(name)).responseJson

                onResult(name, Result.success(result))

                logger.debug("Writing resource $name to disk")

                val cacheFile = File(context.cacheDir, name.json())
                cacheFile.createNewFile()
                cacheFile.writeText(result.toString())
            } catch (e: Throwable) {
                logger.error(e.toString())
                onResult(name, Result.failure(e))
            }
        }
    }

    private suspend fun onResult(name: String, result: Result<JSONObject>) {
        val toUpdate: List<Callback> = synchronized(callbacks) {
            callbacks.remove(name) ?: emptyList()
        }

        result.getOrNull()?.let {
            jsonCache[name] = it
        }

        toUpdate.forEach {
            it.onMain(result)
        }
    }

    private fun readJson(directory: File, fileName: String): JSONObject? {
        logger.debug("looking for $fileName in $directory")
        return File(directory, fileName.json())
            .takeIf { it.exists() }
            ?.let {
                logger.debug("found $fileName on disk")
                readJson(Scanner(it))
            }
    }

    internal data class Args(
        val context: Context,
        val workScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        val logger: Logger = Logger.noop()
    )

    internal companion object :
        SingletonHolder<Context, StripeResourceManager>({ StripeResourceManager(it) }) {

        private fun String.json(): String {
            return "$this.json"
        }

        private fun String.isAsset(context: Context): Boolean {
            return context.assets.list("")?.contains(this) ?: false
        }

        private fun readJson(scanner: Scanner): JSONObject {
            scanner.use {
                return JSONObject(it.useDelimiter("\\A").next())
            }
        }
    }
}
