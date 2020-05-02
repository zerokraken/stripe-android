package com.stripe.android

import android.content.Context
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.util.Scanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

private typealias Callback = (Result<JSONObject>) -> Unit
class StripeResourceManager private constructor(args: Args) {

    private val context = args.context.applicationContext
    private val logger = args.logger
    private val requestExecutor = ResourceRequestExecutor.Default()
    private val scope = args.workScope

    private val jsonCache = mutableMapOf<String, JSONObject>()
    private val callbacks =
        mutableMapOf<String, MutableList<Callback>>()

    fun fetchJson(name: String, callback: Callback? = null): JSONObject? {
        jsonCache[name]?.let {
            logger.debug("Found resource $name in memory")
            return it
        }

        val withExt = "$name.json"

        scope.launch {
            synchronized(callbacks) {
                if (name in callbacks) {
                    logger.debug("Existing request for resource $name exists")
                    callback?.let { callbacks[name]!!.add(it) }
                    return@launch
                } else {
                    callbacks[name] = mutableListOf()
                    callback?.let { callbacks[name]!!.add(it) }
                }
            }

            val fromDisk = readJson(context.cacheDir, withExt)
            if (fromDisk != null) {
                logger.debug("Found resource $name on disk")
                onResult(name, Result.success(fromDisk))
            } else {
                logger.debug("Fetching resource $name from cdn")
                try {
                    val result = requestExecutor.execute(JsonResourceRequest(name)).responseJson

                    onResult(name, Result.success(result))

                    logger.debug("Writing resource $name to disk")

                    val cacheFile = File(context.cacheDir, withExt)
                    cacheFile.createNewFile()
                    cacheFile.writeText(result.toString())
                } catch (e: Throwable) {
                    logger.error(e.toString())
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

    private fun readJson(directory: File, fileName: String): JSONObject? {
        logger.debug("looking for $fileName in $directory")
        return File(directory, fileName)
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
        SingletonHolder<Args, StripeResourceManager>(::StripeResourceManager) {

        fun getInstance(context: Context): StripeResourceManager {
            return getInstance(Args(context))
        }

        private fun readJson(scanner: Scanner): JSONObject {
            scanner.use {
                return JSONObject(it.useDelimiter("\\A").next())
            }
        }
    }
}
