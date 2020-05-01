package com.stripe.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend inline fun <T> ((T) -> Any).onMain(input: T) {
    val f = this
    withContext(Dispatchers.Main) {
        f(input)
    }
}