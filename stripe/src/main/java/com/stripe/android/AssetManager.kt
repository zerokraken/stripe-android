package com.stripe.android

import android.content.Context
import java.io.InputStream

interface AssetManager {
    fun open(asset: String): InputStream?

    class Default(val context: Context) : AssetManager {

        override fun open(asset: String): InputStream? {
            return asset.takeIf { it.isAsset(context) }?.let(context.assets::open)
        }
    }

    companion object {
        private fun String.isAsset(context: Context): Boolean {
            return context.assets.list("")?.contains(this) ?: false
        }
    }
}
